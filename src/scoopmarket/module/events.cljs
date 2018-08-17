(ns scoopmarket.module.events
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [integrant.core :as ig]
            [scoopmarket.module.uport :as uport]
            [scoopmarket.module.web3 :as web3]
            [day8.re-frame.http-fx]
            [district0x.re-frame.web3-fx]
            [ajax.core :as ajax]
            [cljs-web3.eth :as web3-eth]
            [meta-merge.core :refer [meta-merge]]))

(defmethod ig/init-key :scoopmarket.module.events [_ _]
  [(re-frame/reg-event-db
    ::initialize-db
    (fn-traced [_ [_ initial-db]]
               initial-db))

   (re-frame/reg-event-db
    ::toggle-sidebar
    (fn-traced [db _]
               (update-in db [:sidebar-opened] not)))

   (re-frame/reg-event-db
    ::set-active-page
    (fn-traced [db [_ panel route-params]]
               (assoc db :active-page {:panel panel :route-params route-params})))

   (re-frame/reg-event-db
    ::connect-uport
    (fn-traced [db [_ uport web3]]
               (uport/request-credentials
                uport #(re-frame/dispatch [::request-credential-success uport web3 %]))
               db))

   (re-frame/reg-event-db
    ::request-credential-success
    (fn-traced [db [_ uport web3 credential]]
               (let [{:keys [:address :avatar]} credential
                     {:keys [:contract :contract-address :network-id :dev]} web3
                     {:keys [:abi :networks]} contract
                     web3-instance (js-invoke uport "getWeb3")
                     contract-instance
                     (web3-eth/contract-at web3-instance abi contract-address)
                     my-address (when (uport/is-mnid? address)
                                  (aget (uport/decode address) "address"))
                     is-rinkeby? (or (some-> (:uport db)
                                             (aget "network")
                                             (aget "id")
                                             (js/parseInt 16)
                                             (== network-id))
                                     dev)]
                 (re-frame/dispatch [::fetch-credit web3])
                 (-> db
                     (assoc-in [:web3 :web3-instance] web3-instance)
                     (assoc-in [:web3 :contract-instance] contract-instance)
                     (assoc-in [:web3 :my-address] my-address)
                     (assoc-in [:web3 :is-rinkeby?] is-rinkeby?)
                     (assoc :credential credential)
                     (dissoc :scoops)))))

   (re-frame/reg-event-db
    ::api-failure
    (fn-traced [db [_ result]]
               (js/console.log "Api failure." result)
               (-> db
                   (assoc :message {:status :error :text (str result)})
                   (dissoc :loading?))))

   (re-frame/reg-event-fx
    ::fetch-credit
    (fn-traced [{:keys [:db]} [_ web3]]
               {:web3/call {:web3 (:web3-instance web3)
                            :fns [{:instance (:contract-instance web3)
                                   :fn :payments
                                   :args [(:my-address web3)]
                                   :on-success [::fetch-credit-success]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-credit-success
    (fn-traced [db [_ credit]]
               (-> db
                   (dissoc :loading?)
                   (assoc :credit (js-invoke credit "toNumber")))))

   (re-frame/reg-event-fx
    ::fetch-mint-cost
    (fn-traced [{:keys [:db]} [_ web3]]
               {:web3/call {:web3 (:web3-instance web3)
                            :fns [{:instance (:contract-instance web3)
                                   :fn :mint-cost
                                   :on-success [::fetch-mint-cost-success]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-mint-cost-success
    (fn-traced [db [_ mint-cost]]
               (-> db
                   (dissoc :loading?)
                   (assoc :mint-cost (js-invoke mint-cost "toNumber")))))

   (re-frame/reg-event-fx
    ::fetch-scoops
    (fn-traced [{:keys [:db]} [_ web3]]
               {:web3/call {:web3 (:web3-instance web3)
                            :fns [{:instance (:contract-instance web3)
                                   :fn :scoops-of
                                   :args [(:my-address web3)]
                                   :on-success [::fetch-scoops-success web3]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-scoops-success
    (fn-traced [db [_ web3 scoops]]
               (let [ids (map #(let [id (js-invoke % "toNumber")]
                                 (re-frame/dispatch [::fetch-scoop web3 :scoops id])
                                 id) scoops)]
                 (-> db
                     (dissoc :loading?)
                     (assoc :scoops (select-keys (:scoops db)
                                                 (map #(keyword (str %)) ids)))))))

   (re-frame/reg-event-fx
    ::fetch-scoops-for-sale
    (fn-traced [{:keys [db]} [_ web3]]
               {:web3/call {:web3 (:web3-instance web3)
                            :fns [{:instance (:contract-instance web3)
                                   :fn :scoops-for-sale
                                   :args []
                                   :on-success [::fetch-scoops-for-sale-success web3]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-scoops-for-sale-success
    (fn-traced [db [_ web3 scoops-for-sale]]
               (let [ids (->> scoops-for-sale
                              (map-indexed (fn [id for-sale?]
                                             (when for-sale?
                                               (re-frame/dispatch [::fetch-scoop web3
                                                                   :scoops-for-sale id])
                                               id)))
                              (filter some?))]
                 (-> db
                     (dissoc :loading?)
                     (assoc :scoops-for-sale (select-keys (:scoops-for-sale db)
                                                          (map #(keyword (str %)) ids)))))))

   (re-frame/reg-event-fx
    ::fetch-scoop
    (fn-traced [{:keys [db]} [_ web3 db-key id]]
               {:web3/call {:web3 (:web3-instance web3)
                            :fns [{:instance (:contract-instance web3)
                                   :fn :scoop
                                   :args [id]
                                   :on-success [::fetch-scoop-success db-key]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-scoop-success
    (fn-traced [db [_ db-key scoop]]
               (let [[:id :name :timestamp :image-hash :price :for-sale?
                      :meta-hash :author :owner :requestor] scoop
                     id (js-invoke id "toNumber")
                     price (js-invoke price "toNumber")
                     timestamp (js-invoke timestamp "toNumber")]
                 (when-not (empty? meta-hash)
                   (js-invoke (:ipfs db) "cat" meta-hash
                              (fn [_ file]
                                (when file
                                  (let [meta (js->clj (.parse js/JSON
                                                              (.toString file "utf8"))
                                                      :keywordize-keys true)]
                                    (re-frame/dispatch [::fetch-meta-success
                                                        db-key id meta]))))))
                 (-> db
                     (dissoc :loading?)
                     (update-in [db-key (keyword (str id))]
                                assoc :id id :name name :timestamp timestamp
                                :image-hash image-hash :price price :for-sale? for-sale?
                                :author author :meta-hash meta-hash
                                :owner owner :requestor requestor)))))

   (re-frame/reg-event-fx
    ::fetch-scoop-approval
    (fn-traced [{:keys [db]} [_ web3 db-key id]]
               {:web3/call {:web3 (:web3-instance web3)
                            :fns [{:instance (:contract-instance web3)
                                   :fn :get-approved
                                   :args [id]
                                   :on-success [::fetch-scoop-approval-success db-key id]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-scoop-approval-success
    (fn-traced [db [_ db-key id approved]]
               (-> db
                   (dissoc :loading?)
                   (update-in [db-key (keyword (str id))]
                              assoc :approved approved))))

   (re-frame/reg-event-db
    ::fetch-meta-success
    (fn-traced [db [_ db-key id meta]]
               (assoc-in db [db-key (keyword (str id)) :meta] meta)))

   (re-frame/reg-event-db
    ::mint
    (fn-traced [db [_ web3 mint-cost scoop]]
               (println mint-cost)
               (let [{:keys [:image-hash :name :for-sale? :price]} scoop
                     price (if-not (empty? price)
                             (js/parseInt price) 0)]
                 (web3-eth/contract-call (:contract-instance web3)
                                         :mint (or name "") price (or for-sale? false) image-hash
                                         {:gas 4700000
                                          :gas-price 100000000000
                                          :value mint-cost}
                                         (fn [err tx-hash]
                                           (if err
                                             (js/console.log err)
                                             (web3/wait-for-mined web3 tx-hash
                                                                  #(js/console.log "pending")
                                                                  #(re-frame/dispatch [::mint-success web3 %]))))))
               (assoc db :loading? {:message "Minting..."})))

   (re-frame/reg-event-db
    ::mint-success
    (fn-traced [db [_ web3 res]]
               (re-frame/dispatch [::fetch-scoops web3])
               (re-frame/dispatch [::fetch-scoops-for-sale web3])
               (dissoc db :loading?)))

   (re-frame/reg-event-db
    ::edit-scoop
    (fn-traced [db [_ web3 id scoop]]
               (let [{:keys [name price for-sale? meta-hash]} scoop]
                 (web3-eth/contract-call (:contract-instance web3)
                                         :edit-token
                                         id name (or price 0) (or for-sale? false) meta-hash
                                         {:gas 4700000
                                          :gas-price 100000000000}
                                         (fn [err tx-hash]
                                           (web3/wait-for-mined web3 tx-hash
                                                                #(js/console.log "pending")
                                                                #(re-frame/dispatch [::edit-scoop-success web3])))))
               (assoc db :loading? {:message "Updating scoop..."})))

   (re-frame/reg-event-db
    ::edit-scoop-success
    (fn-traced [db [_ web3 res]]
               (re-frame/dispatch [::fetch-scoops web3])
               (re-frame/dispatch [::fetch-scoops-for-sale web3])
               (dissoc db :loading?)))

   (re-frame/reg-event-db
    ::request
    (fn-traced [db [_ web3 id]]
               (web3-eth/contract-call (:contract-instance web3)
                                       :request id
                                       {:gas 4700000
                                        :gas-price 100000000000}
                                       (fn [err tx-hash]
                                         (if err
                                           (js/console.log err)
                                           (web3/wait-for-mined web3 tx-hash
                                                                #(js/console.log "pending")
                                                                #(re-frame/dispatch [::request-success web3 %])))))
               (assoc db :loading? {:message "Requesting"})))

   (re-frame/reg-event-db
    ::request-success
    (fn-traced [db [_ web3 res]]
               (re-frame/dispatch [::fetch-scoops-for-sale web3])
               (dissoc db :loading?)))

   (re-frame/reg-event-db
    ::approve
    (fn-traced [db [_ web3 id to]]
               (web3-eth/contract-call (:contract-instance web3)
                                       :approve to id
                                       {:gas 4700000
                                        :gas-price 100000000000}
                                       (fn [err tx-hash]
                                         (if err
                                           (js/console.log err)
                                           (web3/wait-for-mined web3 tx-hash
                                                                #(js/console.log "pending")
                                                                #(re-frame/dispatch [::approve-success web3 %])))))
               (assoc db :loading? {:message "Approving..."})))

   (re-frame/reg-event-db
    ::approve-success
    (fn-traced [db [_ web3 res]]
               (re-frame/dispatch [::fetch-scoops-for-sale web3])
               (dissoc db :loading?)))

   (re-frame/reg-event-db
    ::cancel
    (fn-traced [db [_ web3 id]]
               (web3-eth/contract-call (:contract-instance web3)
                                       :cancel id
                                       {:gas 4700000
                                        :gas-price 100000000000}
                                       (fn [err tx-hash]
                                         (if err
                                           (js/console.log err)
                                           (web3/wait-for-mined web3 tx-hash
                                                                #(js/console.log "pending")
                                                                #(re-frame/dispatch [::cancel-success web3 %])))))
               (assoc db :loading? {:message "Approving..."})))

   (re-frame/reg-event-db
    ::cancel-success
    (fn-traced [db [_ web3 res]]
               (re-frame/dispatch [::fetch-scoops-for-sale web3])
               (dissoc db :loading?)))

   (re-frame/reg-event-db
    ::deny
    (fn-traced [db [_ web3 id]]
               (web3-eth/contract-call (:contract-instance web3)
                                       :deny id
                                       {:gas 4700000
                                        :gas-price 100000000000}
                                       (fn [err tx-hash]
                                         (if err
                                           (js/console.log err)
                                           (web3/wait-for-mined web3 tx-hash
                                                                #(js/console.log "pending")
                                                                #(re-frame/dispatch [::deny-success web3 %])))))
               (assoc db :loading? {:message "Approving..."})))

   (re-frame/reg-event-db
    ::deny-success
    (fn-traced [db [_ web3 res]]
               (re-frame/dispatch [::fetch-scoops-for-sale web3])
               (dissoc db :loading?)))

   (re-frame/reg-event-db
    ::purchase
    (fn-traced [db [_ web3 id price]]
               (web3-eth/contract-call (:contract-instance web3)
                                       :purchase id
                                       {:gas 4700000
                                        :gas-price 100000000000
                                        :value price}
                                       (fn [err tx-hash]
                                         (if err
                                           (js/console.log err)
                                           (web3/wait-for-mined web3 tx-hash
                                                                #(js/console.log "pending")
                                                                #(re-frame/dispatch [::purchase-success web3 %])))))
               (assoc db :loading? {:message "Purchasing..."})))

   (re-frame/reg-event-db
    ::purchase-success
    (fn-traced [db [_ web3 res]]
               (re-frame/dispatch [::fetch-credit web3])
               (re-frame/dispatch [::fetch-scoops web3])
               (re-frame/dispatch [::fetch-scoops-for-sale web3])
               (dissoc db :loading?)))

   (re-frame/reg-event-db
    ::withdraw
    (fn-traced [db [_ web3]]
               (web3-eth/contract-call (:contract-instance web3)
                                       :withdraw-payments
                                       {:gas 4700000
                                        :gas-price 100000000000}
                                       (fn [err tx-hash]
                                         (if err
                                           (js/console.log err)
                                           (web3/wait-for-mined web3 tx-hash
                                                                #(js/console.log "pending")
                                                                #(re-frame/dispatch [::withdraw-success web3 %])))))
               (assoc db :loading? {:message "withdrawing..."})))

   (re-frame/reg-event-db
    ::withdraw-success
    (fn-traced [db [_ web3]]
               (re-frame/dispatch [::fetch-credit web3])
               (dissoc db :loading?)))])

(defmethod ig/halt-key! :scoopmarket.module.events [_ _]
  (re-frame/clear-event))
