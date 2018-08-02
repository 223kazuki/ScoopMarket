(ns scoopmarket.client.config)

(def debug? ^boolean goog.DEBUG)

(when debug? (enable-console-print!))

(def network-id (if debug? 4 4))
