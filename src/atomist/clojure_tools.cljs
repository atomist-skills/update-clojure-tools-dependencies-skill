(ns atomist.clojure-tools
  (:require [cljs-node-io.core :as io]
            [cljs-node-io.fs :as fs]
            [atomist.cljs-log :as log]
            [cljs.core.async :refer [<! timeout chan]]
            [atomist.json :as json]
            [atomist.sha :as sha]
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.string :as s])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn library-name->name [s]
  (-> s
      (s/replace-all #"@" "")
      (s/replace-all #"/" "::")))

(defn data->sha [data]
  (sha/sha-256 (json/->str data)))

(defn data->library-version [data]
  [(if (= (:group data) (:artifact data))
     (gstring/format "%s/%s" (:group data) (:artifact data))
     (:artifact data)) (:version data)])

(defn library-version->data [[library version]]
  )

(defn ->coordinate [[n v]]
  (merge
   {:version v}
   (if-let [[_ g a] (re-find #"(.*)/(.*)" n)]
     {:group g
      :artifact a}
     {:group n
      :artifact n})))

(defn extract
  "extract fingerprints from a project.clj file

    we use cljs-node-io Files when we call atomist.lein/deps

    fingerprints names should not have '/'s - they should be transformed to '::'s

    returns array of leiningen fingerprints or empty [] if project.clj is not present"
  [project]
  (let [f (io/file (. ^js project -baseDir) "deps.edn")]
    (if (.exists f)
      (let [deps (-> (io/slurp f) (cljs.reader/read-string))]
        (->> (:deps deps)
             (map (fn [[sym version]] [(str sym) (:mvn/version version)]))
             (map (fn [data]
                    {:type "maven-direct-dep"
                     :name (library-name->name (nth data 0))
                     :displayName (nth data 0)
                     :displayValue (nth data 1)
                     :displayType "MVN Coordinate"
                     :data (->coordinate data)
                     :sha (data->sha (->coordinate data))
                     :abbreviation "m2"
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
  [project target-fingerprint]
  (go
    (try
      (let [f (io/file (. ^js project -baseDir) "deps.edn")
            [library-name library-version] (data->library-version (:data target-fingerprint))]
        (log/info "applying " library-name " and " library-version)
        (io/spit f (with-out-str (cljs.pprint/pprint (edit-library (cljs.reader/read-string (io/slurp f)) library-name library-version)))))
      :success
      (catch :default ex
        (log/error "failure updating deps.edn for dependency change" ex)
        :failure))))
