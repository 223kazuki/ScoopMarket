(ns cljs.user
  (:require [scoopmarket.core :refer [system config start stop]]
            [meta-merge.core :refer [meta-merge]]))

(enable-console-print!)
(println "dev mode")

(defn dev-conf []
  {:scoopmarket.module/web3
   {:network-id 1533140371286
    :dev true}

   :scoopmarket.module/uport
   {:app-name "ScoopMarket"
    :client-id "2ongzbaHaEopuxDdxrCvU1XZqWt16oir144"
    :network "rinkeby"
    :signing-key "f5dc5848640a565994f9889d9ddda443a2fcf4c3d87aef3a74c54c4bcadc8ebd"}

   :scoopmarket.module/ipfs
   {:protocol "http"
    :host "localhost"
    :port 5001
    :endpoint "http://localhost:8080/ipfs/"}

   :scoopmarket.module/app
   {:dev true}})

(swap! config #(meta-merge % (dev-conf)))

(defn reset []
  (stop)
  (start))
