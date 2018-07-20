(ns scoopmarket.client.config)

(def debug?
  ^boolean goog.DEBUG)

(when debug? (enable-console-print!))
