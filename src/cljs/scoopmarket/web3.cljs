(ns scoopmarket.web3
  (:require [integrant.core :as ig]
            [cljs-web3.core :as web3]))

(defmethod ig/init-key ::module [_ {:keys [contract-uri dev network-id]}]
  (when-let [instance (aget js/window "web3")]
    {:instance instance
     :network-id network-id
     :my-address (aget instance "eth" "defaultAccount")
     :is-rinkeby? (or (some-> instance
                              (aget "currentProvider")
                              (aget "publicConfigStore")
                              (aget "_state")
                              (aget "networkVersion")
                              (= "4"))
                      dev)
     :contract {:uri contract-uri}}))

(defmethod ig/halt-key! ::module [_ _])
