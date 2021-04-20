(ns dv.fulcro-re-frame.play.client.client-entry
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.fulcro.components :as c]
    [clojure.edn :as edn]
    [dv.fulcro-re-frame.play.client.ui.root :as root]
    [dv.fulcro-re-frame.play.client.application :refer [SPA]]
    [shadow.resource :as rc]
    [dv.fulcro-reitit :as fr]
    [taoensso.timbre :as log]))

;; set logging lvl using goog-define, see shadow-cljs.edn
(goog-define LOG_LEVEL "warn")

(def fe-config (edn/read-string (rc/inline "/config/fe-config.edn")))
(log/info "Log level is: " LOG_LEVEL)

(def log-config
  (merge
    (-> fe-config ::config :logging)
    {:level (keyword LOG_LEVEL)}))

(defn ^:export refresh []
  (log/info "Hot code Remount")
  (log/merge-config! log-config)
  (c/refresh-dynamic-queries! SPA)
  (app/mount! SPA root/Root "app"))

(defn ^:export init []
  (log/merge-config! log-config)
  (log/info "Application starting.")
  (app/set-root! SPA root/Root {:initialize-state? true})
  (fr/start-router! SPA)
   (log/info "MOUNTING APP")
  (js/setTimeout #(app/mount! SPA root/Root "app" {:initialize-state? true})))
