(ns atomist.main
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<! >! timeout chan]]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [atomist.clojure-tools :as tools]
            [atomist.deps :as deps]
            [atomist.api :as api])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn just-fingerprints
  [request]
  (go
    (try
      (let [fingerprints (tools/extract (:project request))]
       ;; return the fingerprints in a form that they can be added to the graph
        fingerprints)
      (catch :default ex
        (log/error "unable to compute deps.edn fingerprints")
        (log/error ex)
        {:error ex
         :message "unable to compute deps.edn fingerprints"}))))

(def apply-policy (partial deps/apply-policy-targets {:type "maven-direct-dep"
                                                      :apply-library-editor tools/apply-library-editor
                                                      :->library-version tools/data->library-version
                                                      :->data tools/library-version->data
                                                      :->sha tools/data->sha
                                                      :->name tools/library-name->name}))

(defn compute-fingerprints
  [request]
  (go
    (try
      (let [fingerprints (tools/extract (:project request))]
       ;; first create PRs for any off target deps
        (<! (apply-policy (assoc request :fingerprints fingerprints)))
       ;; return the fingerprints in a form that they can be added to the graph
        fingerprints)
      (catch :default ex
        (log/error "unable to compute deps.edn fingerprints")
        (log/error ex)
        {:error ex
         :message "unable to compute deps.edn fingerprints"}))))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (deps/deps-handler data sendreponse
                     ["ShowToolsDepsDependencies"]
                     ["SyncToolsDepsDependency"]
                     ["UpdateToolsDepsDependency"
                      (api/compose-middleware
                       [deps/set-up-target-configuration]
                       [api/check-required-parameters {:name "dependency"
                                                       :required true
                                                       :pattern ".*"
                                                       :validInput "[lib version]"}]
                       [api/extract-cli-parameters [[nil "--dependency dependency" "[lib version]"]]])]
                     just-fingerprints
                     compute-fingerprints
                     deps/mw-validate-policy))
