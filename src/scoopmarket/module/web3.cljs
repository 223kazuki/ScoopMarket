(ns scoopmarket.module.web3
  (:require [integrant.core :as ig]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [district0x.re-frame.web3-fx]
            [cljs.spec.alpha :as s]
            [cljs-web3.eth :as web3-eth]))

(defn- dispach-fn [on-success on-error & args]
  (fn [err res]
    (if err
      (re-frame/dispatch (vec (concat on-error (cons err args))))
      (re-frame/dispatch (vec (concat on-success (cons res args)))))))

;; Initial DB
(def initial-db {})

;; Subscriptions
(defmulti reg-sub identity)
(defmethod reg-sub ::web3-instance [k]
  (re-frame/reg-sub k #(::web3-instance %)))
(defmethod reg-sub ::my-address [k]
  (re-frame/reg-sub k #(::my-address %)))

;; Events
(defmulti reg-event identity)
(defmethod reg-event ::init [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db [web3-instance network-id]]
    (-> db
        (merge initial-db)
        (assoc
         ::network-id network-id
         ::web3-instance web3-instance
         ::my-address (when web3-instance
                        (aget web3-instance "eth" "defaultAccount")))))))
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
   (fn-traced [{:keys [::network-id] :as db} [web3-instance]]
              (re-frame/dispatch [:scoopmarket.module.scoopmarket/reconnect])
              (assoc db
                     ::web3-instance web3-instance
                     ::my-address (aget web3-instance "eth" "defaultAccount")))))

;; Effects
(defmulti reg-fx identity)
(defmethod reg-fx ::call [k]
  (re-frame/reg-fx
   k  (fn [{:keys [:web3 :fns] :as params}]
        (s/assert :district0x.re-frame.web3-fx/call params)
        (doseq [{:keys [:instance :fn :args :tx-opts
                        :on-success :on-error]} (remove nil? fns)]
          (if instance
            (apply web3-eth/contract-call
                   (concat [instance fn]
                           args
                           [(dispach-fn on-success on-error)]))
            (apply fn (concat [web3] args [(dispach-fn on-success on-error)])))))))

;; Init
(defmethod ig/init-key :scoopmarket.module/web3
  [k {:keys [network-id]}]
  (js/console.log (str "Initializing " k))
  (let [subs (->> reg-sub methods (map key))
        events (->> reg-event methods (map key))
        effects (->> reg-fx methods (map key))
        web3-instance (aget js/window "web3")]
    (->> subs (map reg-sub) doall)
    (->> events (map reg-event) doall)
    (->> effects (map reg-fx) doall)
    (re-frame/dispatch-sync [::init web3-instance network-id])
    {:subs subs :events events :effects effects}))

;; Halt
(defmethod ig/halt-key! :scoopmarket.module/web3
  [k {:keys [:subs :events :effects]}]
  (js/console.log (str "Halting " k))
  (re-frame/dispatch-sync [::halt])
  (->> subs (map re-frame/clear-sub) doall)
  (->> events (map re-frame/clear-event) doall)
  (->> effects (map re-frame/clear-fx) doall))
