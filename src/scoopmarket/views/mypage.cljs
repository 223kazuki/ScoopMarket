(ns scoopmarket.views.mypage
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scoopmarket.module.subs :as subs]
            [scoopmarket.module.events :as events]
            [scoopmarket.module.ipfs :as ipfs]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.buffer]
            [cljsjs.moment]))

(defn scoop-editor [{:keys [:configs :handlers]}]
  (let [{:keys [:scoop :web3 :ipfs]} configs
        {:keys [:edit-handler]} handlers
        editing? (reagent/atom false)
        uploading? (reagent/atom false)
        form (reagent/atom (select-keys scoop [:name :for-sale? :price :meta]))
        Buffer js/buffer.Buffer]
    (letfn [(input-text-handler [el]
              (let [n (aget (.-target el) "name")
                    v (aget (.-target el) "value")]
                (swap! form assoc-in [(keyword n)] v)))
            (input-price-handler [el]
              (let [n (aget (.-target el) "name")
                    v (aget (.-target el) "value")]
                (when (<= 0 (js/parseInt v) 100000000000000000000)
                  (swap! form assoc-in [(keyword n)] v))))
            (check-handler [_ el]
              (let [n (aget el "name")
                    v (aget el "checked")]
                (swap! form assoc-in [(keyword n)] v)))
            (add-tag [tag]
              (swap! form dissoc :new-tag)
              (swap! form update-in [:meta :tags] #(set (conj % tag))))
            (delete-tag [tag] (swap! form update-in [:meta :tags] #(set (remove #{tag} %))))
            (upload-meta []
              (reset! uploading? true)
              (let [meta (select-keys (:meta @form) [:tags])
                    meta-str (.stringify js/JSON (clj->js meta))
                    buffer (js-invoke Buffer "from" meta-str)]
                (ipfs/upload-data ipfs buffer
                                  #(do
                                     (reset! uploading? false)
                                     (edit-handler
                                      (-> @form
                                          (select-keys [:name :for-sale? :price])
                                          (assoc :meta-hash %)))))))]
      (fn []
        (let [{:keys [:new-tag :name :for-sale? :price :meta]} @form
              tags (:tags meta)]
          [:<>
           [sa/Button {:on-click #(swap! editing? not)} "Edit"]
           [sa/Transition {:visible @editing?
                           :animation "fade up" :duration 500 :unmount-on-hide true}
            [sa/Modal {:open true :size "small" :close-icon true
                       :on-close #(swap! editing? not)}
             [sa/ModalContent
              [sa/Segment
               [sa/Form
                [sa/FormField
                 [:label "Scoop Name"]
                 [:input {:placeholder "My Scoop"
                          :name "name"
                          :maxLength 50
                          :value (:name @form "")
                          :on-change input-text-handler}]]
                [sa/FormField
                 [sa/Checkbox {:name "for-sale?"
                               :on-change check-handler
                               :checked (:for-sale? @form false)
                               :label "For sale?"}]]
                [sa/FormField
                 [:label "Scoop Price"]
                 [:input {:placeholder "1000000000000000 wei"
                          :name "price"
                          :type "number"
                          :disabled (not (:for-sale? @form false))
                          :value (:price @form "")
                          :on-change input-price-handler}]]
                [sa/FormField
                 [:label "Tags"]
                 (for [tag tags]
                   ^{:key tag}
                   [sa/Label {:style {:margin-top "3px"}}
                    tag [sa/Icon {:name "delete"
                                  :on-click #(delete-tag tag)}]])
                 [sa/Input {:icon (clj->js
                                   {:name "tags" :circular true :link true
                                    :onClick
                                    #(when-not (empty? new-tag)
                                       (add-tag new-tag))})
                            :style {:width "100%" :margin-top "5px"}
                            :placeholder "Enter tags"
                            :name "new-tag"
                            :value (or new-tag "")
                            :on-change input-text-handler}]]]
               [sa/Divider {:hidden true}]
               [sa/Button {:disabled (or (nil? (:name @form))
                                         (empty? (:name @form))
                                         @uploading?)
                           :on-click upload-meta}
                (if @uploading?
                  [:<> [sa/Icon {:loading true :name "spinner"}] "Uploading..."]
                  "Update")]]]]]])))))

(defn scoop-card [{:keys [:configs :handlers]}]
  (let [{:keys [:scoop :ipfs :web3]} configs
        {:keys [:id :name :timestamp :image-hash :price :for-sale? :author :meta]} scoop
        type (reagent/atom nil)
        modal-open? (reagent/atom false)]
    (fn []
      (when image-hash
        (let [image-uri (ipfs/get-url ipfs image-hash)]
          (when-not @type
            (ajax.core/GET image-uri
                {:handler #(let [content-type (get-in % [:headers "content-type"])]
                             (if (clojure.string/starts-with? content-type "image/")
                               (reset! type :img)
                               (reset! type :video)))
                 :response-format (ajax.ring/ring-response-format)}))
          [:<>
           [sa/Transition {:visible @modal-open?
                           :animation "fade up" :duration 500 :unmount-on-hide true}
            [sa/Modal {:close-icon true :size "large"
                       :open @modal-open? :on-close #(swap! modal-open? not)}
             [sa/ModalContent
              (when-let [type @type]
                (case type
                  :img [:img {:src image-uri :style {:width "100%"}}]
                  :video [:video {:src image-uri :style {:width "100%"}}]
                  [:span "This file can't display"]))]]]
           [sa/Card {:style {:width "100%"}}
            [:div {:on-click #(swap! modal-open? not)
                   :style {:display "table-cell" :width "100%" :height "210px"
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
              [:span (if-not for-sale?
                       "Not for sale"
                       (str "For sale : "  price " wei"))]
              (when-not (empty? (:tags meta))
                [:<>
                 [:br]
                 "Tags: "(for [tag (:tags meta)]
                           ^{:key tag} [sa/Label {:style {:margin-top "3px"}} tag])])]]
            [sa/CardContent
             [scoop-editor {:configs {:scoop scoop :web3 web3 :ipfs ipfs}
                            :handlers {:edit-handler
                                       #(re-frame/dispatch [::events/edit-scoop
                                                            web3 id %])}}]]]])))))

(defn image-uploader [{:keys [:configs :handlers]}]
  (let [id (random-uuid) {:keys [:upload-handler]} handlers]
    [:<>
     [sa/Label {:htmlFor id :as "label" :class "button" :size "large"}
      [sa/Icon {:name "photo"}] "New Scoop"]
     [:input {:id id :type "file" :style {:display "none"}
              :accept "image/*,video/mp4,video/x-m4v,video/*" :capture "camera"
              :on-change (fn []
                           (let [el (.getElementById js/document id)
                                 file (aget (.-files el) 0)
                                 reader (js/FileReader.)]
                             (set! (.-onloadend reader)
                                   #(upload-handler reader))
                             (.readAsArrayBuffer reader file)))}]]))

(defn scoop-uploader [{:keys [:configs :handlers]}]
  (let [{:keys [:ipfs]} configs
        {:keys [:scoop-upload-handler]} handlers
        form (reagent/atom {})
        uploading? (reagent/atom false)
        Buffer js/buffer.Buffer
        input-text-handler
        (fn [el]
          (let [n (aget (.-target el) "name")
                v (aget (.-target el) "value")]
            (swap! form assoc-in [(keyword n)] v)))
        input-price-handler
        (fn [el]
          (let [n (aget (.-target el) "name")
                v (aget (.-target el) "value")]
            (when (<= 0 (js/parseInt v) 100000000000000000000)
              (swap! form assoc-in [(keyword n)] v))))
        check-handler
        (fn [_ el]
          (let [n (aget el "name")
                v (aget el "checked")]
            (swap! form assoc-in [(keyword n)] v)))
        upload-image-handler
        (fn [reader]
          (reset! uploading? true)
          (ipfs/upload-data ipfs (js-invoke Buffer "from" (aget reader "result"))
                            (fn [hash]
                              (reset! uploading? false)
                              (swap! form assoc :image-hash hash))))]
    (fn []
      [:<>
       (if @uploading?
         [sa/Label {:as "label" :class "button" :size "large"}
          [sa/Icon {:loading true :name "spinner"}] "Uploading..."]
         [image-uploader {:handlers {:upload-handler upload-image-handler}}])
       [sa/Transition {:visible (not (nil? (:image-hash @form)))
                       :animation "fade up" :duration 500 :unmount-on-hide true}
        [sa/Modal {:open true :size "small"}
         [sa/ModalContent
          [sa/Segment
           [sa/Form
            [sa/FormField
             [:label "Scoop Name"]
             [:input {:placeholder "My Scoop"
                      :name "name"
                      :maxLength 50
                      :value (:name @form "")
                      :on-change input-text-handler}]]
            [sa/FormField
             [sa/Checkbox {:name "for-sale?"
                           :on-change check-handler
                           :checked (:for-sale? @form false)
                           :label "For sale?"}]]
            [sa/FormField
             [:label "Scoop Price"]
             [:input {:placeholder "1000000000000000 wei"
                      :name "price"
                      :type "number"
                      :disabled (not (:for-sale? @form false))
                      :value (:price @form "")
                      :on-change input-price-handler}]]]
           [sa/Divider {:hidden true}]
           [sa/Button {:disabled (or (nil? (:name @form))
                                     (empty? (:name @form)))
                       :on-click #(scoop-upload-handler @form)} "Mint"]]]]]])))

(defn mypage-panel [mobile? _]
  (let [scoops (re-frame/subscribe [::subs/scoops])
        credential (re-frame/subscribe [::subs/credential])
        web3 (re-frame/subscribe [::subs/web3])
        ipfs (re-frame/subscribe [::subs/ipfs])
        uport (re-frame/subscribe [::subs/uport])
        credit (re-frame/subscribe [::subs/credit])
        mint-cost (re-frame/subscribe [::subs/mint-cost])]
    (reagent/create-class
     {:component-will-mount
      #(do (re-frame/dispatch [::events/fetch-credit @web3])
           (re-frame/dispatch [::events/fetch-mint-cost @web3])
           (re-frame/dispatch [::events/fetch-scoops @web3]))

      :reagent-render
      (fn []
        (let [web3 @web3 ipfs @ipfs uport @uport
              {:keys [avatar name]} @credential]
          (when (and @mint-cost @credit @scoops)
            [:div
             [sa/Header {:as "h1"} "My Page"]
             (when @credential
               [sa/Segment {:text-align "center"}
                [sa/Header {:as "h2"} name]
                [:img {:src (:uri avatar)}]
                [sa/Divider {:hidden true}]])
             [sa/Header {:as "h2"} (str "Your credit is " @credit " wei")]
             [sa/Label {:as "label" :class "button" :size "large"
                        :on-click #(do (re-frame/dispatch [::events/fetch-credit web3])
                                       (re-frame/dispatch [::events/fetch-scoops web3]))}
              [sa/Icon {:name "undo" :style {:margin 0}}]]
             [scoop-uploader {:configs {:ipfs ipfs}
                              :handlers {:scoop-upload-handler
                                         (fn [scoop]
                                           (re-frame/dispatch [::events/mint web3 @mint-cost scoop]))}}]
             (when-not @credential
               [sa/Label {:as "label" :class "button" :size "large"
                          :on-click #(re-frame/dispatch [::events/connect-uport uport web3])}
                [sa/Icon {:name "id card"}] "Connect uPort"])
             (when-not (zero? @credit)
               [sa/Label {:as "label" :class "button" :size "large"
                          :on-click #(re-frame/dispatch [::events/withdraw web3])}
                [sa/Icon {:name "money"}] "Withdraw credit"])
             [sa/Divider]
             (let [scoops (sort-by key @scoops)]
               (if-not (empty? scoops)
                 [sa/Grid {:columns (if mobile? 1 3)}
                  (for [[_ scoop] scoops]
                    ^{:key scoop}
                    [sa/GridColumn
                     [scoop-card {:configs {:scoop scoop :web3 web3 :ipfs ipfs}}]])]
                 [:span "No scoops."]))])))})))
