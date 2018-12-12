(ns scoopmarket.views.verify
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scoopmarket.module.subs :as subs]
            [scoopmarket.module.events :as events]
            [scoopmarket.module.ipfs :as ipfs]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.moment]))

(defn verify-panel [_ route-params]
  (let [{:keys [:id]} route-params
        web3 (re-frame/subscribe [::subs/web3])
        ipfs (re-frame/subscribe [::subs/ipfs])
        scoops (re-frame/subscribe [::subs/scoops])
        type (reagent/atom nil)]
    (reagent/create-class
     {:component-did-mount
      #(let [scoop (get-in @scoops [(keyword (str id))])]
         (when-not (:image-hash scoop)
           (re-frame/dispatch [::events/fetch-scoop web3 :scoops id])))

      :reagent-render
      (fn []
        (let [web3 @web3
              ipfs @ipfs
              scoop (get-in @scoops [(keyword (str id))])
              {:keys [:id :name :timestamp :image-hash :price
                      :for-sale? :author :owner]} scoop
              image-uri (ipfs/get-url ipfs image-hash)]
          (when image-hash
            (when-not @type
              (ajax.core/GET image-uri
                  {:handler #(let [content-type (get-in % [:headers "content-type"])]
                               (if (clojure.string/starts-with? content-type "image/")
                                 (reset! type :img)
                                 (reset! type :video)))
                   :response-format (ajax.ring/ring-response-format)}))
            [:div
             [sa/Segment {:textAlign "center"}
              (when-let [type @type]
                (case type
                  :img [:img {:style {:width "100%" :max-width "500px"} :src image-uri}]
                  :video [:video {:src image-uri :style {:width "100%"}}]
                  [:span "This file can't display"]))
              [sa/Header {:as "h1"} name]
              [sa/CardMeta
               [:span (str "Uploaded : " (.format (.unix js/moment timestamp)
                                                  "YYYY/MM/DD HH:mm:ss")
                           " by ")
                [:a {:href (str "https://rinkeby.etherscan.io/address/" author)
                     :target "_blank"} author]]]
              [sa/CardContent
               [:span "This scoop is owned by "
                [:a {:href (str "https://rinkeby.etherscan.io/address/" owner)
                     :target "_blank"} owner]]]]])))})))
