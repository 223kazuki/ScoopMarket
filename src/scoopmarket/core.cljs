(ns scoopmarket.core
  (:require [integrant.core :as ig]
            [scoopmarket.config :as config]
            [ajax.core :refer [GET]]))

(defonce system (atom nil))

(defn start []
  (if (-> js/location.host
          (clojure.string/split #":")
          first
          (= "localhost"))
    (GET "/contracts/ScoopMarket.json"
         {:response-format :json
          :keywords? true
          :handler #(reset! system (ig/init (config/system-conf %)))})
    (reset! system (ig/init (config/system-conf nil)))))

(defn stop []
  (ig/halt! @system)
  (reset! system nil))

(defn reset []
  (stop)
  (start))

(defn ^:export init []
  (start))
