(ns atomist.config
  (:require [atomist.deps :as deps]
            [atomist.api :as api]
            [atomist.cljs-log :as log]
            [cljs.core.async :refer [<! timeout chan]]
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.edn :as edn])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn- transform-dependency-to-edn-format
  "transform the dependencies parameter in a configuration from application/json to the edn format"
  [configuration]
  (letfn [(transform-edn [s]
            (->> (try
                   (edn/read-string s)
                   (catch :default _
                     (throw (ex-info "dependencies configuration was not valid JSON"
                                     {:policy "manualconfiguration"
                                      :message (gstring/format "bad EDN:  %s" s)}))))
                 (map (fn [[k v]] [k (:mvn/version v)]))
                 (into [])
                 (pr-str)))]
    (update configuration :parameters (fn [parameters]
                                        (->> parameters
                                             (map #(if (= "dependencies" (:name %))
                                                     (update % :value transform-edn)
                                                     %)))))))

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
                                         (transform-dependency-to-edn-format %)
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
         (log/error ex)
         (<! (api/finish request :failure (-> (ex-data ex) :message))))))))
