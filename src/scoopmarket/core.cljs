(ns scoopmarket.core
  (:require [integrant.core :as ig]
            [scoopmarket.config :as config]
            [goog.string :as gstring]
            [goog.string.format]
            [ajax.core :refer [GET]]))

(defonce system (atom nil))

(defn start []
  (let [localhost? (-> js/location.host
                       (clojure.string/split #":")
                       first
                       (= "localhost"))]
    (if localhost?
      (let [abi-uri (if-let [ipfs-hash
                             (.. js/document (querySelector "meta[name=ipfs-hash]"))]
                      (gstring/format "/ipfs/%s/contracts/ScoopMarket.json"
                                      (.getAttribute ipfs-hash "content"))
                      "/contracts/ScoopMarket.json")]
        (GET abi-uri {:response-format :json
                      :keywords? true
                      :handler #(reset! system (ig/init (config/system-conf localhost? %)))}))
      (reset! system (ig/init (config/system-conf localhost? nil))))))

(defn stop []
  (ig/halt! @system)
  (reset! system nil))

(defn reset []
  (stop)
  (start))

(defn ^:export init []
  (start))
