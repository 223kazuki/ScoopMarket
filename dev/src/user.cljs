(ns cljs.user
  (:require [scoopmarket.core :refer [system config start stop]]
            [meta-merge.core :refer [meta-merge]])
  (:require-macros [scoopmarket.utils :refer [read-config]]))

(enable-console-print!)

(println "dev mode")

(swap! config #(meta-merge % (read-config "dev.edn")))

(defn reset []
  (stop)
  (start))

(defn ^:export init []
  (start))
