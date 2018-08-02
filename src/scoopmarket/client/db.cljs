(ns scoopmarket.client.db
  (:require [cljs-web3.core :as web3]
            [cljsjs.ipfs]))

(def default-db
  {:active-panel :none
   :sidebar-opened false
   :loading? true
   :ipfs (js/IpfsApi (clj->js {:host "ipfs.infura.io"
                               :port 5001
                               :protocol "https"}))
   :abi-loaded false
   :web3 (aget js/window "web3")
   :my-address (when-let [web3 (aget js/window "web3")]
                 (aget web3 "eth" "defaultAccount"))
   :contract {:name "Scoop"
              :abi nil
              :instance nil
              :address nil}})
