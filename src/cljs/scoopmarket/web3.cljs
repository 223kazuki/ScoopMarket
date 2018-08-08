(ns scoopmarket.web3
  (:require [integrant.core :as ig]
            [cljs-web3.core :as web3]))

(defmethod ig/init-key ::module [_ opts]
  (aget js/window "web3"))

(defmethod ig/halt-key! ::module [_ _])
