(ns scoopmarket.module.uport
  (:require [integrant.core :as ig]))

(def is-mnid? (aget js/window "uportconnect" "MNID" "isMNID"))
(def encode (aget js/window "uportconnect" "MNID" "encode"))
(def decode (aget js/window "uportconnect" "MNID" "decode"))

(defn request-credentials [uport success-handler]
  (.then
   (js-invoke uport "requestCredentials"
              (clj->js {:requested ["name" "avatar" "address"]
                        :notifications true}))
   (fn [cred err]
     (if err
       (js/console.err err)
       (success-handler (js->clj cred :keywordize-keys true))))))

(defmethod ig/init-key :scoopmarket.module/uport [_ {:keys [app-name client-id network signing-key] :as opts}]
  (let [Connect (aget js/window "uportconnect" "Connect")
        SimpleSigner (aget js/window "uportconnect" "SimpleSigner")]
    (Connect. app-name
              (clj->js {:clientId client-id
                        :network network
                        :signer (SimpleSigner signing-key)}))))

(defmethod ig/halt-key! :scoopmarket.module/uport [_ _])
