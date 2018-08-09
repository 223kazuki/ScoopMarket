(ns scoopmarket.config
  (:require [integrant.core :as ig]
            [goog.string :as gstring]
            [goog.string.format]
            [scoopmarket.app :as app]
            [scoopmarket.events :as events]
            [scoopmarket.subs :as subs]
            [scoopmarket.web3 :as web3]
            [scoopmarket.uport :as uport]
            [scoopmarket.ipfs :as ipfs]
            [scoopmarket.routes :as routes]))

(def debug? ^boolean goog.DEBUG)

(def system-conf
  {::web3/module {:network-id (if debug? 1533140371286 4)
                  :contract-uri
                  (if-let [ipfs-hash
                           (.. js/document (querySelector "meta[name=ipfs-hash]"))]
                    (gstring/format "/ipfs/%s/contracts/Scoop.json"
                                    (.getAttribute ipfs-hash "content"))
                    "/contracts/Scoop.json")
                  :dev debug?}
   ::uport/module {:app-name "Kazuki's new app"
                   :client-id "2ongzbaHaEopuxDdxrCvU1XZqWt16oir144"
                   :network "rinkeby"
                   :signing-key "f5dc5848640a565994f9889d9ddda443a2fcf4c3d87aef3a74c54c4bcadc8ebd"}
   ::ipfs/module {:protocol "https"
                  :host "ipfs.infura.io"
                  :port 5001}
   ::events/module {}
   ::subs/module {}
   ::routes/module {:routes ["/" {""                       :mypage
                                  ["verify/" [#"\d+" :id]] :verify
                                  "market"                 :market}]
                    :subs (ig/ref ::subs/module)
                    :events (ig/ref ::events/module)}
   ::app/module {:initial-db {:active-page {:panel :none}
                              :sidebar-opened false
                              :loading? true
                              :web3 (ig/ref ::web3/module)
                              :uport (ig/ref ::uport/module)
                              :ipfs (ig/ref ::ipfs/module)}
                 :dev debug?
                 :routes (ig/ref ::routes/module)}})
