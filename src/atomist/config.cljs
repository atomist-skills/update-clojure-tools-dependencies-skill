(ns atomist.config
  (:require [atomist.deps :as deps]
            [atomist.api :as api]
            [atomist.cljs-log :as log]
            [cljs.core.async :refer [<!]]
            [goog.string.format]
            [clojure.edn :as edn])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn- transform-dependency-to-edn-format
  "transform the dependencies parameter in a configuration from application/json to the edn format"
  [configuration]
  (letfn [(transform-edn [s]
            (->> (edn/read-string s)
                 (map (fn [[k v]] [k (:mvn/version v)]))
                 (into [])
                 (pr-str)))]
    (update configuration :parameters (fn [parameters]
                                        (->> parameters
                                             (map #(if (= "dependencies" (:name %))
                                                     (update % :value transform-edn)
                                                     %))
                                             (doall))))))

(defn validate-deps-policy
  "validate npm dependency configuration
    all configurations with a policy=manualConfiguration should have a dependency which is an application/json map
    all configurations with other policies use a dependency which is an array of strings"
  [handler]
  (fn [request]
    (go
      (try
        (let [configurations (->> (:configurations request)
                                  (map #(if (= "manualConfiguration" (deps/policy-type %))
                                          (try
                                            (transform-dependency-to-edn-format %)
                                            (catch :default _
                                              (log/warnf "Unable to transform %s to valid policy" %)
                                              (assoc % :error "Unable to transform configuration")))
                                          %))
                                  (map deps/validate-policy))]
          (if (->> configurations
                   (filter :error)
                   (empty?))
            (<! (handler (assoc request :configurations configurations)))
            (<! (api/finish request :failure (->> configurations
                                                  (map :error)
                                                  (interpose ",")
                                                  (apply str))))))
        (catch :default ex
          (log/error "unable to validate deps policy" ex)
          (<! (api/finish request :failure (str "unable to validate dependencies policy" ex))))))))
