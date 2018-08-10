(ns scoopmarket.core
  (:require [integrant.core :as ig]
            [scoopmarket.config :as config]))

(defonce system (atom nil))

(defn start []
  (reset! system (ig/init config/system-conf)))

(defn stop []
  (ig/halt! @system)
  (reset! system nil))

(defn reset []
  (stop)
  (start))

(defn ^:export init []
  (start))
