(ns dv.fulcro-re-frame.play.data-model.task
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [com.fulcrologic.guardrails.core :refer [>defn >def | => ?]]
    [dv.fulcro-util-common :as fu]
    #?(:clj [dv.crux-util :as cu])
    [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sample Task Model
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :task/id fu/id?)
(s/def :task/description string?)
(s/def :task/number int?)
(s/def :db/created-at inst?)

(def required-task-keys [:task/id :task/description])
(def optional-task-keys [])
(def all-task-keys (into required-task-keys optional-task-keys))
(def global-keys [:db/created-at :db/updated-at :crux.db/id])
(def db-task-keys (into all-task-keys global-keys))

(s/def ::task (s/keys :req [:task/id :task/description]
                :opt [:db/created-at :db/updated-at :crux.db/id]))

(>defn make-task
  [{:task/keys [id description number]
    :db/keys [created-at]
    :or        {id (fu/uuid) number 0
                created-at (js/Date.)}}]
  [map? => ::task]
  {:task/id          id
   :task/number      number
   :task/description description
   :db/created-at created-at})

(comment (make-task {:task/description "TEST"}))

(defn fresh-task [props]
  (make-task (merge props {:task/id (fu/uuid)})))

#?(:clj (cu/gen-make-db-entity make-db-task ::task))

(comment
  (make-task {:task/id          :TESTING
              :task/description "desc"}))
