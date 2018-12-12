(defproject ScoopMarket "0.1.0-SNAPSHOT"
  :description "A market place for scoops."
  :url "http://example.com/FIXME"
  :min-lein-version "2.5.3"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [integrant "0.7.0"]
                 [org.clojure/data.json "0.2.6"]
                 [cljsjs/buffer "5.1.0-1"]
                 [cljsjs/ipfs-api "18.1.1-0"]
                 [cljsjs/moment "2.22.2-2"]
                 [cljsjs/react-transition-group "2.4.0-0"]
                 [cljs-web3 "0.19.0-0-11"]
                 [kibu/pushy "0.3.8"]
                 [bidi "2.1.4"]
                 [soda-ash "0.83.0" :exclusions [[cljsjs/react]]]
                 [day8.re-frame/http-fx "0.1.6"]
                 [district0x.re-frame/web3-fx "1.0.5" :exclusions [[cljs-web3]]]]
  :plugins [[lein-cljsbuild "1.1.7"]]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]
  :figwheel {:css-dirs ["resources/public/css"]}
  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
  :aliases {"dev" ["do" "clean"
                   ["pdo" ["figwheel" "dev"]]]
            "build" ["with-profile" "+prod,-dev" "do"
                     ["clean"]
                     ["cljsbuild" "once" "min"]]}
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :prod {:dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]}
   :profiles/dev {}
   :project/dev  {:dependencies [[binaryage/devtools "0.9.10"]
                                 [day8.re-frame/re-frame-10x "0.3.6"]
                                 [day8.re-frame/tracing "0.5.1"]
                                 [figwheel-sidecar "0.5.16"]
                                 [cider/piggieback "0.3.10"]
                                 [meta-merge "1.0.0"]]
                  :resource-paths ["dev/resources" "build"]
                  :plugins      [[lein-figwheel "0.5.16"]
                                 [lein-doo "0.1.8"]
                                 [lein-pdo "0.1.1"]]}}
  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src" "dev/src"]
     :figwheel     {:on-jsload            cljs.user/reset}
     :compiler     {:main                 cljs.user
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           day8.re-frame-10x.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true
                                           "day8.re_frame.tracing.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config {:features-to-install :all}}}}
    {:id "min"
     :source-paths ["src"]
     :compiler     {:main            scoopmarket.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}
    {:id "test"
     :source-paths ["src" "test"]
     :compiler     {:main          scoopmarket.runner
                    :output-to     "resources/public/js/compiled/test.js"
                    :output-dir    "resources/public/js/compiled/test/out"
                    :optimizations :none}}]})
