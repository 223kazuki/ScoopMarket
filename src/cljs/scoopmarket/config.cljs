(ns scoopmarket.config)

(def debug? ^boolean goog.DEBUG)

(when debug? (enable-console-print!))

(def network-id (if debug? 1533140371286 4))
