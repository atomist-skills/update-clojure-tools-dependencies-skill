(ns atomist.main
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<! >! timeout chan]]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [atomist.clojure-tools :as tools]
            [atomist.deps :as deps]
            [atomist.api :as api]
            [atomist.config :as config])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (deps/deps-handler
   data
   sendreponse
   :deps-command/show "ShowToolsDepsDependencies"
   :deps-command/sync "SyncToolsDepsDependency"
   :deps-command/update "UpdateToolsDepsDependency"
   :deps/type "maven-direct-dep"
   :deps/apply-library-editor tools/apply-library-editor
   :deps/extract tools/extract
   :deps/->library-version tools/data->library-version
   :deps/->data tools/library-version->data
   :deps/->sha tools/data->sha
   :deps/->name tools/library-name->name
   :deps/validate-policy config/validate-deps-policy
   :deps/validate-command-parameters (api/compose-middleware
                                      [deps/set-up-target-configuration]
                                      [api/check-required-parameters {:name "dependency"
                                                                      :required true
                                                                      :pattern ".*"
                                                                      :validInput "[lib version]"}]
                                      [api/extract-cli-parameters [[nil "--dependency dependency" "[lib version]"]]])))
