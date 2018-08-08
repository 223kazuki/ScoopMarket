(ns scoopmarket.ipfs
  (:require [integrant.core :as ig]
            [cljsjs.ipfs]))

(defmethod ig/init-key ::module [_ opts]
  (js/IpfsApi (clj->js opts)))

(defmethod ig/halt-key! ::module [_ _])
