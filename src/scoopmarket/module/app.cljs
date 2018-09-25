(ns scoopmarket.module.app
  (:require [integrant.core :as ig]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [scoopmarket.module.router :as router]
            [scoopmarket.views :as views]))

;; Initial DB
(def initial-db {})

;; Subscriptions
(defmulti reg-sub identity)

;; Events
(defmulti reg-event identity)
(defmethod reg-event ::init [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db _]
    (merge db initial-db))))
(defmethod reg-event ::halt [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db _]
    (->> db
         (filter #(not= (namespace (key %)) (namespace ::x)))
         (into {})))))
(defmethod reg-event ::toggle-sidebar [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced [db _]
              (update-in db [::sidebar-opened] not))))

;; Init
(defmethod ig/init-key :scoopmarket.module/app
  [k {:keys [:mount-point-id]}]
  (js/console.log (str "Initializing " k))
  (let [subs (->> reg-sub methods (map key))
        events (->> reg-event methods (map key))
        container (.getElementById js/document mount-point-id)]
    (->> subs (map reg-sub) doall)
    (->> events (map reg-event) doall)
    (re-frame/dispatch-sync [::init])
    (when container (reagent/render [views/app-container] container))
    {:subs subs :events events :container container}))

;; Halt
(defmethod ig/halt-key! :scoopmarket.module/app
  [k {:keys [:subs :events :container]}]
  (js/console.log (str "Halting " k))
  (reagent/unmount-component-at-node container)
  (re-frame/dispatch-sync [::halt])
  (->> subs (map re-frame/clear-sub) doall)
  (->> events (map re-frame/clear-event) doall))
