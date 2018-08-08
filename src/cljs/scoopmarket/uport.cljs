(ns scoopmarket.uport
  (:require [integrant.core :as ig]
            [cljs-web3.core :as web3]))

(def is-mnid? (aget js/window "uportconnect" "MNID" "isMNID"))
(def encode (aget js/window "uportconnect" "MNID" "encode"))
(def decode (aget js/window "uportconnect" "MNID" "decode"))

(defmethod ig/init-key ::module [_ {:keys [app-name client-id network signing-key] :as opts}]
  (let [Connect (aget js/window "uportconnect" "Connect")
        SimpleSigner (aget js/window "uportconnect" "SimpleSigner")
        uport (Connect. app-name
                        (clj->js {:clientId client-id
                                  :network network
                                  :signer (SimpleSigner signing-key)}))]
    (assoc opts :instance uport)))

(defmethod ig/halt-key! ::module [_ _])
