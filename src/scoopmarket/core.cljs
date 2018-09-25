(ns scoopmarket.core
  (:require [integrant.core :as ig]
            [scoopmarket.module.app]
            [scoopmarket.module.router]
            [scoopmarket.module.web3]
            [scoopmarket.module.uport]
            [scoopmarket.module.ipfs])
  (:require-macros [scoopmarket.utils :refer [read-config]]))

(defonce system (atom nil))

(def config (atom (read-config "config.edn")))

(defn start []
  (reset! system (ig/init @config)))

(defn stop []
  (when @system
    (ig/halt! @system)
    (reset! system nil)))

(defn ^:export init []
  (start))

;; (defn start []
;;   (let [localhost? (-> js/location.host
;;                        (clojure.string/split #":")
;;                        first
;;                        (= "localhost"))]
;;     (if localhost?
;;       (let [abi-uri (if-let [ipfs-hash
;;                              (.. js/document (querySelector "meta[name=ipfs-hash]"))]
;;                       (gstring/format "/ipfs/%s/contracts/ScoopMarket.json"
;;                                       (.getAttribute ipfs-hash "content"))
;;                       "/contracts/ScoopMarket.json")]
;;         (GET abi-uri {:response-format :json
;;                       :keywords? true
;;                       :handler #(reset! system (ig/init (config/system-conf localhost? %)))}))
;;       (reset! system (ig/init (config/system-conf localhost? nil))))))
