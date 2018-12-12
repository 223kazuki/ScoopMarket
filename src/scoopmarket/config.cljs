(ns scoopmarket.config
  (:require [integrant.core :as ig]
            [goog.string :as gstring]
            [goog.string.format]
            [scoopmarket.module.app]
            [scoopmarket.module.events]
            [scoopmarket.module.subs]
            [scoopmarket.module.web3]
            [scoopmarket.module.uport]
            [scoopmarket.module.ipfs]
            [scoopmarket.module.routes])
  (:require-macros [scoopmarket.macro :refer [json]]))

(defn system-conf []
  {:scoopmarket.module/web3
   {:network-id 4
    :contract-json (json "build/contracts/ScoopMarket.json")
    :dev false}

   :scoopmarket.module/uport
   {:app-name "ScoopMarket"
    :client-id "2ongzbaHaEopuxDdxrCvU1XZqWt16oir144"
    :network "rinkeby"
    :signing-key "f5dc5848640a565994f9889d9ddda443a2fcf4c3d87aef3a74c54c4bcadc8ebd"}

   :scoopmarket.module/ipfs
   {:protocol "https"
    :host "ipfs.infura.io"
    :port 5001
    :endpoint "https://ipfs.infura.io/ipfs/"}

   :scoopmarket.module/events {}

   :scoopmarket.module/subs {}

   :scoopmarket.module/routes
   {:routes ["/" {""                       :mypage
                  ["verify/" [#"\d+" :id]] :verify
                  "market"                 :market}]
    :subs (ig/ref :scoopmarket.module/subs)
    :events (ig/ref :scoopmarket.module/events)}

   :scoopmarket.module/app
   {:initial-db {:active-page {:panel :none}
                 :sidebar-opened false
                 :loading? true
                 :ipfs (ig/ref :scoopmarket.module/ipfs)
                 :web3 (ig/ref :scoopmarket.module/web3)
                 :uport (ig/ref :scoopmarket.module/uport)}
    :dev false
    :routes (ig/ref :scoopmarket.module/routes)}})
