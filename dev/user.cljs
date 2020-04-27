(ns user
  (:require [atomist.main]
            [atomist.cljs-log :as log]
            [atomist.clojure-tools]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_PROD_GITHUB_AUTH))
(def github-token (.. js/process -env -GITHUB_TOKEN))

(comment

 ;; this should fail because of a bad manualConfiguration
 (atomist.main/handler #js {:data {:Push [{:branch "master"
                                           :repo {:name "update-clojure-tools-dependencies-skill"
                                                  :org {:owner "atomist-skills"
                                                        :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                      :credential {:secret github-token}}}}
                                           :after {:message ""}}]}
                            :secrets [{:uri "atomist://api-key" :value token}]
                            :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                           {:name "dependencies" :value "blah"}]}]
                            :extensions {:team_id "T29E48P34"
                                         :correlation_id "corrid"}}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; this should work
 (atomist.main/handler #js {:data {:Push [{:branch "master"
                                           :repo {:name "update-clojure-tools-dependencies-skill"
                                                  :org {:owner "atomist-skills"
                                                        :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                      :credential {:secret github-token}}}}
                                           :after {:message ""}}]}
                            :secrets [{:uri "atomist://api-key" :value token}]
                            :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                           {:name "dependencies" :value "[[org.clojure/clojure \"1.10.2\"]]"}]}]
                            :extensions {:correlation_id "corrid"
                                         :team_id "T29E48P34"}}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; command handler to extract
 (atomist.main/handler #js {:command "ShowToolsDepsDependencies"
                            :source {:slack {:channel {:id "C012TCG93HN"}
                                             :user {:id "U2ATJPCSK"}
                                             :team {:id "T29E48P34"}}}
                            :team {:id "T29E48P34" :name "atomist-community"}
                            :correlation_id "corrid"
                            :api_version "1"
                            :raw_message "clj fingerprints"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; command handler to update
 (atomist.main/handler #js {:command "UpdateToolsDepsDependency"
                            :source {:slack {:channel {:id "C012TCG93HN"}
                                             :user {:id "U2ATJPCSK"}
                                             :team {:id "T29E48P34"}}}
                            :team {:id "T29E48P34" :name "atomist-community"}
                            :correlation_id "corrid"
                            :api_version "1"
                            :parameters [{:name "dependency" :value "[org.clojure/clojurescript \"1.10.522\"]"}]
                            :raw_message "clj update"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (cljs.pprint/pprint (js->clj (first args)))))

 (go (println (<! (atomist.graphql-channels/head-commits->channel
                   {:secrets [{:uri "atomist://api-key" :value token}]
                    :team {:id "AK748NQC5"}}
                   "clojure-tools-deps"
                   "com.atomist::api-cljs")))))
