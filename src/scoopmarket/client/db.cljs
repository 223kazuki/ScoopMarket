(ns scoopmarket.client.db
  (:require [cljs-web3.core :as web3]))

(def default-db
  {:active-panel :none
   :sidebar-opened false
   :loading? true
   :web3 (aget js/window "web3")
   :provides-web3? (aget js/window "web3")
   :contract {:name "ScoopMarket"
              :abi nil
              :instance nil
              :address nil}})
