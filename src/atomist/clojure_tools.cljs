;; Copyright © 2020 Atomist, Inc.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns atomist.clojure-tools
  (:require [cljs-node-io.core :as io]
            [atomist.cljs-log :as log]
            [atomist.json :as json]
            [atomist.sha :as sha]
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.string :as s]
            [cljs.pprint :refer [pprint]]
            [cljs.tools.reader.edn :as edn])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn library-name->name [s]
  (-> s
      (s/replace #"@" "")
      (s/replace #"/" "::")))

(defn data->sha [data]
  (sha/sha-256 (json/->str data)))

(defn data->library-version [data]
  [(if (= (:group data) (:artifact data))
     (:artifact data)
     (gstring/format "%s/%s" (:group data) (:artifact data)))
   (:version data)])

(defn library-version->data [[library version]]
  (assoc
   (if-let [[_ group artifact] (re-find #"(.*)/(.*)" library)]
     {:group group
      :artifact artifact}
     {:group library
      :artifact library})
   :version version))

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
  (go
    (let [f (io/file (:path project) "deps.edn")]
      (if (.exists f)
        (try
          (let [deps (-> (io/slurp f) (edn/read-string))]
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
          (catch :default ex
            (log/warn "unable to extract fingerprints from deps.edn:  " ex)
            []))
        []))))

(defn- edit-library [edn lib version]
  (assoc edn :deps
         (->> (:deps edn)
              (map (fn [[l m]]
                     (if (= (str l) lib)
                       [l {:mvn/version version}]
                       [l m])))
              (into {}))))

(defn apply-library-editor
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
      (let [f (io/file (:path project) "deps.edn")
            [library-name library-version] (data->library-version (:data target-fingerprint))]
        (log/info "applying " library-name " and " library-version)
        (io/spit f (with-out-str (pprint (edit-library (edn/read-string (io/slurp f)) library-name library-version)))))
      :success
      (catch :default ex
        (log/error "failure updating deps.edn for dependency change" ex)
        :failure))))
