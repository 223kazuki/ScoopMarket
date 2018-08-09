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
            [scoopmarket.module.routes]))

(def debug? ^boolean goog.DEBUG)

(def system-conf
  {:scoopmarket.module.web3 {:network-id (if debug? 1533140371286 4)
                  :contract-uri
                  (if-let [ipfs-hash
                           (.. js/document (querySelector "meta[name=ipfs-hash]"))]
                    (gstring/format "/ipfs/%s/contracts/Scoop.json"
                                    (.getAttribute ipfs-hash "content"))
                    "/contracts/Scoop.json")
                  :dev debug?}
   :scoopmarket.module.uport {:app-name "Kazuki's new app"
                   :client-id "2ongzbaHaEopuxDdxrCvU1XZqWt16oir144"
                   :network "rinkeby"
                   :signing-key "f5dc5848640a565994f9889d9ddda443a2fcf4c3d87aef3a74c54c4bcadc8ebd"}
   :scoopmarket.module.ipfs {:protocol "https"
                  :host "ipfs.infura.io"
                  :port 5001}
   :scoopmarket.module.events {}
   :scoopmarket.module.subs {}
   :scoopmarket.module.routes {:routes ["/" {""                       :mypage
                                  ["verify/" [#"\d+" :id]] :verify
                                  "market"                 :market}]
                    :subs (ig/ref :scoopmarket.module.subs)
                    :events (ig/ref :scoopmarket.module.events)}
   :scoopmarket.module.app {:initial-db {:active-page {:panel :none}
                              :sidebar-opened false
                              :loading? true
                              :web3 (ig/ref :scoopmarket.module.web3)
                              :uport (ig/ref :scoopmarket.module.uport)
                              :ipfs (ig/ref :scoopmarket.module.ipfs)}
                 :dev debug?
                 :routes (ig/ref :scoopmarket.module.routes)}})
