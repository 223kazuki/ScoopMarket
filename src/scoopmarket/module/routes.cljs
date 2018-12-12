(ns scoopmarket.module.routes
  (:require [bidi.bidi :as bidi]
            [integrant.core :as ig]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]
            [scoopmarket.module.events :as events]))

(defn app-routes [routes]
  (letfn [(dispatch-route [{:keys [:handler :route-params]}]
            (let [panel-name (keyword (str (name handler) "-panel"))]
              (re-frame/dispatch [::events/set-active-page panel-name route-params])))
          (parse-url [url]
            (when (empty? url)
              (set! js/window.location (str js/location.pathname "#/")))
            (let [url (-> url
                          (clojure.string/split #"&")
                          (first))]
              (bidi/match-route routes url)))]
    (let [history (pushy/pushy dispatch-route parse-url)]
      (.setUseFragment (aget history "history") true)
      (pushy/start! history)
      {:history history :routes routes})))

(defn go-to-page [{:keys [history routes]} route]
  (pushy/set-token! history (bidi/path-for routes route)))

(defmethod ig/init-key :scoopmarket.module/routes
  [_ {:keys [routes] :as opts}]
  (app-routes routes))

(defmethod ig/halt-key! :scoopmarket.module/routes
  [_ {:keys [history]}]
  (pushy/stop! history))
