(ns scoopmarket.client.events
  (:require [re-frame.core :as re-frame]
            [scoopmarket.client.db :as db]
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
            [cljsjs.buffer]))

(re-frame/reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db
    :http-xhrio {:method :get
                 :uri (gstring/format "%s.json"
                                      (get-in db/default-db [:contract :name]))
                 :timeout 6000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::abi-loaded]
                 :on-failure [::api-failure]}}))

(re-frame/reg-event-db
 ::abi-loaded
 (fn [db [_ {:keys [abi networks]}]]
   (let [web3 (:web3 db)
         ;; TODO: Specify network ID.
         address (-> networks first val :address)
         instance (web3-eth/contract-at web3 abi address)]
     (re-frame/dispatch [::fetch-my-address])
     (-> db
         (assoc-in [:contract :abi] abi)
         (assoc-in [:contract :address] address)
         (assoc-in [:contract :instance] instance)
         (assoc :abi-loaded true)
         (dissoc :loading?)))))

(re-frame/reg-event-fx
 ::fetch-my-address
 (fn [{:keys [:db]} _]
   {:web3/call {:web3 (:web3 db)
                :fns [{:fn cljs-web3.eth/accounts
                       :args []
                       :on-success [::fetch-my-address-success]
                       :on-error [::api-failure]}]}}))

(re-frame/reg-event-db
 ::fetch-my-address-success
 (fn [db [_ [address]]]
   (-> db
       (assoc :my-address address))))

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
 ::fetch-my-scoops
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
   (let [[id hash] scoop
         id (first (aget id "c"))
         cat (aget (:ipfs db) "cat")]
     (cat (or hash
              ;; TODO: not found image
              "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr")
          (fn [_ bytes]
            ;; TODO: Not only jpeg.
            (let [blob (js/Blob. (clj->js [bytes]) (clj->js {:type "image/jpeg"}))
                  image-url (js/window.URL.createObjectURL (clj->js blob))]
              (re-frame/dispatch [::fetch-image-success id image-url]))))
     db)))

(re-frame/reg-event-db
 ::fetch-image-success
 (fn [db [_ id image-url]]
   (assoc-in db [:scoops (keyword (str id)) :image-url] image-url)))

(re-frame/reg-event-db
 ::upload-image
 (fn [db [_ reader]]
   (let [buffer (js/buffer.Buffer.from (aget reader "result"))
         add (aget (:ipfs db) "add")]
     (.then (add buffer)
            (fn [response]
              (re-frame/dispatch [::mint (aget (first response) "hash")]))))
   db))

(re-frame/reg-event-fx
 ::mint
 (fn [{:keys [db]} [_ hash]]
   {:web3/call {:web3 (:web3 db)
                :fns [{:instance (get-in db [:contract :instance])
                       :fn :mint
                       :args [hash]
                       :tx-opts {:gas 4700000
                                 :gas-price 100000000000
                                 ;; TODO: Specify value.
                                 :value 10000000000000000}
                       :on-tx-hash [::tx-hash]
                       :on-tx-hash-error [::api-failure]
                       :on-tx-success [::tx-success]
                       :on-tx-error [::api-failure]
                       :on-tx-receipt [::tx-receipt]}]}}))

(re-frame/reg-event-db
 ::tx-hash
 (fn [db [_ result]]
   (js/console.log (pr-str result))
   (assoc db :message {:status :info :text "Sending transaction..."} :loading? true)))

(re-frame/reg-event-db
 ::tx-success
 (fn [db [_ result]]
   (js/console.log (pr-str result))
   (-> db
       (assoc :message {:status :info :text "Transation was written in blockchain successfully." :transaction-result result})
       (dissoc :loading? :form))))

(re-frame/reg-event-db
 ::tx-receipt
 (fn [db [_ result]]
   (js/console.log (pr-str result))
   (assoc db :message {:status :info :text {:status "info" :text "Transaction was sent."}})))
