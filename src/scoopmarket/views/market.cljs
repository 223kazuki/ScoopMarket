(ns scoopmarket.views.market
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scoopmarket.module.web3 :as web3]
            [scoopmarket.module.uport :as uport]
            [scoopmarket.module.ipfs :as ipfs]
            [scoopmarket.module.scoopmarket :as scoopmarket]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.moment]
            [cljs-web3.core :refer [to-decimal]]))

(defn scoop-info [{:keys [:configs :handlers]} contents]
  (let [{:keys [:scoop :web3]} configs
        {:keys [:id :name :timestamp :image-hash :price :author :owner
                :meta :requestor :approved]} scoop
        type (reagent/atom nil)]
    (fn []
      (if-not (nil? image-hash)
        (let [image-uri (str "https://ipfs.infura.io/ipfs/" image-hash)
              tags (:tags meta)]
          (when-not @type
            (ajax.core/GET image-uri
                           {:handler #(let [content-type (get-in % [:headers "content-type"])]
                                        (if (clojure.string/starts-with? content-type "image/")
                                          (reset! type :img)
                                          (reset! type :video)))
                            :response-format (ajax.ring/ring-response-format)}))
          [sa/Card {:style {:width "100%"}}
           [:div {:style {:display "table-cell" :width "100%" :height "210px"
                          :padding-top "5px" :text-align "center" :vertical-align "middle"}}
            (when-let [type @type]
              (case type
                :img [:img {:src image-uri :style {:height "100%" :max-height "200px"
                                                   :max-width "100%"}}]
                :video [:video {:src image-uri :style {:height "100%" :max-height "200px"
                                                       :max-width "100%"}}]
                [:span "This file can't display"]))]
           [sa/CardContent
            [sa/CardHeader [:a {:href (str "/verify/" id)} name]]
            [sa/CardMeta
             [:span.date (str "Uploaded : " (.format (.unix js/moment timestamp)
                                                     "YYYY/MM/DD HH:mm:ss"))] [:br]
             [:span (str price " wei")]
             (when-not (empty? (:tags meta))
               [:<>
                [:br]
                "Tags: " (for [tag tags]
                           ^{:key tag} [sa/Label {:style {:margin-top "3px"}} tag])])]]
           contents])))))

(defn my-scoop-card [{:keys [:configs :handlers]}]
  (let [{:keys [:scoop :web3]} configs
        {:keys [:approve-handler :deny-handler]} handlers
        {:keys [:requestor :approved]} scoop]
    (let [requested? (not= 0 (to-decimal requestor))
          someone-approved? (not (== 0 (to-decimal approved)))]
      [scoop-info {:configs configs}
       (when (and requested? (not someone-approved?))
         [sa/CardContent {:style {:color "black" :text-align "center"}}
          [:span (str "Approval request from " requestor)] [:br]
          [sa/Button {:style {:margin-top "10px"}
                      :on-click approve-handler} "Approve"]
          [sa/Button {:style {:margin-top "10px"}
                      :on-click deny-handler} "Deny"]])])))

(defn others-scoop-card [{:keys [:configs :handlers]}]
  (let [{:keys [:scoop :web3]} configs
        {:keys [:request-handler :purchase-handler :cancel-handler]} handlers
        {:keys [:requestor :approved]} scoop]
    (let [requestor? (= (:my-address web3) requestor)
          requested? (not= 0 (to-decimal requestor))
          approved? (= (:my-address web3) approved)]
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
          [sa/Button {:on-click request-handler} "Request purchase"])]])))

(defn market-panel [mobile? _]
  (let [scoops-for-sale (re-frame/subscribe [::scoopmarket/scoops-for-sale])
        credential (re-frame/subscribe [::uport/credential])
        web3 (re-frame/subscribe [::web3/web3])
        ipfs (re-frame/subscribe [::ipfs/ipfs])
        uport (re-frame/subscribe [::uport/uport])]
    (reagent/create-class
     {:component-will-mount
      #(re-frame/dispatch [::scoopmarket/fetch-scoops-for-sale @web3])

      :reagent-render
      (fn []
        (let [web3 @web3 ipfs @ipfs uport @uport
              my-address (:my-address web3)]
          [:div
           [sa/Header {:as "h1"} "Market"]
           [sa/Label {:as "label" :class "button" :size "large"
                      :on-click #(re-frame/dispatch [::scoopmarket/fetch-scoops-for-sale web3])}
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
                                #(re-frame/dispatch [::scoopmarket/approve web3
                                                     (name id) (:requestor scoop)])
                                :deny-handler
                                #(re-frame/dispatch [::scoopmarket/deny web3
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
                                #(re-frame/dispatch [::scoopmarket/request web3 (name id)])
                                :purchase-handler
                                #(re-frame/dispatch [::scoopmarket/purchase
                                                     web3 (name id) (:price scoop)])
                                :cancel-handler
                                #(re-frame/dispatch [::scoopmarket/cancel
                                                     web3 (name id) (:price scoop)])}}]])]))]))})))
