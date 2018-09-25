(ns scoopmarket.module.ipfs
  (:require [integrant.core :as ig]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljsjs.ipfs]
            [goog.string :as gstring]
            [goog.string.format]))

;; Initial DB
(def initial-db {::ipfs nil})

;; Subscriptions
(defmulti reg-sub identity)
(defmethod reg-sub ::ipfs-url [k]
  (re-frame/reg-sub
   k (fn [db [_ hash]]
       (let [{:keys [:endpoint]} (::ipfs-opts db)]
         (gstring/format "%s/%s" endpoint hash)))))
(defmethod reg-sub ::json [k]
  (re-frame/reg-sub
   k (fn [db [_ hash]]
       (re-frame/dispatch [::fetch-json hash])
       (get-in db [::ipfs-data hash]))))

;; Events
(defmulti reg-event identity)
(defmethod reg-event ::init [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db [ipfs ipfs-opts]]
    (-> db
        (merge initial-db)
        (assoc ::ipfs ipfs ::ipfs-opts ipfs-opts)))))
(defmethod reg-event ::halt [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db _]
    (->> db
         (filter #(not= (namespace (key %)) (namespace ::x)))
         (into {})))))
(defmethod reg-event ::fetch-json [k]
  (re-frame/reg-event-fx
   k [re-frame/trim-v]
   (fn-traced
    [{:keys [:db]} [hash]]
    (let [{:keys [::ipfs]} db]
      {:db db
       ::cat-json {:hash hash
                   :on-success [::fetch-json-success hash]}}))))
(defmethod reg-event ::fetch-json-success [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db [hash result]]
    (assoc-in db [::ipfs-data hash] result))))

;; Effects
(defmulti reg-fx identity)
(defmethod reg-fx ::cat-json [k ipfs]
  (re-frame/reg-fx
   k (fn [{:keys [:hash :on-success :on-error] :as params}]
       (js-invoke ipfs "cat" hash
                  (fn [err file]
                    (if err
                      (re-frame/dispatch (conj on-error err))
                      (when file
                        (let [data (js->clj (.parse js/JSON
                                                    (.toString file "utf8"))
                                            :keywordize-keys true)]
                          (re-frame/dispatch (conj on-success data))))))))))

;; Init
(defmethod ig/init-key :scoopmarket.module/ipfs
  [k opts]
  (js/console.log (str "Initializing " k))
  (let [ipfs (js/IpfsApi (clj->js opts))
        subs (->> reg-sub methods (map key))
        events (->> reg-event methods (map key))
        effects (->> reg-fx methods (map key))]
    (->> subs (map reg-sub) doall)
    (->> events (map reg-event) doall)
    (->> effects (map #(reg-fx % ipfs)) doall)
    (re-frame/dispatch-sync [::init ipfs opts])
    {:subs subs :events events :effects effects :ipfs ipfs}))

;; Halt
(defmethod ig/halt-key! :scoopmarket.module/ipfs
  [k {:keys [:subs :events :effects :ipfs]}]
  (js/console.log (str "Halting " k))
  (re-frame/dispatch-sync [::halt])
  (->> subs (map re-frame/clear-sub) doall)
  (->> events (map re-frame/clear-event) doall)
  (->> effects (map re-frame/clear-fx) doall))
