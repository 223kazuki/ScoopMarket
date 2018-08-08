(ns scoopmarket.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [integrant.core :as ig]
            [scoopmarket.events :as events]
            [scoopmarket.subs :as subs]
            [scoopmarket.views :as views]
            [scoopmarket.config :as config]
            [scoopmarket.web3 :as web3]
            [scoopmarket.ipfs :as ipfs]
            [scoopmarket.routes :as routes]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defonce system (atom nil))

(defmethod ig/init-key ::app
  [_ {:keys [initial-db] :as opts}]
  (re-frame/dispatch-sync [::events/initialize-db initial-db])
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel opts]
                  (.getElementById js/document "app")))

(def system-conf
  {::events/module nil
   ::subs/module nil
   ::web3/module nil
   ::ipfs/module {:host "ipfs.infura.io"
                  :port 5001
                  :protocol "https"}
   ::routes/module {:routes ["/" {""       :mypage
                                  "market" :market}]
                    :subs (ig/ref ::subs/module)
                    :events (ig/ref ::events/module)}
   ::app {:initial-db {:active-panel :none
                       :sidebar-opened false
                       :loading? true
                       :ipfs (ig/ref ::ipfs/module)
                       :abi-loaded false
                       :web3 (ig/ref ::web3/module)
                       :is-rinkeby? false
                       :my-address (when-let [web3 (aget js/window "web3")]
                                     (aget web3 "eth" "defaultAccount"))
                       :contract {:name "Scoop"
                                  :abi nil
                                  :instance nil
                                  :address nil}}
          :routes (ig/ref ::routes/module)}})

(defn start []
  (reset! system (ig/init system-conf)))

(defn stop []
  (ig/halt! @system)
  (reset! system nil))

(defn reset []
  (stop)
  (start))

(defn ^:export init []
  (dev-setup)
  (start))
