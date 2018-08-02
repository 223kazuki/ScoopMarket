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

(defn scoop-uploader [{:keys [:config :upload-handler]}]
  (let [uuid (random-uuid)]
    [:span
     [sa/Label {:htmlFor uuid :as "label" :class "button"}
      [sa/Icon {:name "upload"}] "Upload"]
     [:input {:id uuid :type "file" :style {:display "none"}
              :on-change (fn []
                           (let [el (.getElementById js/document uuid)
                                 file (aget (.-files el) 0)
                                 reader (js/FileReader.)]
                             (set! (.-onloadend reader)
                                   #(upload-handler reader))
                             (.readAsArrayBuffer reader file)))}]]))

(defn- input-text-handler [el]
  (let [n (aget (.-target el) "name")
        v (aget (.-target el) "value")]
    (re-frame/dispatch [::events/update-form n (fn [_] v)])))

(defn tags [{:keys [:config :on-click-handler]}]
  (let [{:keys [:id :meta] :as scoop} (:scoop config)]
    (reagent/create-class
     {:reagent-render
      (fn []
        (let [{:keys [:tags]} meta
              form (re-frame/subscribe [::subs/form])
              new-tag ((keyword (str "new-tag/" id)) @form)]
          [sa/Segment
           [sa/Input {:icon "tags"
                      :iconPosition "left"
                      :label (clj->js {:tag true :content "Add Tag"
                                       :style {:cursor "pointer"}
                                       :onClick #(when-not (empty? new-tag)
                                                   (re-frame/dispatch [::events/add-scoop-tag id new-tag]))})
                      :labelPosition "right"
                      :placeholder "Enter tags"
                      :name (str "new-tag/" id)
                      :value (or new-tag "")
                      :on-change input-text-handler}]
           (when-not (empty? tags) [:br])
           (for [tag tags]
             ^{:key tag}
             [sa/Label {:style {:margin-top "3px"}}
              tag [sa/Icon {:name "delete"
                            :on-click #(re-frame/dispatch [::events/delete-scoop-tag id tag])}]])
           [sa/Divider]
           [sa/Button {:on-click #(re-frame/dispatch [::events/upload-meta id])} "Update"]]))})))

(defn scoop-panel [{:keys [:config :on-click-handler]}]
  (let [{:keys [:id :image-url] :as scoop} (:scoop config)]
    (reagent/create-class
     {:component-did-mount
      #(when (nil? image-url)
         (re-frame/dispatch [::events/fetch-scoop id]))

      :reagent-render
      (fn []
        [sa/Card {:style {:width "100%"}}
         [sa/Modal {:close-icon true
                    :size "large"
                    :trigger (js/React.createElement
                              "img" (clj->js {:style {:width "100%"} :src image-url}))}
          [sa/ModalContent
           [:img {:style {:width "100%"} :src image-url}]]]
         [sa/CardContent
          [tags {:config {:scoop scoop}}]]])})))

(defn mypage-panel []
  (reagent/create-class
   {:component-did-mount
    #()

    :reagent-render
    (fn []
      (let [scoops (re-frame/subscribe [::subs/scoops])]
        [:div
         "This is my page."
         [sa/Divider {:hidden true}]
         [sa/Button {:on-click #(re-frame/dispatch [::events/connect-uport])} "Connect to uPort"]
         [sa/Divider]
         [scoop-uploader {:upload-handler
                          (fn [reader]
                            (re-frame/dispatch [::events/upload-image reader]))}]
         [sa/Divider]
         [sa/Grid {:doubling true :columns 3}
          (for [[_ scoop] @scoops]
            ^{:key scoop}
            [sa/GridColumn
             [scoop-panel {:config {:scoop scoop}}]])]]))}))

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
  (let [abi-loaded (re-frame/subscribe [::subs/abi-loaded])
        address (re-frame/subscribe [::subs/my-address])
        active-panel (re-frame/subscribe [::subs/active-panel])]
    [sa/Container {:className "mainContainer" :style {:marginTop "7em"}}
     (when (and @abi-loaded @address)
       [transition-group
        [css-transition {:key @active-panel
                         :classNames "pageChange"
                         :timeout 500
                         :className "transition"}
         [(panels @active-panel) mobile?]]])]))

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
