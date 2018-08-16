(ns scoopmarket.views.market
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scoopmarket.module.subs :as subs]
            [scoopmarket.module.events :as events]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.moment]
            [cljs-web3.core :refer [to-decimal]]))

(defn scoop-info [{:keys [:configs :handlers]} contents]
  (let [{:keys [:scoop :web3]} configs
        {:keys [:id :name :timestamp :image-hash :price :author :owner
                :meta :requestor :approved]} scoop]
    (when (nil? approved)
      (re-frame/dispatch [::events/fetch-scoop-approval web3 :scoops-for-sale id]))
    (if (nil? image-hash)
      (re-frame/dispatch [::events/fetch-scoop web3 :scoops-for-sale id])
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
           [:span (str price " wei")]
           (when meta
             [:<>
              [:br]
              "Tags: " (for [tag tags]
                         ^{:key tag} [sa/Label {:style {:margin-top "3px"}} tag])])]]
         contents]))))

(defn my-scoop-card [{:keys [:configs :handlers]}]
  (let [{:keys [:scoop :web3]} configs
        {:keys [:approve-handler :deny-handler]} handlers
        {:keys [:requestor :approved]} scoop]
    (fn []
      (let [requested? (not= 0 (to-decimal requestor))
            someone-approved? (not (== 0 (to-decimal approved)))]
        [scoop-info {:configs configs}
         (when (and requested? (not someone-approved?))
           [sa/CardContent {:style {:color "black" :text-align "center"}}
            ;; TODO: Cancel approval.
            [:span (str "Approval request from " requestor)] [:br]
            [sa/Button {:style {:margin-top "10px"}
                        :on-click approve-handler} "Approve"]
            [sa/Button {:style {:margin-top "10px"}
                        :on-click deny-handler} "Deny"]])]))))

(defn others-scoop-card [{:keys [:configs :handlers]}]
  (let [{:keys [:scoop :web3]} configs
        {:keys [:request-handler :purchase-handler :cancel-handler]} handlers
        {:keys [:requestor :approved]} scoop]
    (fn []
      (let [requestor? (= (:my-address web3) requestor)
            requested? (not= 0 (to-decimal requestor))
            approved? (= (:my-address web3) approved)]
        (println requestor)
        [scoop-info {:configs configs}
         [sa/CardContent {:style {:color "black" :text-align "center"}}
          (if requested?
            (if requestor?
              (if approved?
                [:<>
                 [sa/Button {:on-click cancel-handler} "Cancel"]
                 [sa/Button {:on-click purchase-handler} "Purchase"]]
                [:<>
                 [:span "Requesting..."] [:br]
                 [sa/Button {:style {:margin-top "10px"}
                             :on-click cancel-handler} "Cancel"]])
              [:span "Under deal."])
            [sa/Button {:on-click request-handler} "Request purchase"])]]))))

(defn market-panel [mobile? _]
  (let [scoops-for-sale (re-frame/subscribe [::subs/scoops-for-sale])
        credential (re-frame/subscribe [::subs/credential])
        web3 (re-frame/subscribe [::subs/web3])
        ipfs (re-frame/subscribe [::subs/ipfs])
        uport (re-frame/subscribe [::subs/uport])]
    (reagent/create-class
     {:component-will-mount
      #(re-frame/dispatch [::events/fetch-scoops-for-sale @web3])

      :reagent-render
      (fn []
        (let [web3 @web3 ipfs @ipfs uport @uport
              my-address (:my-address web3)]
          [:div
           [sa/Header {:as "h1"} "Market"]
           [sa/Label {:as "label" :class "button" :size "large"
                      :on-click #(re-frame/dispatch [::events/refetch-scoops-for-sale web3])}
            [sa/Icon {:name "undo" :style {:margin 0}}]]
           [sa/Divider]
           [sa/Header {:as "h2"} "Your Scoops"]
           (let [scoops (->> @scoops-for-sale
                             (filter #(or (nil? (:owner (val %)))
                                          (= my-address (:owner (val %)))))
                             (sort-by key))]
             (if (empty? scoops)
               "No scoops."
               [sa/Grid {:columns (if mobile? 1 3)}
                (for [[id scoop] scoops]
                  ^{:key scoop}
                  [sa/GridColumn
                   [my-scoop-card
                    {:configs {:scoop scoop :web3 web3}
                     :handlers {:approve-handler
                                #(re-frame/dispatch [::events/approve web3
                                                     (name id) (:requestor scoop)])
                                :deny-handler
                                #(re-frame/dispatch [::events/deny web3
                                                     (name id) (:requestor scoop)])}}]])]))
           [sa/Divider]
           [sa/Header {:as "h2"} "Scoops on sale"]
           (let [scoops (->> @scoops-for-sale
                             (filter #(or (nil? (:owner (val %)))
                                          (not= my-address (:owner (val %)))))
                             (sort-by key))]
             (if (empty? scoops)
               "No scoops."
               [sa/Grid {:columns (if mobile? 1 3)}
                (for [[id scoop] scoops]
                  ^{:key scoop}
                  [sa/GridColumn
                   [others-scoop-card
                    {:configs {:scoop scoop :web3 web3}
                     :handlers {:request-handler
                                #(re-frame/dispatch [::events/request web3 (name id)])
                                :purchase-handler
                                #(re-frame/dispatch [::events/purchase
                                                     web3 (name id) (:price scoop)])
                                :cancel-handler
                                #(re-frame/dispatch [::events/cancel
                                                     web3 (name id) (:price scoop)])}}]])]))]))})))
