(ns atomist.main
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<! >! timeout chan]]
            [clojure.string :as s]
            [goog.crypt.base64 :as b64]
            [goog.string :as gstring]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [atomist.editors :as editors]
            [atomist.sdmprojectmodel :as sdm]
            [atomist.json :as json]
            [atomist.api :as api]
            [atomist.promise :as promise]
            [atomist.clojure-tools :as tools]
            [atomist.sha :as sha]
            [cljs-node-io.core :as io]
            ["@atomist/automation-client" :as ac]
            [atomist.deps :as deps])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn just-fingerprints
  [request project]
  (go
   (try
     (let [fingerprints (tools/extract project)]
       ;; return the fingerprints in a form that they can be added to the graph
       fingerprints)
     (catch :default ex
       (log/error "unable to compute deps.edn fingerprints")
       (log/error ex)
       {:error ex
        :message "unable to compute deps.edn fingerprints"}))))

(defn compute-fingerprints
  [request project]
  (go
   (try
     (let [fingerprints (tools/extract project)]
       ;; first create PRs for any off target deps
       (<! (deps/apply-policy-target
            (assoc request :project project :fingerprints fingerprints)
            tools/apply-library-editor))
       ;; return the fingerprints in a form that they can be added to the graph
       fingerprints)
     (catch :default ex
       (log/error "unable to compute deps.edn fingerprints")
       (log/error ex)
       {:error ex
        :message "unable to compute deps.edn fingerprints"}))))

(defn check-for-targets-to-apply [handler]
  (fn [request]
    (if (and
         (empty? (-> request :data :CommitFingerprintImpact :offTarget))
         (empty? (:configurations request)))
      (api/finish request)
      (handler request))))

(defn- handle-push-event [request]
  ((-> (api/finished :message "handling Push" :success "successfully handled Push event")
       (api/send-fingerprints)
       (api/run-sdm-project-callback compute-fingerprints)
       (api/extract-github-token)
       (api/create-ref-from-push-event)) request))

(defn- handle-impact-event [request]
  ((-> (api/finished :message "handling CommitFingerprintImpact")
       (api/extract-github-token)
       (api/create-ref-from-repo
        (-> request :data :CommitFingerprintImpact :repo)
        (-> request :data :CommitFingerprintImpact :branch))
       (check-for-targets-to-apply)) request))

(defn log-attempt [handler]
  (fn [request]
    (log/infof "compute leiningen fingerprints in %s" (:ref request))
    (handler request)))

(defn fp-command-handler [request]
  ((-> (api/finished :message "handling extraction CommandHandler")
       (api/show-results-in-slack :result-type "fingerprints")
       (api/run-sdm-project-callback just-fingerprints)
       (log-attempt)
       (api/create-ref-from-first-linked-repo)
       (api/extract-linked-repos)
       (api/extract-github-user-token)
       (api/set-message-id)) (assoc request :branch "master")))

(defn update-command-handler [request]
  ((-> (api/finished :message "handling application CommandHandler")
       (api/show-results-in-slack :result-type "fingerprints")
       (api/run-sdm-project-callback compute-fingerprints)
       (log-attempt)
       (api/create-ref-from-first-linked-repo)
       (api/extract-linked-repos)
       (api/extract-github-user-token)
       (api/check-required-parameters {:name "dependency"
                                       :required true
                                       :pattern ".*"
                                       :validInput "[lein-lib version]"})
       (api/extract-cli-parameters [[nil "--dependency dependency" "[lib version]"]])
       (api/set-message-id)) (assoc request :branch "master")))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (api/make-request
   data
   sendreponse
   (fn [request]
     (cond
       ;; handle Push events
       (contains? (:data request) :Push)
       (handle-push-event request)
       ;; handle Commit Fingeprint Impact events
       (contains? (:data request) :CommitFingerprintImpact)
       (handle-impact-event request)

       (= "ShowToolsDepsDependencies" (:command request))
       (fp-command-handler request)

       (= "UpdateToolsDepsDependency" (:command request))
       (update-command-handler request)))))
