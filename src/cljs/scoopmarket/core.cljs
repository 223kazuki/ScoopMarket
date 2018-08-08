(ns scoopmarket.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [integrant.core :as ig]
            [scoopmarket.events :as events]
            [scoopmarket.subs :as subs]
            [scoopmarket.views :as views]
            [scoopmarket.config :as config]
            [scoopmarket.routes :as routes]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defonce system (atom nil))


(defmethod ig/init-key ::app
  [_ {:keys [routes] :as opts}]
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel opts]
                  (.getElementById js/document "app")))

(def system-conf
  {::events/module nil
   ::subs/module nil
   ::routes/module {:routes ["/" {""       :mypage
                                  "market" :market}]
                    :subs (ig/ref ::subs/module)
                    :events (ig/ref ::events/module)}
   ::app {:routes (ig/ref ::routes/module)}})

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
