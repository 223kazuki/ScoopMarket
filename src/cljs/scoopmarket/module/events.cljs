(ns scoopmarket.module.events
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [integrant.core :as ig]
            [scoopmarket.module.uport :as uport]
            [scoopmarket.module.web3 :as web3]
            [day8.re-frame.http-fx]
            [district0x.re-frame.web3-fx]
            [ajax.core :as ajax]
            [cljs-web3.eth :as web3-eth]))

(defmethod ig/init-key :scoopmarket.module.events [_ _]
  [(re-frame/reg-event-db
    ::initialize-db
    (fn-traced [_ [_ initial-db]]
               initial-db))

   (re-frame/reg-event-db
    ::connect-uport
    (fn-traced [db _]
               (let [uport (:uport db)]
                 (.then
                  (.requestCredentials uport
                                       (clj->js {:requested ["name" "avatar" "address"]
                                                 :notifications true}))
                  (fn [cred err]
                    (if err
                      (js/console.err err)
                      (re-frame/dispatch [::request-credential-success
                                          (js->clj cred :keywordize-keys true)]))))
                 db)))

   (re-frame/reg-event-db
    ::request-credential-success
    (fn-traced [db [_ credential]]
               (let [{:keys [:address :avatar]} credential
                     {:keys [abi networks]} (get-in db [:web3 :contract])
                     uport (:uport db)
                     web3-instance (.getWeb3 uport)
                     contract-instance (web3-eth/contract-at web3-instance abi address)
                     my-address (when (uport/is-mnid? address)
                                  (aget (uport/decode address) "address"))
                     is-rinkeby? (some-> (get-in db [:uport :instance])
                                         (aget "network")
                                         (aget "id")
                                         (= "0x4"))]
                 (-> db
                     (assoc-in [:web3 :web3-instance] web3-instance)
                     (assoc-in [:web3 :contract-instance] contract-instance)
                     (assoc-in [:web3 :my-address] my-address)
                     (assoc-in [:web3 :is-rinkeby?] is-rinkeby?)
                     (assoc :credential credential)))))

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
    (fn-traced [{:keys [db]} [_ address]]
               {:web3/call {:web3 (get-in db [:web3 :web3-instance])
                            :fns [{:instance (get-in db [:web3 :contract-instance])
                                   :fn :scoops-of
                                   :args [address]
                                   :on-success [::fetch-scoops-success]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-scoops-success
    (fn-traced [db [_ scoops]]
               (assoc db
                      :loading? false
                      :scoops (->> scoops
                                   (map #(let [id (first (aget % "c"))]
                                           (hash-map (keyword (str id)) {:id id})))
                                   (into {})))))

   (re-frame/reg-event-fx
    ::fetch-scoop
    (fn-traced [{:keys [db]} [_ id]]
               {:web3/call {:web3 (get-in db [:web3 :web3-instance])
                            :fns [{:instance (get-in db [:web3 :contract-instance])
                                   :fn :scoop
                                   :args [id]
                                   :on-success [::fetch-scoop-success]
                                   :on-error [::api-failure]}]}}))

   (re-frame/reg-event-db
    ::fetch-scoop-success
    (fn-traced [db [_ scoop]]
               (let [[id name timestamp image-hash price for-sale? meta-hash author] scoop
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
                 (update-in db [:scoops (keyword (str id))]
                            assoc :id id :name name :timestamp timestamp :image-hash image-hash
                            :price price :for-sale? for-sale? :author author :meta-hash meta-hash))))

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
                                                                  #(re-frame/dispatch [::mint-success %]))))))
               (assoc db :loading? {:message "Minting..."})))

   (re-frame/reg-event-db
    ::mint-success
    (fn-traced [db [_ res]]
               (re-frame/dispatch [::fetch-scoops (get-in db [:web3 :my-address])])
               (dissoc db :loading?)))

   (re-frame/reg-event-db
    ::update-meta
    (fn-traced [db [_ web3 id hash]]
               (web3-eth/contract-call (get-in web3 [:contract-instance])
                                       :set-token-meta-data-uri
                                       id hash
                                       {:gas 4700000
                                        :gas-price 100000000000}
                                       (fn [err tx-hash]
                                         (web3/wait-for-mined web3 tx-hash
                                                              #(js/console.log "pending")
                                                              #(re-frame/dispatch [::update-meta-success %]))))
               (assoc db :loading? {:message "Updating meta info..."})))

   (re-frame/reg-event-db
    ::update-meta-success
    (fn-traced [db [_ res]]
               (re-frame/dispatch [::fetch-scoops (get-in db [:web3 :my-address])])
               (dissoc db :loading?)))])

(defmethod ig/halt-key! :scoopmarket.module.events [_ _]
  (re-frame/clear-event))
