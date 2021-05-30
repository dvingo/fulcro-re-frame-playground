(ns dv.fulcro-re-frame.play.client.ui.task-item
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [dv.fulcro-re-frame.play.client.application :refer [SPA]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.components :as c :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.ui-state-machines :as sm]
    [dv.cljs-emotion-reagent :refer [defstyled]]
    [dv.fulcro-entity-state-machine :as fmachine]
    [dv.fulcro-re-frame.play.data-model.task :as dm]
    [dv.fulcro-util :as fu]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.application :as app])
  (:require-macros [dv.fulcro-re-frame.play.client.fe-macros :as rfm :refer [defsc-re-frame]]))

(defstyled flex :div
  {:display     "flex"
   :align-items "center"
   "> * + *"    {:margin-left "0.5em"}})

(defstyled bold :div
  {:font-weight "700"})

(defn inc-task-num [s ref]
  (-> s (update-in (conj ref :task/number) inc)))
;
;(rf/reg-event-db ::inc
;  (fn [db [_ this]]
;    (inc-task-num db this)))

(defmutation inc-num [_]
  (action [{:keys [state ref]}]
    (log/info "inc num ident: " ref)
    (swap! state #(inc-task-num % ref))))

(defn inc-num! [this args]
  (c/transact!! this [(inc-num args)] {:refresh [:all-tasks]}))

(defsc TaskItem
  [this {:task/keys [id description number]
         :db/keys   [created-at] :ui/keys [show-debug?]}]
  {:query [:task/id :task/description :task/number :ui/show-debug?
           :ui/editing?
           :db/created-at]
   :ident :task/id}
  [:div.ui.segment
   [:h4.ui.header "Task Item"]
   [flex [bold "ID: "] [:span (pr-str id)]]
   [flex [bold "Description: "] [:span description]]
   [flex {:style {:margin-bottom "1em"}} [bold "Number: "] [:span number]]

   [:button.ui.button.mini {:on-click #_(m/set-integer!! this :task/number :value (inc number))
                            #(inc-num! this {})} "Inc"]
   [:button.ui.button.mini {:on-click #(m/toggle!! this :ui/show-debug?)}
    (str (if show-debug? "Hide" "Show") " debug")]
   (fu/props-data-debug this show-debug?)])

(def ui-task-item (c/factory TaskItem {:keyfn :task/id}))

(defn make-sub-keyword
  "Takes prop name (keyword as used in a fulcro query) and returns a string version of it to
  be used in a re-frame subscription.

  ex:
  :task/id => task-id
  :id => id"
  [prop]
  (if (qualified-keyword? prop)
    (str (namespace prop) "-" (name prop))
    (name prop)))

(defn sub-name
  "Returns a fq keyword following the conventions the macro uses to setup re-frame subscriptions for fulcro components:
  ComponentName-prop-name
  or:
  ComponentName-ns-name-prop-name"
  [cls prop]
  (let [[ns-name cls-name] (str/split (c/component-name cls) "/")]
    (keyword ns-name (str cls-name "-" (make-sub-keyword prop)))))

(declare TaskList)

(rf/reg-sub
  ::TaskList-total
  (fn [[_ this]] (rf/subscribe
                   [(sub-name TaskList :all-tasks) this]
                   ;; ^ same as [::TaskList-all-tasks this]
                   ))
  (fn [items] (reduce (fn [a i] (+ a (:task/number i))) 0 items)))

(defn format-date [d] (-> d (.toISOString) (.split "T") first))

(rf/reg-sub
  ::TaskList-by-date
  (fn [[_ arg1 arg2]] (rf/subscribe [(sub-name TaskList :all-tasks) arg1 arg2]))
  (fn [items]
    (log/info "by date items: " items)
    (let [by-date (group-by #(-> % :db/created-at format-date) items)]
      by-date)))

(defn total-component [this]
  (log/info "render total")
  (let [total @(rf/subscribe [::TaskList-total this])]
    [:h3 "re-frame total: " total]))

(comment (format-date (js/Date.)))

;; so next I want to render an input for a task - and have a list of all tasks
;; and list of tasks by date and then mutate a description of a task and see if it updates.
;;

(defn format-time [d]
  (str/join ":" [(.getHours d) (.getMinutes d) (.getSeconds d)]))
(comment (format-time (new js/Date)))

(defmutation toggle-edit-task [{:task/keys [id]}]
  (action [{:keys [state]}]
    (swap! state #(update-in % [:task/id id :ui/editing?] not))))

(defmutation edit-task [{:task/keys [id] :as t}]
  (action [{:keys [state]}]
    (swap! state #(update-in % [:task/id id] (fn [t-curr] (merge t-curr t))))))

(defn task-item-view [{:task/keys [id description number] :as t}]
  [:div.task-item "task item: "
   [:button.ui.button
    {:on-click #(c/transact! SPA [(inc-num)] {:ref [:task/id id]})} "inc"]
   [:button.ui.button.mini {:on-click #(c/transact! SPA [(toggle-edit-task t)]
                                         {:synchronous? true
                                          :refresh      [[:task/id id]]
                                          :ref          [:task/id id]})} "toggle edit"]
   [:pre "editing: " (pr-str (:ui/editing? t))]
   [:div (format-time (js/Date.))]
   [:div "count: " number]
   (when (:ui/editing? t)
     [:div
      [:p "you are editing me: "]
      [:input {:value (:task/description t) :on-change
                      (fn [e]
                        (.log js/console "on change: " (.. e -target -value))
                        (c/transact! SPA [(edit-task (assoc t :task/description (.. e -target -value)))]
                          {:synchronous? true
                           :ref          [:task/id id]}))}]])
   [:pre (:task/id t)]
   [:div (:task/description t)]])


(defn ui-tasks-by-date [this]
  ;; todo install spyscope to use during dev
  (let [by-date @(rf/subscribe [::TaskList-by-date this])]
    (log/info "by date rendered " by-date)
    [:<>
     [:pre (format-time (js/Date.))]
     [:h3 "re-frame by-date: " (str (count by-date))]

     #_[:h4 "dates: "]
     #_(->> (keys by-date)
         (map (fn [d]
                ^{:key d}
                [:p (format-date d)])))

     (for [date (keys by-date)]
       ^{:key date}
       [:div
        [:h2 "date: " date #_(format-date date)]
        (map (fn [t] ^{:key (:task/id t)}
               [task-item-view t])
          (get by-date date))])]))

(defsc-re-frame TaskList
  [this {:keys [all-tasks]}]
  {:initial-state (fn [_] {:all-tasks
                           (->>
                             (range 10)
                             (mapv
                               #(let [num (inc (rand-int 10))]
                                  (dm/make-task {:task/description (str "task " num)
                                                 :task/number      num}))))})
   ::rfm/subs     [:all-tasks]
   :ident         (fn [_] [:component/id ::task-list])
   :query         [{:all-tasks (c/get-query TaskItem)}]}
  (log/info "REFRESH TaskList")
  (let [local-total (reduce (fn [a i] (+ a (:task/number i))) 0 all-tasks)]
    [:div "This is the list of tasks"
     [:div (format-time (js/Date.))]
     [:div.ui.divider]
     [:h3 "fulcro total: " local-total]
     [total-component this]
     [:div {:style {:display "flex"}}
      [ui-tasks-by-date this]
      (map ui-task-item all-tasks)]]))

(comment
  (let [db (app/current-state SPA)]
    (fdn/db->tree (c/get-query TaskList db) [:component/id ::task-list] db))
  (c/get-query TaskList (app/current-state SPA))
  )
(comment
  @(rf/subscribe [::TaskList-by-date TaskList [:component/id ::task-list]]))

(def ui-task-list (c/factory TaskList))

#_(defn task-item-card
    [{:task/keys [id description]}]
    [:div.ui.card {:key id}
     [:div.content>div.ui.tiny.horizontal.list>div.item description]])

(defn empty-form [] (dm/make-task {:task/description ""}))

(defn task-valid [form field]
  (let [v (get form field)]
    (s/valid? field v)))

(def validator (fs/make-validator task-valid))

(defsc TaskForm
  [this {:keys [server/message ui/machine-state ui/loading? ui/show-form-debug?] :as props}
   {:keys [cb-on-submit on-cancel]}]
  {:query         [:task/id :task/description fs/form-config-join
                   :ui/machine-state :ui/loading? :server/message
                   (sm/asm-ident ::form-machine)
                   :ui/show-form-debug?]
   :ident         :task/id
   :form-fields   #{:task/description}
   :initial-state (fn [_] (fs/add-form-config
                            TaskForm (merge (empty-form)
                                       {:ui/show-form-debug? false})))}
  (let [{:keys [checked? disabled?]} (fu/validator-state this validator)]
    [:div
     (fu/notification {:ui/submit-state machine-state :ui/server-message message})
     (when goog.DEBUG
       (fu/ui-button #(m/toggle! this :ui/show-form-debug?) "Debug form"))
     (fu/form-debug validator this show-form-debug?)

     [:h3 nil "Enter a new task"]

     [:div.ui.form
      {:class    (when checked? "error")
       :onChange (fn [e] (sm/trigger! this ::form-machine :event/reset)
                   true)}
      [:div.field
       (fu/ui-textfield this "Task Description" :task/description props :tabIndex 0
         :autofocus? true)]

      [:div.ui.grid
       [:div.column.four.wide>button
        {:tabIndex 0
         :disabled disabled?
         :onClick  (fu/prevent-default
                     #(let [task (dm/fresh-task props)]
                        (fmachine/submit-entity! this
                          {:entity          task
                           :machine         ::form-machine
                           :creating?       true
                           :remote-mutation `create-task
                           :on-reset        cb-on-submit
                           :target          {:append [:all-tasks]}})))
         :class    (str "ui primary button" (when loading? " loading"))}
        "Enter"]

       (when on-cancel
         [:div.column.four.wide>button.ui.secondary.button.column
          {:on-click on-cancel} "Cancel"])]]]))

(def ui-task-form (c/factory TaskForm {:keyfn :task/id}))
