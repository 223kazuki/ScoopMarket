(ns scoopmarket.events
  (:require [re-frame.core :as re-frame]
            [integrant.core :as ig]
            [scoopmarket.db :as db]
            [scoopmarket.config :as conf]
            [day8.re-frame.http-fx]
            [district0x.re-frame.web3-fx]
            [ajax.core :as ajax]
            [ajax.protocols :as protocol]
            [cljsjs.web3]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljsjs.moment]
            [goog.string :as gstring]
            [goog.string.format]
            [cljsjs.buffer]
            [clojure.core.async :refer [go <! timeout]]))

(def is-mnid? (aget js/window "uportconnect" "MNID" "isMNID"))
(def encode (aget js/window "uportconnect" "MNID" "encode"))
(def decode (aget js/window "uportconnect" "MNID" "decode"))

(defn wait-for-mined [web3 tx-hash pending-cb success-cb]
  (letfn [(polling-loop []
            (go
              (<! (timeout 1000))
              (web3-eth/get-transaction
               web3 tx-hash
               (fn [err res]
                 (when-not err
                   (wait-for-mined (js->clj res)))))))
          (wait-for-mined [res]
            (if (:block-number res)
              (success-cb res)
              (do
                (pending-cb res)
                (polling-loop))))]
    (wait-for-mined {:block-number nil})))

