(ns scoopmarket.views.market
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scoopmarket.module.subs :as subs]
            [scoopmarket.module.events :as events]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.moment]
            [cljs-web3.core :refer [to-decimal]]))

(defn scoop-for-sale-card [{:keys [:configs :handlers]}]
  (let [{:keys [:scoop :ipfs :web3]} configs
        {:keys [:request-handler :approve-handler :purchase-handler]} handlers
        {:keys [:id :name :timestamp :image-hash :price :author :owner :meta :requestor :approved]} scoop]
    (reagent/create-class
     {:component-did-mount
      #(when (nil? image-hash)
         (re-frame/dispatch [::events/fetch-scoop-for-sale web3 id])
         (re-frame/dispatch [::events/fetch-scoop-for-sale-approved web3 id]))

      :reagent-render
      (fn []
        (when image-hash
          (let [image-uri (str "https://ipfs.infura.io/ipfs/" image-hash)
                tags (:tags meta)]
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
              (for [tag tags]
                ^{:key tag}
                [sa/Label {:style {:margin-top "3px"}}
                 tag])]
             [sa/CardContent
              (cond
                (and (not= (:my-address web3) owner)
                     (== 0 (to-decimal requestor)))
                [sa/Button {:on-click request-handler} "Request purchase"]

                (and (= (:my-address web3) owner)
                     (not= 0 (to-decimal requestor))
                     (== 0 (to-decimal approved)))
                [sa/Button {:on-click approve-handler} (str "Approve purchase to "
                                                            requestor)]

                (and (not= (:my-address web3) owner)
                     (not= 0 (to-decimal approved))
                     (== (to-decimal (:my-address web3)) (to-decimal approved)));; TODO: check approved
                [sa/Button {:on-click purchase-handler} "Purchase"]

                ;; TODO: Cancel

                :else
                [:div])]])))})))

(defn market-panel [mobile? _]
  (let [scoops-for-sale (re-frame/subscribe [::subs/scoops-for-sale])
        credential (re-frame/subscribe [::subs/credential])
        web3 (re-frame/subscribe [::subs/web3])
        ipfs (re-frame/subscribe [::subs/ipfs])
        uport (re-frame/subscribe [::subs/uport])]
    (reagent/create-class
     {:component-did-mount
      #(re-frame/dispatch [::events/fetch-scoops-for-sale @web3])

      :reagent-render
      (fn []
        (let [web3 @web3 ipfs @ipfs uport @uport
              my-address (:my-address web3)]
          [:div
           [sa/Header {:as "h1"} "Market"]
           [sa/Grid {:columns (if mobile? 1 3)}
            (for [[id scoop] (sort-by key @scoops-for-sale)]
              ^{:key scoop}
              [sa/GridColumn
               [scoop-for-sale-card
                {:configs {:scoop scoop :web3 web3}
                 :handlers {:request-handler
                            #(re-frame/dispatch [::events/request web3 (name id)])
                            :approve-handler
                            #(re-frame/dispatch [::events/approve web3
                                                 (name id) (:requestor scoop)])
                            :purchase-handler
                            #(re-frame/dispatch [::events/purchase web3 (name id) (:price scoop)])}}]])]]))})))
