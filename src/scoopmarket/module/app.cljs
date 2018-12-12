(ns scoopmarket.module.app
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [integrant.core :as ig]
            [scoopmarket.module.events :as events]
            [scoopmarket.views :as views]))

(defn- dev-setup [dev]
  (when dev
    (enable-console-print!)
    (println "dev mode")))

(defmethod ig/init-key :scoopmarket.module/app
  [_ {:keys [initial-db dev]}]
  (dev-setup dev)
  (re-frame/dispatch-sync [::events/initialize-db initial-db])
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/responsible-container]
                  (.getElementById js/document "app")))
