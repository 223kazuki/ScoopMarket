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
                     contract-instance (web3-eth/contract-at web3-instance abi contract-address)
                     my-address (when (uport/is-mnid? address)
                                  (aget (uport/decode address) "address"))
                     is-rinkeby? (or (some-> (:uport db)
                                             (aget "network")
                                             (aget "id")
                                             (js/parseInt 16)
                                             (== network-id))
                                     dev)]
                 (-> db
                     (assoc-in [:web3 :web3-instance] web3-instance)
                     (assoc-in [:web3 :contract-instance] contract-instance)
                     (assoc-in [:web3 :my-address] my-address)
                     (assoc-in [:web3 :is-rinkeby?] is-rinkeby?)
                     (assoc :credential credential)
                     (dissoc :scoops)))))

   (re-frame/reg-event-db
    ::toggle-sidebar
    (fn-traced [db _]
               (update-in db [:sidebar-opened] not)))

   (re-frame/reg-event-db
    ::set-active-page
    (fn-traced [db [_ panel route-params]]
               (assoc db :active-page {:panel panel :route-params route-params})))

   (re-frame/reg-event-db
    ::api-failure
    (fn-traced [db [_ result]]
               (js/console.log "Api failure." result)
               (-> db
                   (assoc :message {:status :error :text (str result)})
                   (dissoc :loading?))))

   (re-frame/reg-event-fx
    ::fetch-scoops
    (fn-traced [{:keys [db]} [_ web3]]
               {:web3/call {:web3 (:web3-instance web3)
                            :fns [{:instance (:contract-instance web3)
                                   :fn :scoops-of
                                   :args [(:my-address web3)]
                                   :on-success [::fetch-scoops-success]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-scoops-success
    (fn-traced [db [_ scoops]]
               (assoc db
                      :loading? false
                      :scoops (meta-merge
                               (:scoops db)
                               (->> scoops
                                    (map #(let [id (first (aget % "c"))]
                                            (hash-map (keyword (str id)) {:id id})))
                                    (into {}))))))

   (re-frame/reg-event-fx
    ::fetch-scoop
    (fn-traced [{:keys [db]} [_ web3 id]]
               {:web3/call {:web3 (:web3-instance web3)
                            :fns [{:instance (:contract-instance web3)
                                   :fn :scoop
                                   :args [id]
                                   :on-success [::fetch-scoop-success]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-scoop-success
    (fn-traced [db [_ scoop]]
               (let [[id name timestamp image-hash price for-sale? meta-hash author owner requestor] scoop
                     id (first (aget id "c"))
                     price (first (aget price "c"))
                     timestamp (first (aget timestamp "c"))
                     cat (aget (:ipfs db) "cat")]
                 (when-not (empty? meta-hash)
                   (cat meta-hash
                        (fn [_ file]
                          (when file
                            (let [meta (js->clj (.parse js/JSON
                                                        (.toString file "utf8"))
                                                :keywordize-keys true)]
                              (re-frame/dispatch [::fetch-meta-success id meta]))))))
                 (-> db
                     (dissoc :loading?)
                     (update-in [:scoops (keyword (str id))]
                                assoc :id id :name name :timestamp timestamp :image-hash image-hash
                                :price price :for-sale? for-sale? :author author :meta-hash meta-hash
                                :owner owner requestor requestor)))))

   (re-frame/reg-event-db
    ::fetch-meta-success
    (fn-traced [db [_ id meta]]
               (assoc-in db [:scoops (keyword (str id)) :meta] meta)))

   (re-frame/reg-event-db
    ::mint
    (fn-traced [db [_ web3 scoop]]
               (let [{:keys [:image-hash :name :for-sale? :price]} scoop
                     price (if-not (empty? price)
                             (js/parseInt price) 0)]
                 (web3-eth/contract-call (:contract-instance web3)
                                         :mint (or name "") price (or for-sale? false) image-hash
                                         {:gas 4700000
                                          :gas-price 100000000000
                                          ;; TODO: Specify value.
                                          :value 10000000000000000}
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
               (dissoc db :loading?)))

   (re-frame/reg-event-db
    ::update-meta
    (fn-traced [db [_ web3 id hash]]
               (web3-eth/contract-call (:contract-instance web3)
                                       :set-token-meta-data-uri
                                       id hash
                                       {:gas 4700000
                                        :gas-price 100000000000}
                                       (fn [err tx-hash]
                                         (web3/wait-for-mined web3 tx-hash
                                                              #(js/console.log "pending")
                                                              #(re-frame/dispatch [::update-meta-success web3 %]))))
               (assoc db :loading? {:message "Updating meta info..."})))

   (re-frame/reg-event-db
    ::update-meta-success
    (fn-traced [db [_ web3 res]]
               (re-frame/dispatch [::fetch-scoops web3])
               (dissoc db :loading?)))])

(defmethod ig/halt-key! :scoopmarket.module.events [_ _]
  (re-frame/clear-event))
