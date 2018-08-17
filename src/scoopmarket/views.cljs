(ns scoopmarket.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scoopmarket.module.subs :as subs]
            [scoopmarket.module.events :as events]
            [scoopmarket.views.market :as market]
            [scoopmarket.views.verify :as verify]
            [scoopmarket.views.mypage :as mypage]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.react-transition-group]
            [cljsjs.moment]))

(defmulti  panels :panel)
(defmethod panels :mypage-panel [] #'mypage/mypage-panel)
(defmethod panels :verify-panel [] #'verify/verify-panel)
(defmethod panels :market-panel [] #'market/market-panel)
(defmethod panels :none [] [:div])
(defmethod panels :default [] [:div "This page does not exist."])

(def transition-group
  (reagent/adapt-react-class js/ReactTransitionGroup.TransitionGroup))
(def css-transition
  (reagent/adapt-react-class js/ReactTransitionGroup.CSSTransition))

(defn main-container [mobile?]
  (let [loading? (re-frame/subscribe [::subs/loading?])
        active-page (re-frame/subscribe [::subs/active-page])
        web3 (re-frame/subscribe [::subs/web3])
        uport (re-frame/subscribe [::subs/uport])]
    (reagent/create-class
     {:component-will-mount
      #(when (nil? (:web3-instance @web3))
         (re-frame/dispatch [::events/connect-uport @uport @web3]))

      :reagent-render
      (fn []
        [:<>
         [sa/Transition {:visible (not (or (false? @loading?)
                                           (nil? @loading?)))
                         :animation "fade" :duration 500 :unmountOnHide true}
          [sa/Dimmer {:active true :page true}
           [sa/Loader (if-let [message (:message @loading?)]
                        message "Loading...")]]]
         (cond
           (nil? (:web3-instance @web3))
           [:div]

           (not (:is-rinkeby? @web3))
           [sa/Modal {:size "large" :open true}
            [sa/ModalContent
             [:div "You must use Rinkeby test network!"]]]

           :else
           (when (:contract-instance @web3)
             [sa/Container {:className "mainContainer" :style {:marginTop "7em"}}
              [transition-group
               [css-transition {:key (:panel @active-page)
                                :classNames "pageChange"
                                :timeout 500
                                :className "transition"}
                [(panels @active-page) mobile? (:route-params @active-page)]]]]))])})))

(defn responsible-container []
  (let [sidebar-opened (re-frame/subscribe [::subs/sidebar-opened])]
    [:div
     [sa/Responsive {:min-width 768}
      [sa/Menu {:fixed "top" :inverted true :style {:background-color "black"} :size "huge"}
       [sa/Container
        [sa/MenuItem {:as "a" :header true  :href "/"}
         "ScoopMarket"]
        [sa/MenuItem {:as "a" :href "/"} "My Page"]
        [sa/MenuItem {:as "a" :href "/market"} "Market"]]]
      [main-container false]]
     [sa/Responsive {:max-width 767}
      [sa/SidebarPushable
       [sa/Sidebar {:as (aget js/semanticUIReact "Menu") :animation "push"
                    :inverted true :vertical true :visible @sidebar-opened
                    :style {:background-color "black"}}
        [sa/MenuItem {:as "a" :href "/"
                      :on-click #(re-frame/dispatch [::events/toggle-sidebar])} "My Page"]
        [sa/MenuItem {:as "a" :href "/market"
                      :on-click #(re-frame/dispatch [::events/toggle-sidebar])} "Market"]]
       [sa/SidebarPusher {:dimmed @sidebar-opened :style {:min-height "100vh"
                                                          :overflow-y "scroll"}
                          :on-click #(if @sidebar-opened
                                       (re-frame/dispatch [::events/toggle-sidebar]))}
        [sa/Segment {:inverted true :text-align "center" :vertical true
                     :style {:height 70 :background-color "black"}}
         [sa/Container
          [sa/Menu {:inverted true :pointing true :secondary true :size "large"
                    :style {:border "none"}}
           [sa/MenuItem {:on-click #(re-frame/dispatch [::events/toggle-sidebar])}
            [sa/Icon {:name "sidebar"}]]]
          [:br]
          [main-container true]]]]]]]))
