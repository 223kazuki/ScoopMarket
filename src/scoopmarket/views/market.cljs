(ns scoopmarket.views.market
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scoopmarket.module.subs :as subs]
            [scoopmarket.module.events :as events]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.moment]))

(defn scoop-for-sale-card [{:keys [:configs :handlers]}]
  (let [{:keys [:scoop :ipfs :web3]} configs
        {:keys [:request-to-buy-handler :approve-to-buy-handler :buy-handler]} handlers
        {:keys [:id :name :timestamp :image-hash :price :author :meta]} scoop]
    (reagent/create-class
     {:component-did-mount
      #(when (nil? image-hash)
         (re-frame/dispatch [::events/fetch-scoop-for-sale web3 id]))

      :reagent-render
      (fn []
        (when image-hash
          (let [image-uri (str "https://ipfs.infura.io/ipfs/" image-hash)]
            [sa/Card {:style {:width "100%"}}
             [:div {:style {:display "table-cell" :width "100%" :height "210px"
                            :padding-top "5px" :text-align "center" :vertical-align "middle"}}
              [:img {:src image-uri :style {:height "100%" :max-height "200px"
                                            :max-width "100%"}}]]
             [sa/CardContent
              [sa/CardHeader [:a {:href (str "/verify/" id)} name]]
              [sa/CardMeta
               [:span.date (str "Uploaded : " (.format (.unix js/moment timestamp)
                                                       "YYYY/MM/DD HH:mm:ss"))] [:br]
               [:span (str price " wei")]]]
             [sa/CardContent
              [sa/Button {:on-click request-to-buy-handler} "Request to buy"]
              [sa/Button {:on-click request-to-buy-handler} "Approve to buy"]
              [sa/Button {:on-click request-to-buy-handler} "Buy"]]])))})))

(defn market-panel []
  (let [scoops-for-sale (re-frame/subscribe [::subs/scoops-for-sale])
        credential (re-frame/subscribe [::subs/credential])
        web3 (re-frame/subscribe [::subs/web3])
        ipfs (re-frame/subscribe [::subs/ipfs])
        uport (re-frame/subscribe [::subs/uport])]
    (reagent/create-class
     {:component-did-mount
      #(re-frame/dispatch [::events/fetch-scoops-for-sale @web3])

      :reagent-render
      (let []
        (fn []
          (let [web3 @web3 ipfs @ipfs uport @uport]
            [:div
             [sa/Header {:as "h1"} "Market"]
             [sa/Grid {:doubling true :columns 3}
              (for [[_ scoop] (sort-by key @scoops-for-sale)]
                ^{:key scoop}
                [sa/GridColumn
                 [scoop-for-sale-card {:configs {:scoop scoop :web3 web3 :ipfs ipfs}
                                       :handlers {}}]])]
             ])))})))
