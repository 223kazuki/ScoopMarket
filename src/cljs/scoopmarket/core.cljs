(ns scoopmarket.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [integrant.core :as ig]
            [scoopmarket.events :as events]
            [scoopmarket.views :as views]
            [scoopmarket.config :as config]
            [scoopmarket.routes :as routes]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defonce system (atom nil))

(defmethod ig/init-key ::db
  [_ _]
  (re-frame/dispatch-sync [::events/initialize-db]))

(defmethod ig/init-key ::routes
  [_ _]
  (routes/app-routes))

(defmethod ig/init-key ::app
  [_ _]
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(def system-conf
  {::db nil
   ::routes nil
   ::app {:db (ig/ref ::db)
          :routes (ig/ref ::routes)}})

(defn start []
  (dev-setup)
  (reset! system (ig/init system-conf)))

(defn stop []
  (ig/halt! @system)
  (reset! system nil))

(defn reset []
  (stop)
  (start))

(defn ^:export init []
  (start))
