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

(ns user
  (:require [atomist.main]
            [atomist.clojure-tools]
            [cljs.core.async :refer [<!]]
            [atomist.local-runner :refer [call-event-handler fake-push fake-command-handler]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)
(atomist.local-runner/set-env :prod-github-auth)

(comment

 ;; this should fail because of a bad manualConfiguration
  (-> (fake-push "T29E48P34" "atomist-skills" "update-clojure-tools-dependencies-skill" "master")
      (assoc :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                            {:name "dependencies" :value "blah"}]}])
      (call-event-handler atomist.main/handler))

  (-> (fake-push "T29E48P34" "atomist-skills" "update-clojure-tools-dependencies-skill" "master")
      (assoc :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                            {:name "dependencies" :value "{rewrite-cljs {:mvn/version \"1.10.2\"}}"}]}])
      (call-event-handler atomist.main/handler))

  (-> (fake-command-handler "T29E48P34" "ShowToolsDepsDependencies" "clj fingerprints" "C012TCG93HN" "U2ATJPCSK")
      (call-event-handler atomist.main/handler))

  (-> (fake-command-handler "T29E48P34" "ShowToolsDepsDependencies" "clj fingerprints" "C012TCG93HN" "U2ATJPCSK")
      (assoc :parameters [{:name "dependency" :value "[org.clojure/clojurescript \"1.10.522\"]"}])
      (call-event-handler atomist.main/handler)))
