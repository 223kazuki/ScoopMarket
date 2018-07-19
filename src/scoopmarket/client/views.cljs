(ns scoopmarket.client.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scoopmarket.client.subs :as subs]
            [scoopmarket.client.events :as events]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.react-transition-group]
            [cljsjs.moment]))

(defn- dimmer []
  (let [loading? (re-frame/subscribe [::subs/loading?])]
    [sa/Transition {:visible @loading? :animation "fade" :duration 500
                    :unmountOnHide true}
     [sa/Dimmer {:active true :page true}
      [sa/Loader "Loading..."]]]))

(defn mypage-panel []
  (reagent/create-class
   {:component-did-mount
    #(re-frame/dispatch [::events/fetch-image])

    :reagent-render
    (fn []
      (let [image-url (re-frame/subscribe [::subs/image-url])]
        [:div
         [:div "This is my page."]
         [:img {:src @image-url}]]))}))

(defn market-panel [] [:div "This is market."])
(defn none-panel   [] [:div])

(defmulti  panels identity)
(defmethod panels :mypage-panel [] #'mypage-panel)
(defmethod panels :market-panel [] #'market-panel)
(defmethod panels :none [] #'none-panel)
(defmethod panels :default [] [:div "This page does not exist."])

(def transition-group
  (reagent/adapt-react-class js/ReactTransitionGroup.TransitionGroup))
(def css-transition
  (reagent/adapt-react-class js/ReactTransitionGroup.CSSTransition))

(defn main-container [mobile?]
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [sa/Container {:className "mainContainer" :style {:marginTop "7em"}}
     [transition-group
      [css-transition {:key @active-panel
                       :classNames "pageChange"
                       :timeout 500
                       :className "transition"}
       [(panels @active-panel) mobile?]]]]))

(defn main-panel []
  (let [sidebar-opened (re-frame/subscribe [::subs/sidebar-opened])]
    [:div
     [dimmer]
     [sa/Responsive {:min-width 768}
      [sa/Menu {:fixed "top" :inverted true :style {:background-color "black"}
                :size "huge"}
       [sa/Container
        [sa/MenuItem {:as "a" :header true  :href "/"}
         "ScoopMarket"]
        [sa/MenuItem {:as "a" :href "/"} "My Page"]
        [sa/MenuItem {:as "a" :href "/market"} "Market"]]]
      [main-container]]
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
          [main-container true]]]]]]]))
