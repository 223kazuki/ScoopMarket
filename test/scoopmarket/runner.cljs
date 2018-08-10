(ns scoopmarket.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [scoopmarket.core-test]))

(doo-tests 'scoopmarket.core-test)
