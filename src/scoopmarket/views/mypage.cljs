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

(defn meta-editor [{:keys [:configs :handlers]}]
  (let [{:keys [scoop ipfs]} configs
        {:keys [:id :meta]} scoop
        {:keys [:meta-update-handler]} handlers
        form (reagent/atom meta)
        editing (reagent/atom false)
        Buffer js/buffer.Buffer
        input-text-handler (fn [el]
                             (let [n (aget (.-target el) "name")
                                   v (aget (.-target el) "value")]
                               (swap! form assoc-in [(keyword n)] v)))
        add-tag (fn [tag]
                  (swap! form dissoc :new-tag)
                  (swap! form update :tags #(set (conj % tag))))
        delete-tag (fn [tag] (swap! form update :tags #(set (remove #{tag} %))))
        upload-meta (fn []
                      (let [meta (select-keys @form [:tags])
                            meta-str (.stringify js/JSON (clj->js meta))
                            buffer (js-invoke Buffer "from" meta-str)]
                        (ipfs/upload-data ipfs buffer meta-update-handler)))]
    (fn []
      (let [{:keys [:new-tag :tags]} @form]
        [:<>
         (for [tag tags]
           ^{:key tag}
           [sa/Label {:style {:margin-top "3px"}}
            tag [sa/Icon {:name "delete"
                          :on-click #(delete-tag tag)}]])
         [sa/Icon {:name "edit" :circular true :style {:margin-left "5px"}
                   :on-click #(swap! editing not)}]
         (when @editing
           [:<>
            [sa/Input {:icon (clj->js
                              {:name "tags" :circular true :link true
                               :onClick
                               #(when-not (empty? new-tag)
                                  (add-tag new-tag))})
                       :style {:width "100%" :margin-top "5px"}
                       :placeholder "Enter tags"
                       :name "new-tag"
                       :value (or new-tag "")
                       :on-change input-text-handler}]
            [sa/Button {:on-click upload-meta :style {:margin-top "5px"}} "Update meta data"]])]))))

(defn scoop-card [{:keys [:configs :handlers]}]
  (let [{:keys [:scoop :ipfs :web3]} configs
        {:keys [:id :name :timestamp :image-hash :price :for-sale? :author :meta]} scoop
        modal-open? (reagent/atom false)]
    (reagent/create-class
     {:component-did-mount
      #(when (nil? image-hash)
         (re-frame/dispatch [::events/fetch-scoop web3 id]))

      :reagent-render
      (fn []
        (when image-hash
          (let [image-uri (str "https://ipfs.infura.io/ipfs/" image-hash)]
            [:<>
             [sa/Transition {:visible @modal-open?
                             :animation "fade up" :duration 500 :unmount-on-hide true}
              [sa/Modal {:close-icon true :size "large"
                         :open @modal-open? :on-close #(swap! modal-open? not)}
               [sa/ModalContent
                [:img {:style {:width "100%"} :src image-uri}]]]]
             [sa/Card {:style {:width "100%"}}
              [:div {:on-click #(swap! modal-open? not)
                     :style {:display "table-cell" :width "100%" :height "210px"
                             :padding-top "5px" :text-align "center" :vertical-align "middle"}}
               [:img {:src image-uri :style {:height "100%" :max-height "200px"
                                             :max-width "100%"}}]]
              [sa/CardContent
               [sa/CardHeader [:a {:href (str "/verify/" id)} name]]
               [sa/CardMeta
                [:span.date (str "Uploaded : " (.format (.unix js/moment timestamp)
                                                        "YYYY/MM/DD HH:mm:ss"))] [:br]
                [:span (if-not for-sale?
                         "Not for sale"
                         (str "For sale : "  price " wei"))]]]
              [sa/CardContent
               [meta-editor {:configs {:scoop scoop :ipfs ipfs}
                             :handlers {:meta-update-handler
                                        (fn [meta-hash]
                                          (re-frame/dispatch [::events/update-meta web3 id meta-hash]))}}]]]])))})))

(defn image-uploader [{:keys [:configs :handlers]}]
  (let [id (random-uuid)
        {:keys [:upload-handler]} handlers]
    (fn []
      [:<>
       [sa/Label {:htmlFor id :as "label" :class "button" :size "large"}
        [sa/Icon {:name "photo"}] "New Scoop"]
       [:input {:id id :type "file" :style {:display "none"}
                :accept "image/*" :capture "camera"
                :on-change (fn []
                             (let [el (.getElementById js/document id)
                                   file (aget (.-files el) 0)
                                   reader (js/FileReader.)]
                               (set! (.-onloadend reader)
                                     #(upload-handler reader))
                               (.readAsArrayBuffer reader file)))}]])))

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
                      :on-change input-text-handler}]]]
           [sa/Divider {:hidden true}]
           [sa/Button {:disabled (or (nil? (:name @form))
                                     (empty? (:name @form)))
                       :on-click #(scoop-upload-handler @form)} "Mint"]]]]]])))

(defn mypage-panel [mobile? _]
  (let [scoops (re-frame/subscribe [::subs/scoops])
        credential (re-frame/subscribe [::subs/credential])
        web3 (re-frame/subscribe [::subs/web3])
        ipfs (re-frame/subscribe [::subs/ipfs])
        uport (re-frame/subscribe [::subs/uport])]
    (reagent/create-class
     {:component-did-mount
      #(re-frame/dispatch [::events/fetch-scoops @web3])

      :reagent-render
      (fn []
        (let [web3 @web3 ipfs @ipfs uport @uport
              {:keys [avatar name]} @credential]
          [:div
           [sa/Header {:as "h1"} (if name name "My Page")]
           (when-let [image-uri (:uri avatar)]
             [:img {:src image-uri}])
           [sa/Label {:as "label" :class "button" :size "large"
                      :on-click #(re-frame/dispatch [::events/fetch-scoops web3])}
            [sa/Icon {:name "undo" :style {:margin 0}}]]
           [scoop-uploader {:configs {:ipfs ipfs}
                            :handlers {:scoop-upload-handler
                                       (fn [scoop]
                                         (re-frame/dispatch [::events/mint web3 scoop]))}}]
           (when-not @credential
             [sa/Label {:as "label" :class "button" :size "large"
                        :on-click #(re-frame/dispatch [::events/connect-uport uport web3])}
              [sa/Icon {:name "id card"}] "Connect uPort"])
           [sa/Divider]
           [sa/Grid {:columns (if mobile? 1 3)}
            (for [[_ scoop] (sort-by key @scoops)]
              ^{:key scoop}
              [sa/GridColumn
               [scoop-card {:configs {:scoop scoop :web3 web3 :ipfs ipfs}}]])]]))})))
