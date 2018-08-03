(ns scoopmarket.client.events
  (:require [re-frame.core :as re-frame]
            [scoopmarket.client.db :as db]
            [scoopmarket.client.config :as conf]
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

(re-frame/reg-event-fx
 ::initialize-db
 (fn [{:keys [db]} _]
   {:db db/default-db
    :http-xhrio {:method :get
                 :uri (gstring/format "%s.json"
                                      (get-in db/default-db [:contract :name]))
                 :timeout 6000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::abi-loaded]
                 :on-failure [::api-failure]}}))

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
                           (clj->js {:requested ["name" "phone" "country" "address"]
                                     :notifications true}))
      #(re-frame/dispatch [::request-credential-success %]))
     (assoc db
            :uport uport
            :web3 (.getWeb3 uport)
            :abi-loaded false
            :my-address nil
            :loading? false))))

(re-frame/reg-event-db
 ::request-credential-success
 (fn [db [_ credential]]
   (re-frame/dispatch [::abi-loaded (:contract db)])
   (let [network-address (aget credential "address")
         address (when (is-mnid? network-address )
                   (aget (decode network-address) "address"))]
     (assoc db
            :credential credential
            :network-address network-address
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
 (fn  [db [_ active-panel]]
   (-> db
       (assoc :active-panel active-panel))))

(re-frame/reg-event-db
 ::api-failure
 (fn [db [_ result]]
   (js/console.log "Api failure." result)
   (-> db
       (assoc :message {:status :error :text (str result)})
       (assoc :loading? false))))

(re-frame/reg-event-fx
 ::fetch-scoops
 (fn [{:keys [db]} [_ address]]
   {:web3/call {:web3 (:web3 db)
                :fns [{:instance (get-in db [:contract :instance])
                       :fn :scoops-of
                       :args [address]
                       :on-success [::fetch-my-scoops-success]
                       :on-error [::api-failure]}]}}))

(re-frame/reg-event-db
 ::fetch-my-scoops-success
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
   (let [[id timestamp image-hash meta-hash] scoop
         id (first (aget id "c"))
         cat (aget (:ipfs db) "cat")]
     (cat image-hash
          (fn [_ bytes]
            ;; TODO: Not only jpeg.
            (let [blob (js/Blob. (clj->js [bytes]) (clj->js {:type "image/jpeg"}))
                  image-url (js/window.URL.createObjectURL (clj->js blob))]
              (re-frame/dispatch [::fetch-image-success id image-url]))))
     (when-not (empty? meta-hash)
       (cat meta-hash
            (fn [_ file]
              (when file
                (let [meta (js->clj (.parse js/JSON
                                            (.toString file "utf8"))
                                    :keywordize-keys true)]
                  (re-frame/dispatch [::fetch-meta-success id meta]))))))
     db)))

(re-frame/reg-event-db
 ::fetch-image-success
 (fn [db [_ id image-url]]
   (assoc-in db [:scoops (keyword (str id)) :image-url] image-url)))

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
              (re-frame/dispatch [::mint (aget (first response) "hash")]))))
   (assoc db :loading? true)))

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

(re-frame/reg-event-db
 ::mint
 (fn [db [_ hash]]
   (let [{:keys [:web3 :contract :network-address]} db]
     (web3-eth/contract-call (:instance contract)
                             :mint hash  {:gas 4700000
                                          :gas-price 100000000000
                                          ;; TODO: Specify value.
                                          :value 10000000000000000}
                             (fn [err tx-hash]
                               (if err
                                 (js/console.log err)
                                 (wait-for-mined web3 tx-hash
                                                 #(js/console.log "pending")
                                                 #(re-frame/dispatch [::mint-success %]))))))
   (assoc db :loading? true)))

(re-frame/reg-event-db
 ::mint-success
 (fn [db [_ res]]
   (re-frame/dispatch [::fetch-scoops (:my-address db)])
   (assoc db :loading? false)))

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
   (assoc db :loading? true)))

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
   (assoc db :loading? false)))
