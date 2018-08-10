(ns scoopmarket.module.uport
  (:require [integrant.core :as ig]))

(def is-mnid? (aget js/window "uportconnect" "MNID" "isMNID"))
(def encode (aget js/window "uportconnect" "MNID" "encode"))
(def decode (aget js/window "uportconnect" "MNID" "decode"))

(defmethod ig/init-key :scoopmarket.module.uport [_ {:keys [app-name client-id network signing-key] :as opts}]
  (let [Connect (aget js/window "uportconnect" "Connect")
        SimpleSigner (aget js/window "uportconnect" "SimpleSigner")]
    (Connect. app-name
              (clj->js {:clientId client-id
                        :network network
                        :signer (SimpleSigner signing-key)}))))

(defmethod ig/halt-key! :scoopmarket.module.uport [_ _])
