(ns scoopmarket.client.db
  (:require [cljs-web3.core :as web3]
            [cljsjs.ipfs]))

(def default-db
  {:active-panel :none
   :sidebar-opened false
   :loading? true

   :ipfs (js/IpfsApi "/ip4/127.0.0.1/tcp/5001")

   :abi-loaded false
   :web3 (aget js/window "web3")
   :my-address nil
   :contract {:name "Scoop"
              :abi nil
              :instance nil
              :address nil}})
