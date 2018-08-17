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
  (:require-macros [scoopmarket.macro :refer [slurp]]))

(defn get-contract-json []
  (-> (slurp "resources/public/contracts/ScoopMarket.json")
      (js/JSON.parse)
      (js->clj :keywordize-keys true)))

(def debug? ^boolean goog.DEBUG)

(defn system-conf [contract-json]
  {:scoopmarket.module.web3
   {:network-id (if debug? 1533140371286 4)
    :contract-json (or contract-json
                       (get-contract-json))
    :dev debug?}

   :scoopmarket.module.uport
   {:app-name "Kazuki's new app"
    :client-id "2ongzbaHaEopuxDdxrCvU1XZqWt16oir144"
    :network "rinkeby"
    :signing-key "f5dc5848640a565994f9889d9ddda443a2fcf4c3d87aef3a74c54c4bcadc8ebd"}

   :scoopmarket.module.ipfs
   {:protocol "https"
    :host "ipfs.infura.io"
    :port 5001}

   :scoopmarket.module.events {}

   :scoopmarket.module.subs {}

   :scoopmarket.module.routes
   {:routes ["/" {""                       :mypage
                  ["verify/" [#"\d+" :id]] :verify
                  "market"                 :market}]
    :subs (ig/ref :scoopmarket.module.subs)
    :events (ig/ref :scoopmarket.module.events)}

   :scoopmarket.module.app
   {:initial-db {:active-page {:panel :none}
                 :sidebar-opened false
                 :loading? true
                 :ipfs (ig/ref :scoopmarket.module.ipfs)
                 :web3 (ig/ref :scoopmarket.module.web3)
                 :uport (ig/ref :scoopmarket.module.uport)}
    :dev debug?
    :routes (ig/ref :scoopmarket.module.routes)}})
