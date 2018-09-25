(ns scoopmarket.module.uport
  (:require [integrant.core :as ig]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [scoopmarket.module.web3 :as web3]
            [uport-connect]))

;; Initial DB
(def initial-db {::uport nil})

;; Subscriptions
(defmulti reg-sub identity)
(defmethod reg-sub ::credential [k]
  (re-frame/reg-sub k #(::credential %)))

;; Events
(defmulti reg-event identity)
(defmethod reg-event ::init [k]
  (re-frame/reg-event-fx
   k [re-frame/trim-v]
   (fn-traced
    [{:keys [db]} [uport]]
    (let [{:keys [::web3/web3-instance]} db]
      (if web3-instance
        {:db (assoc db ::uport uport)}
        {:db (assoc db ::uport uport)
         :dispatch [::connect-uport]})))))
(defmethod reg-event ::halt [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db _]
    (->> db
         (filter #(not= (namespace (key %)) (namespace ::x)))
         (into {})))))
(defmethod reg-event ::connect-uport [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced [db _]
              (let [uport (::uport db)]
                (.then
                 (js-invoke uport "requestCredentials"
                            (clj->js {:requested ["name" "avatar" "address"]
                                      :notifications true}))
                 (fn [cred err]
                   (if err
                     (js/console.err err)
                     (re-frame/dispatch [::connect-uport-success
                                         (js->clj cred :keywordize-keys true)]))))
                db))))
(defmethod reg-event ::connect-uport-success [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced [db [credential]]
              (let [uport (::uport db)
                    web3-instance (js-invoke uport "getWeb3")]
                (re-frame/dispatch [::web3/reconnect web3-instance])
                (assoc db ::credential credential)))))

(defmethod ig/init-key :scoopmarket.module/uport
  [k {:keys [app-name client-id network signing-key dev web3] :as opts}]
  (js/console.log (str "Initializing " k))
  (let [subs (->> reg-sub methods (map key))
        events (->> reg-event methods (map key))
        Connect (aget js/uportconnect "Connect")
        SimpleSigner (aget js/uportconnect "SimpleSigner")
        uport (Connect. app-name
                        (clj->js {:clientId client-id
                                  :network network
                                  :signer (SimpleSigner signing-key)}))]
    (->> subs (map reg-sub) doall)
    (->> events (map reg-event) doall)
    (re-frame/dispatch-sync [::init uport])
    {:subs subs :events events}))

(defmethod ig/halt-key! :scoopmarket.module/uport
  [k {:keys [:subs :events]}]
  (js/console.log (str "Halting " k))
  (re-frame/dispatch-sync [::halt])
  (->> subs (map re-frame/clear-sub) doall)
  (->> events (map re-frame/clear-event) doall))
