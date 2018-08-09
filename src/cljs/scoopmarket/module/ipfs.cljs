(ns scoopmarket.module.ipfs
  (:require [integrant.core :as ig]
            [cljsjs.ipfs]))

(defmethod ig/init-key :scoopmarket.module.ipfs [_ opts]
  (js/IpfsApi (clj->js opts)))

(defmethod ig/halt-key! :scoopmarket.module.ipfs [_ _])
