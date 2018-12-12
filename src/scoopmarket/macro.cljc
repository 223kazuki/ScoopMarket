(ns scoopmarket.macro
  (:require #?(:clj [clojure.data.json :as json])))

(defmacro json [file]
  #?(:clj (-> file
              clojure.core/slurp
              (json/read-str :key-fn keyword))))
