(ns atomist.clojure-tools
  (:require [cljs-node-io.core :as io]
            [cljs-node-io.fs :as fs]
            [atomist.cljs-log :as log]
            [atomist.sdmprojectmodel :as sdm]
            [cljs.core.async :refer [<! timeout chan]]
            [atomist.json :as json]
            [atomist.sha :as sha]
            [goog.string :as gstring]
            [goog.string.format])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn extract
  "extract fingerprints from a project.clj file

    we use cljs-node-io Files when we call atomist.lein/deps

    fingerprints names should not have '/'s - they should be transformed to '::'s

    returns array of leiningen fingerprints or empty [] if project.clj is not present"
  [project]
  (let [f (io/file (. ^js project -baseDir) "deps.edn")]
    (if (fs/fexists? (.getPath f))
      (let [deps (-> (io/slurp f) (cljs.reader/read-string))]
        (->> (:deps deps)
             (map (fn [[sym version]] [(str sym) (:mvn/version version)]))
             (map (fn [data]
                    {:type "clojure-tools-deps"
                     :name (gstring/replaceAll (nth data 0) "/" "::")
                     :displayName (nth data 0)
                     :displayValue (nth data 1)
                     :displayType "Clojure Tools Deps"
                     :data data
                     :sha (sha/sha-256 (json/->str data))
                     :abbreviation "deps.edn"
                     :version "0.0.1"}))))
      [])))

(defn- edit-library [edn lib version]
  (assoc edn :deps
             (->> (:deps edn)
                  (map (fn [[l m]]
                         (if (= (str l) lib)
                           [l {:mvn/version version}]
                           [l m])))
                  (into {}))))

(defn- apply-library-editor
  "apply a library edit inside of a PR

    params
      project - the SDM project
      f - this is cljs-node-io File, not an SDM File
      pr-opts - must conform to {:keys [branch target-branch title body]}
      library-name - leiningen library name string (will have '/'s
      library-version - leiningen library version string

    returns channel"
  [project library-name library-version]
  (go
   (try
     (let [f (io/file (. ^js project -baseDir) "deps.edn")]
       (io/spit f (with-out-str (cljs.pprint/pprint (edit-library (cljs.reader/read-string (io/slurp f)) library-name library-version)))))
     :success
     (catch :default ex
       (log/error "failure updating deps.edn for dependency change" ex)
       :failure))))
