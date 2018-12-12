(ns scoopmarket.module.ipfs
  (:require [integrant.core :as ig]
            [cljsjs.ipfs]
            [cljsjs.buffer]))

(def Buffer js/buffer.Buffer)

(defn upload-data [{:keys [:ipfs]} buffer uploaded-handler]
  (.then (js-invoke ipfs "add" buffer)
         (fn [response]
           (uploaded-handler (aget (first response) "hash")))))

(defn get-url [{:keys [:endpoint]} hash]
  (str endpoint hash))

(defmethod ig/init-key :scoopmarket.module/ipfs [_ opts]
  {:ipfs (js/IpfsApi (clj->js opts))
   :endpoint (:endpoint opts)})

(defmethod ig/halt-key! :scoopmarket.module/ipfs [_ _])
