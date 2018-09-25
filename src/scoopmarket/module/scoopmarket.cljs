(ns scoopmarket.module.scoopmarket
  (:require [integrant.core :as ig]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljs-web3.eth :as web3-eth]
            [scoopmarket.module.web3 :as web3]
            [scoopmarket.module.ipfs :as ipfs]
            [clojure.core.async :refer [chan go go-loop >! <! timeout close!]]))

(defn- get-contract-instance [web3-instance network-id contract]
  (let [{:keys [:abi :networks]} contract
        network-id-key (keyword (str network-id))
        address (-> networks network-id-key :address)]
    (web3-eth/contract-at web3-instance abi address)))

(defn- create-loop [f tick]
  (let [timing? (chan)
        kick #(do (f)
                  (go
                    (<! (timeout tick))
                    (>! timing? true)))]
    (go-loop []
      (when (<! timing?)
        (kick)
        (recur)))
    (kick)
    #(close! timing?)))

;; Initial DB
(def initial-db
  {})

;; Subscriptions
(defmulti reg-sub identity)
;; (defmethod reg-sub ::tickets [k]
;;   (re-frame/reg-sub-raw
;;    k (fn [app-db [_ address event-id]]
;;        (let [close (create-loop #(re-frame/dispatch [::fetch-tickets address event-id])
;;                                 100000)]
;;          (reagent.ratom/make-reaction
;;           (fn []
;;             (get-in @app-db [::events (str event-id) :tickets]))
;;           :on-dispose close)))))
;; (defmethod reg-sub ::ticket [k]
;;   (re-frame/reg-sub-raw
;;    k (fn [app-db [_ event-id ticket-id]]
;;        (let [close (create-loop #(re-frame/dispatch [::fetch-ticket event-id ticket-id])
;;                                 10000)]
;;          (reagent.ratom/make-reaction
;;           #(let [{:keys [:change-enterable-timestamp :status] :as ticket}
;;                  (get-in @app-db [::events (str event-id) :tickets (str ticket-id)])]
;;              (if (and change-enterable-timestamp
;;                       (> (- (js-invoke (js/moment) "unix")
;;                             (js-invoke change-enterable-timestamp "unix"))
;;                          (* 10 60))
;;                       (= status :enterable))
;;                (assoc ticket :status :expire)
;;                ticket))
;;           :on-dispose close)))))

;; Events
(defmulti reg-event identity)
(defmethod reg-event ::init [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [{:keys [::web3/web3-instance ::web3/network-id] :as db} [contract]]
    (let [contract-instance (when web3-instance
                              (get-contract-instance web3-instance network-id contract))]
      (-> db
          (merge initial-db)
          (assoc
           ::contract-instance contract-instance
           ::contract contract))))))
(defmethod reg-event ::halt [k]
  (re-frame/reg-event-fx
   k [re-frame/trim-v]
   (fn-traced [{:keys [db]} _]
              {:db  (->> db
                         (filter #(not= (namespace (key %)) (namespace ::x)))
                         (into {}))
               :web3/stop-watching-all {}})))
(defmethod reg-event ::reconnect [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced [db _]
              (let [{:keys [::web3/web3-instance ::web3/network-id ::contract]} db]
                (assoc db ::contract-instance
                       (get-contract-instance web3-instance network-id contract))))))
;; (defmethod reg-event ::fetch-tickets [k]
;;   (re-frame/reg-event-fx
;;    k [re-frame/trim-v]
;;    (fn-traced
;;     [{:keys [db]} [address event-id]]
;;     {::web3/call {:web3 (::web3/web3-instance db)
;;                   :fns [{:instance (::contract-instance db)
;;                          :fn :tickets-of
;;                          :args [address event-id]
;;                          :on-success [::fetch-tickets-success event-id]
;;                          :on-error [::api-failure]}]}})))
;; (defmethod reg-event ::fetch-tickets-success [k]
;;   (re-frame/reg-event-db
;;    k [re-frame/trim-v]
;;    (fn-traced [db [event-id result]]
;;               (let [ids (map #(js-invoke % "toNumber") result)]
;;                 (as-> db $
;;                   (update-in $ [::events (str event-id) :tickets]
;;                              select-keys (map str ids))
;;                   (reduce (fn [db id] (assoc-in db [::events (str event-id) :tickets
;;                                                     (str id) :id] id))
;;                           $ ids))))))
;; (defmethod reg-event ::fetch-ticket [k]
;;   (re-frame/reg-event-fx
;;    k [re-frame/trim-v]
;;    (fn-traced
;;     [{:keys [db]} [event-id ticket-id]]
;;     {::web3/call {:web3 (::web3/web3-instance db)
;;                   :fns [{:instance (::contract-instance db)
;;                          :fn :ticket-detail
;;                          :args [event-id ticket-id]
;;                          :on-success [::fetch-ticket-success event-id ticket-id]
;;                          :on-error [::api-failure]}]}})))
;; (defmethod reg-event ::fetch-ticket-success [k]
;;   (re-frame/reg-event-db
;;    k [re-frame/trim-v]
;;    (fn-traced [db [event-id ticket-id
;;                    [merchant-id status change-enterable-timestamp enter-timestamp]]]
;;               (let [merchant-id (js-invoke merchant-id "toNumber")
;;                     status (get TICKET_STATUS (js-invoke status "toNumber"))
;;                     change-enterable-timestamp
;;                     (js-invoke js/moment "unix"
;;                                (js-invoke change-enterable-timestamp "toNumber"))
;;                     enter-timestamp
;;                     (js-invoke js/moment "unix" (js-invoke enter-timestamp "toNumber"))]
;;                 (-> db
;;                     (update-in [::events (str event-id) :tickets (str ticket-id)] assoc
;;                                :merchant-id merchant-id
;;                                :status status
;;                                :change-enterable-timestamp change-enterable-timestamp
;;                                :enter-timestamp enter-timestamp))))))
(defmethod reg-event ::api-failure [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced [db [result]]
              (assoc db ::loading? false))))

;; Init
(defmethod ig/init-key :scoopmarket.module.scoopmarket
  [k {:keys [contract web3]}]
  (js/console.log (str "Initializing " k))
  (let [subs (->> reg-sub methods (map key))
        events (->> reg-event methods (map key))]
    (->> subs (map reg-sub) doall)
    (->> events (map reg-event) doall)
    (re-frame/dispatch-sync [::init contract])
    {:subs subs :events events}))

;; Halt
(defmethod ig/halt-key! :scoopmarket.module.scoopmarket
  [k {:keys [:subs :events]}]
  (js/console.log (str "Halting " k))
  (re-frame/dispatch-sync [::halt])
  (->> subs (map re-frame/clear-sub) doall)
  (->> events (map re-frame/clear-event) doall))
