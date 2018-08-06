(ns scoopmarket.routes
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]
            [scoopmarket.events :as events]))

(def routes ["/" {""       :mypage
                  "market" :market}])

(defn- parse-url [url]
  (when (empty? url)
    (set! js/window.location (str js/location.pathname "#/")))
  (let [url (-> url
                (clojure.string/split #"&")
                (first))]
    (bidi/match-route routes url)))

(defn- dispatch-route [matched-route]
  (let [panel-name (keyword (str (name (:handler matched-route)) "-panel"))]
    (re-frame/dispatch [::events/set-active-panel panel-name])))

(def history (pushy/pushy dispatch-route parse-url))

(defn app-routes []
  (.setUseFragment (aget history "history") true)
  (pushy/start! history))

(def url-for (partial bidi/path-for routes))

(defn go-to-page [route]
  (pushy/set-token! history (url-for route)))
