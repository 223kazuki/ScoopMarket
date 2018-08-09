(ns scoopmarket.subs
  (:require [re-frame.core :as re-frame]
            [integrant.core :as ig]))

(defmethod ig/init-key ::module [_ _]
  [(re-frame/reg-sub
    ::active-page
    (fn [db]
      (:active-page db)))

   (re-frame/reg-sub
    ::sidebar-opened
    (fn [db]
      (:sidebar-opened db)))

   (re-frame/reg-sub
    ::loading?
    (fn [db]
      (:loading? db)))

   (re-frame/reg-sub
    ::form
    (fn [db]
      (:form db)))

   (re-frame/reg-sub
    ::web3
    (fn [db]
      (:web3 db)))

   (re-frame/reg-sub
    ::scoops
    (fn [db]
      (:scoops db)))

   (re-frame/reg-sub
    ::credential
    (fn [db]
      (:credential db)))])

(defmethod ig/halt-key! ::module [_ _]
  (re-frame/clear-sub))
