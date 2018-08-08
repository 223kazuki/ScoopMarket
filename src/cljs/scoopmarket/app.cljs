(ns scoopmarket.app
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [integrant.core :as ig]
            [scoopmarket.events :as events]
            [scoopmarket.views :as views]))

(defn- dev-setup [dev]
  (when dev
    (enable-console-print!)
    (println "dev mode")))

(defmethod ig/init-key ::module
  [_ {:keys [initial-db dev] :as opts}]
  (dev-setup dev)
  (re-frame/dispatch-sync [::events/initialize-db initial-db])
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel opts]
                  (.getElementById js/document "app")))
