(ns scoopmarket.handler.index
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response]
            [clojure.java.io :as io]
            [integrant.core :as ig]))

(defmethod ig/init-key :scoopmarket.handler/index [_ options]
  (fn [{[_] :ataraxy/result}]
    [::response/ok (slurp (io/resource "scoopmarket/public/index.html"))]))