(defmethod ig/init-key ::module [_ _]
  (re-frame/reg-event-fx
   ::initialize-db
   (fn [{:keys [db]} _]
     (let [ipfs-hash (.. js/document
                         (querySelector "meta[name=ipfs-hash]"))]
       {:db db/default-db
        :http-xhrio {:method :get
                     :uri (if ipfs-hash
                            (gstring/format "/ipfs/%s/contracts/%s.json"
                                            (.getAttribute ipfs-hash "content")
                                            (get-in db/default-db [:contract :name]))
                            (gstring/format "/contracts/%s.json"
                                            (get-in db/default-db [:contract :name])))
                     :timeout 6000
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [::abi-loaded]
                     :on-failure [::api-failure]}})))

  (re-frame/reg-event-db
   ::connect-uport
   (fn [db _]
     (let [Connect (aget js/window "uportconnect" "Connect")
           SimpleSigner (aget js/window "uportconnect" "SimpleSigner")
           uport (Connect. "Kazuki's new app"
                           (clj->js {:clientId "2ongzbaHaEopuxDdxrCvU1XZqWt16oir144"
                                     :network "rinkeby"
                                     :signer (SimpleSigner "f5dc5848640a565994f9889d9ddda443a2fcf4c3d87aef3a74c54c4bcadc8ebd")}))]
       (.then
        (.requestCredentials uport
                             (clj->js {:requested ["name" "avatar" "address"]
                                       :notifications true}))
        (fn [cred err]
          (if err
            (js/console.err err)
            (re-frame/dispatch [::request-credential-success (js->clj cred
                                                                      :keywordize-keys true)]))))
       (assoc db
              :uport uport
              :abi-loaded false
              :my-address nil
              :loading? nil))))

  (re-frame/reg-event-db
   ::request-credential-success
   (fn [db [_ credential]]
     (re-frame/dispatch [::abi-loaded (:contract db)])
     (let [{:keys [:address :avatar]} credential
           uport (:uport db)
           address (when (is-mnid? address)
                     (aget (decode address) "address"))]
       (assoc db
              :web3 (.getWeb3 uport)
              :credential credential
              :network-address address
              :my-address address))))

  (re-frame/reg-event-db
   ::abi-loaded
   (fn [db [_ {:keys [abi networks]}]]
     (if (:web3 db)
       (let [web3 (:web3 db)
             network-id (keyword (str conf/network-id))
             ;; TODO: Specify network ID.
             address (-> networks network-id :address)
             instance (web3-eth/contract-at web3 abi address)]
         (re-frame/dispatch [::fetch-scoops (:my-address db)])
         (-> db
             (assoc-in [:contract :abi] abi)
             (assoc-in [:contract :networks] networks)
             (assoc-in [:contract :address] address)
             (assoc-in [:contract :instance] instance)
             (assoc :is-rinkeby? (or (some-> (:web3 db)
                                             (aget "currentProvider")
                                             (aget "publicConfigStore")
                                             (aget "_state")
                                             (aget "networkVersion")
                                             (= "4" ))
                                     (some-> (:uport db)
                                             (aget "network")
                                             (aget "id")
                                             (= "0x4"))))
             (assoc :abi-loaded true)
             (dissoc :loading?)))
       (-> db
           (assoc-in [:contract :abi] abi)
           (assoc-in [:contract :networks] networks)
           (dissoc :loading?)))))

  (re-frame/reg-event-db
   ::toggle-sidebar
   (fn [db _]
     (update-in db [:sidebar-opened] not)))

  (re-frame/reg-event-db
   ::set-active-panel
   (fn [db [_ active-panel]]
     (-> db
         (assoc :active-panel active-panel))))

  (re-frame/reg-event-db
   ::api-failure
   (fn [db [_ result]]
     (js/console.log "Api failure." result)
     (-> db
         (assoc :message {:status :error :text (str result)})
         (dissoc :loading?))))

  (re-frame/reg-event-fx
   ::fetch-scoops
   (fn [{:keys [db]} [_ address]]
     {:web3/call {:web3 (:web3 db)
                  :fns [{:instance (get-in db [:contract :instance])
                         :fn :scoops-of
                         :args [address]
                         :on-success [::fetch-scoops-success]
                         :on-error [::api-failure]}]}}))

  (re-frame/reg-event-db
   ::fetch-scoops-success
   (fn [db [_ scoops]]
     (assoc db :scoops (->> scoops
                            (map #(let [id (first (aget % "c"))]
                                    (hash-map (keyword (str id)) {:id id})))
                            (into {})))))

  (re-frame/reg-event-fx
   ::fetch-scoop
   (fn [{:keys [db]} [_ id]]
     {:web3/call {:web3 (:web3 db)
                  :fns [{:instance (get-in db [:contract :instance])
                         :fn :scoop
                         :args [id]
                         :on-success [::fetch-scoop-success]
                         :on-error [::api-failure]}]}}))

  (re-frame/reg-event-db
   ::fetch-scoop-success
   (fn [db [_ scoop]]
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
   (fn [db [_ id meta]]
     (assoc-in db [:scoops (keyword (str id)) :meta] meta)))

  (re-frame/reg-event-db
   ::upload-image
   (fn [db [_ reader]]
     (let [buffer (js/buffer.Buffer.from (aget reader "result"))
           add (aget (:ipfs db) "add")]
       (.then (add buffer)
              (fn [response]
                (re-frame/dispatch [::ready-to-mint (aget (first response) "hash")]))))
     (assoc db :loading? {:message "Uploading file to IPFS..."})))

  (re-frame/reg-event-db
   ::ready-to-mint
   (fn [db [_ hash]]
     (-> db
         (dissoc :loading?)
         (assoc-in [:form :new-scoop/image-hash] hash))))

  (re-frame/reg-event-db
   ::mint
   (fn [db [_]]
     (let [{:keys [:web3 :contract :network-address :form]} db
           {:keys [:new-scoop/image-hash :new-scoop/name
                   :new-scoop/price :new-scoop/for-sale?]} form
           price (if-not (empty? price)
                   (js/parseInt price) 0)]
       (web3-eth/contract-call (:instance contract)
                               :mint (or name "") price (or for-sale? false) image-hash
                               {:gas 4700000
                                :gas-price 100000000000
                                ;; TODO: Specify value.
                                :value 10000000000000000}
                               (fn [err tx-hash]
                                 (if err
                                   (js/console.log err)
                                   (wait-for-mined web3 tx-hash
                                                   #(js/console.log "pending")
                                                   #(re-frame/dispatch [::mint-success %]))))))
     (assoc db :loading? {:message "Minting..."})))

  (re-frame/reg-event-db
   ::mint-success
   (fn [db [_ res]]
     (re-frame/dispatch [::fetch-scoops (:my-address db)])
     (dissoc db :loading? :form)))

  (re-frame/reg-event-db
   ::add-scoop-tag
   (fn [db [_ id tag]]
     (-> db
         (update-in [:scoops (keyword (str id)) :meta :tags] #(set (conj % tag)))
         (update-in [:form] dissoc (keyword (str "new-tag/" id))))))

  (re-frame/reg-event-db
   ::delete-scoop-tag
   (fn [db [_ id tag]]
     (-> db
         (update-in [:scoops (keyword (str id)) :meta :tags] #(set (remove #{tag} %))))))

  (re-frame/reg-event-db
   ::update-form
   (fn [db [_ name update-fn]]
     (update-in db [:form (keyword name)] update-fn)))

  (re-frame/reg-event-db
   ::upload-meta
   (fn [db [_ id]]
     (let [meta (get-in db [:scoops (keyword (str id)) :meta])
           meta-str (.stringify js/JSON (clj->js meta))
           buffer (js/buffer.Buffer.from meta-str)
           add (aget (:ipfs db) "add")]
       (.then (add buffer)
              (fn [response]
                (re-frame/dispatch [::update-meta id (aget (first response) "hash")]))))
     (assoc db :loading? {:message "Updating meta info..."})))

  (re-frame/reg-event-db
   ::update-meta
   (fn [db [_ id hash]]
     (let [{:keys [:web3 :contract :network-address]} db]
       (web3-eth/contract-call (:instance contract)
                               :set-token-meta-data-uri
                               id hash
                               {:gas 4700000
                                :gas-price 100000000000}
                               (fn [err tx-hash]
                                 (wait-for-mined web3 tx-hash
                                                 #(js/console.log "pending")
                                                 #(re-frame/dispatch [::update-meta-success %]))))
       (assoc db :loading? true))))

  (re-frame/reg-event-db
   ::update-meta-success
   (fn [db [_ res]]
     (re-frame/dispatch [::fetch-scoops (:my-address db)])
     (dissoc db :loading?))))

(defmethod ig/halt-key! ::module [_ _]
  (re-frame/clear-event))
