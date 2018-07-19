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
            [cljsjs.ipfs]))

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
 ::toggle-sidebar
 (fn [db _]
   (update-in db [:sidebar-opened] not)))

(re-frame/reg-event-db
 ::abi-loaded
 (fn [db [_ {:keys [abi networks]}]]
   (let [web3 (:web3 db)
         address (get-in networks [(keyword (str 0)) :address])
         instance (web3-eth/contract-at web3 abi address)]
     (js/console.log instance)
     (-> db
         (assoc-in [:contract :abi] abi)
         (assoc-in [:contract :address] address)
         (assoc-in [:contract :instance] instance)
         (dissoc :loading?)))))

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

(re-frame/reg-event-db
 ::fetch-image
 (fn [db [_ hash]]
   (let [ipfs (js/IpfsApi "/ip4/127.0.0.1/tcp/5001")]
     (.cat ipfs (or hash
                    "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr")
           (fn [_ bytes]
             (let [blob (js/Blob. (clj->js [bytes]) (clj->js {:type "image/jpeg"}))
                   image-url (js/window.webkitURL.createObjectURL (clj->js blob))]
               (re-frame/dispatch [::fetch-image-success image-url])))))
   db))

(re-frame/reg-event-db
 ::fetch-image-success
 (fn [db [_ image-url]]
   (assoc db :image-url image-url)))
