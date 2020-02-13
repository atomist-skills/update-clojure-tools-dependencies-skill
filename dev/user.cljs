(ns user
  (:require [atomist.main]
            [atomist.cljs-log :as log]
            [atomist.api :as api]
            [atomist.deps :as deps]
            [atomist.clojure-tools]
            [cljs.core.async :refer [<!]]
            [atomist.sdmprojectmodel :as sdm]
            ["@atomist/sdm" :as atomistsdm]
            ["@atomist/automation-client" :as ac]
            ["@atomist/automation-client/lib/operations/support/editorUtils" :as editor-utils]
            [atomist.json :as json])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_STAGING))
(def github-token (.. js/process -env -GITHUB_TOKEN))

(comment
 ;; this will actually raise a PR when run with a real project
 (let [project #js {:baseDir "/Users/slim/atomist/atomisthqa/clj1"}]
   (go (println (<!
                 (deps/apply-policy-target
                  {:fingerprints (atomist.clojure-tools/extract project)
                   :project project
                   :ref {:branch "master"}
                   :secrets [{:uri "atomist://api-key" :value token}]
                   :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                  {:name "dependencies" :value "[[org.clojure/clojure \"1.10.1\"]]"}]}
                                    {:parameters [{:name "policy" :value "latestSemVerAvailable"}
                                                  {:name "dependencies" :value "[mount]"}]}
                                    {:parameters [{:name "policy" :value "latestSemVerUsed"}
                                                  {:name "dependencies" :value "[com.atomist/common]"}]}]}
                  (fn [_ pr-opts lib-name lib-version]
                    (log/info "update " lib-name lib-version)
                    (go :done))))))))
