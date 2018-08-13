(ns scoopmarket.views.verify
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scoopmarket.module.subs :as subs]
            [scoopmarket.module.events :as events]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.moment]))

(defn verify-panel [route-params]
  (let [{:keys [:id]} route-params
        web3 @(re-frame/subscribe [::subs/web3])
        scoops (re-frame/subscribe [::subs/scoops])]
    (reagent/create-class
     {:component-did-mount
      #(let [scoop (get-in @scoops [(keyword (str id))])]
         (when-not (:image-hash scoop)
           (re-frame/dispatch [::events/fetch-scoop web3 id])))

      :reagent-render
      (fn []
        (let [scoop (get-in @scoops [(keyword (str id))])
              {:keys [:id :name :timestamp :image-hash :price :for-sale? :author :owner]} scoop
              image-uri (str "https://ipfs.infura.io/ipfs/" image-hash)]
          (when image-hash
            [:div
             [sa/Segment {:textAlign "center"}
              [:img {:style {:width "100%" :max-width "500px"} :src image-uri}]
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
