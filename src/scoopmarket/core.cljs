(ns scoopmarket.core
  (:require [integrant.core :as ig]
            [scoopmarket.config :as config]
            [goog.string :as gstring]))

(defonce system (atom nil))

(def config (atom (config/system-conf)))

(defn start []
  (reset! system (ig/init @config)))

(defn stop []
  (when @system
    (ig/halt! @system)
    (reset! system nil)))

(defn ^:export init []
  (start))
