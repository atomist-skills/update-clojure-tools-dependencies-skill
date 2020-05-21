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
