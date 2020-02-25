(ns user
  (:require [atomist.main]
            [atomist.cljs-log :as log]
            [atomist.api :as api]
            [atomist.deps :as deps]
            [atomist.clojure-tools]
            [cljs.core.async :refer [<!]]
            [atomist.sdmprojectmodel :as sdm]
            ["@atomist/sdm" :as atomistsdm]
            ["@atomist/automation-client" :as ac]
            ["@atomist/automation-client/lib/operations/support/editorUtils" :as editor-utils]
            [atomist.json :as json])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_STAGING))
(def github-token (.. js/process -env -GITHUB_TOKEN))

(comment
 ;; this should fail because of a bad manualConfiguration
 (atomist.main/handler #js {:data {:Push [{:branch "master"
                                           :repo {:name "depsedn"
                                                  :org {:owner "atomisthqa"
                                                        :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                      :credential {:secret github-token}}}}
                                           :after {:message ""}}]}
                            :secrets [{:uri "atomist://api-key" :value token}]
                            :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                           {:name "dependencies" :value "blah"}]}]
                            :extensions {:team_id "AK748NQC5"}}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; this should work
 (atomist.main/handler #js {:data {:Push [{:branch "master"
                                           :repo {:name "depsedn"
                                                  :org {:owner "atomisthqa"
                                                        :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                      :credential {:secret github-token}}}}
                                           :after {:message ""}}]}
                            :secrets [{:uri "atomist://api-key" :value token}]
                            :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                           {:name "dependencies" :value "[[org.clojure/clojure \"1.10.2\"]]"}]}]
                            :extensions {:team_id "AK748NQC5"}}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; command handler to extract
 (atomist.main/handler #js {:command "ShowToolsDepsDependencies"
                            :source {:slack {:channel {:id "CUFC92AFJ"}
                                             :user {:id "UDF0NFB5M"}}}
                            :team {:id "AK748NQC5"}
                            :raw_message "clj fingerprints"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; command handler to update
 (atomist.main/handler #js {:command "UpdateToolsDepsDependency"
                            :source {:slack {:channel {:id "CUFC92AFJ"}
                                             :user {:id "UDF0NFB5M"}}}
                            :team {:id "AK748NQC5"}
                            :parameters [{:name "dependency" :value "[org.clojure/clojurescript \"1.10.522\"]"}]
                            :raw_message "clj update"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args)))
 )
