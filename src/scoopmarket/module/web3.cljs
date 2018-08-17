(ns scoopmarket.module.web3
  (:require [integrant.core :as ig]
            [cljs-web3.eth :as web3-eth]
            [clojure.core.async :refer [go <! timeout]]))

(defn wait-for-mined [{:keys [web3-instance]} tx-hash pending-cb success-cb]
  (letfn [(polling-loop []
            (go
              (<! (timeout 1000))
              (web3-eth/get-transaction
               web3-instance tx-hash
               (fn [err res]
                 (when-not err
                   (wait-for-mined (js->clj res)))))))
          (wait-for-mined [res]
            (if (:block-number res)
              (success-cb res)
              (do
                (pending-cb res)
                (polling-loop))))]
    (wait-for-mined {:block-number nil})))

(defmethod ig/init-key :scoopmarket.module.web3 [_ {:keys [contract-json network-id dev]}]
  (when-let [web3-instance (aget js/window "web3")]
    (let [{:keys [abi networks] :as contract} contract-json
          network-id-key (keyword (str network-id))
          address (-> networks network-id-key :address)]
      {:web3-instance web3-instance
       :contract-instance (web3-eth/contract-at web3-instance abi address)
       :network-id network-id
       :dev dev
       :contract contract
       :contract-address address
       :my-address (aget web3-instance "eth" "defaultAccount")
       :is-rinkeby? (or (some-> web3-instance
                                (aget "currentProvider")
                                (aget "publicConfigStore")
                                (aget "_state")
                                (aget "networkVersion")
                                (= (str network-id)))
                        dev)})))

(defmethod ig/halt-key! :scoopmarket.module.web3 [_ _])
