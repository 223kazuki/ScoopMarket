(ns scoopmarket.module.ipfs
  (:require [integrant.core :as ig]
            [cljsjs.ipfs]
            [cljsjs.buffer]))

(def Buffer js/buffer.Buffer)

(defn upload-data [ipfs buffer uploaded-handler]
  (.then (js-invoke ipfs "add" buffer)
         (fn [response]
           (uploaded-handler (aget (first response) "hash")))))

(defmethod ig/init-key :scoopmarket.module.ipfs [_ opts]
  (js/IpfsApi (clj->js opts)))

(defmethod ig/halt-key! :scoopmarket.module.ipfs [_ _])
