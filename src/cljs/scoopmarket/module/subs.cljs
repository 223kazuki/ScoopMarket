(ns scoopmarket.module.subs
  (:require [re-frame.core :as re-frame]
            [integrant.core :as ig]))

(defmethod ig/init-key :scoopmarket.module.subs [_ _]
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
    ::web3
    (fn [db]
      (:web3 db)))

   (re-frame/reg-sub
    ::ipfs
    (fn [db]
      (:ipfs db)))

   (re-frame/reg-sub
    ::uport
    (fn [db]
      (:uport db)))

   (re-frame/reg-sub
    ::scoops
    (fn [db]
      (:scoops db)))

   (re-frame/reg-sub
    ::credential
    (fn [db]
      (:credential db)))])

(defmethod ig/halt-key! :scoopmarket.module.subs [_ _]
  (re-frame/clear-sub))
