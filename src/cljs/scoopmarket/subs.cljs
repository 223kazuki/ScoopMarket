(ns scoopmarket.subs
  (:require [re-frame.core :as re-frame]
            [integrant.core :as ig]))

(defmethod ig/init-key ::module [_ _]
  [(re-frame/reg-sub
    ::active-panel
    (fn [db]
      (:active-panel db)))

   (re-frame/reg-sub
    ::sidebar-opened
    (fn [db]
      (:sidebar-opened db)))

   (re-frame/reg-sub
    ::abi-loaded
    (fn [db]
      (:abi-loaded db)))

   (re-frame/reg-sub
    ::my-address
    (fn [db]
      (:my-address db)))

   (re-frame/reg-sub
    ::loading?
    (fn [db]
      (:loading? db)))

   (re-frame/reg-sub
    ::form
    (fn [db]
      (:form db)))

   (re-frame/reg-sub
    ::scoops
    (fn [db]
      (:scoops db)))

   (re-frame/reg-sub
    ::credential
    (fn [db]
      (:credential db)))

   (re-frame/reg-sub
    ::is-rinkeby?
    (fn [db]
      (:is-rinkeby? db)))])

(defmethod ig/halt-key! ::module [_ _]
  (re-frame/clear-sub))
