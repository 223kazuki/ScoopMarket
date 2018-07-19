(ns scoopmarket.client
  (:require [scoopmarket.client.core :as core]
            [goog.events :as events]))

(if-not @core/system
  (core/start)
  (do (core/stop) (core/start)))
