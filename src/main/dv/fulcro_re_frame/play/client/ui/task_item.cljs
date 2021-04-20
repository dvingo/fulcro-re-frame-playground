(ns dv.fulcro-re-frame.play.client.ui.task-item
  (:require
    [clojure.spec.alpha :as s]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.components :as c :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.ui-state-machines :as sm]
    [dv.cljs-emotion-reagent :refer [defstyled]]
    [dv.fulcro-util :as fu]
    [dv.fulcro-entity-state-machine :as fmachine]
    [dv.fulcro-re-frame.play.data-model.task :as dm]
    [re-frame.core :as rf]
    [taoensso.timbre :as log])
  (:require-macros [dv.fulcro-re-frame.play.client.fe-macros :as rfm :refer [defsc-re-frame]]))

(defstyled flex :div
  {:display     "flex"
   :align-items "center"
   "> * + *"    {:margin-left "0.5em"}})

(defstyled bold :div
  {:font-weight "700"})

(defsc TaskItem
  [this {:task/keys [id description number] :ui/keys [show-debug?]}]
  {:query [:task/id :task/description :task/number :ui/show-debug?]
   :ident :task/id}
  [:div.ui.segment
   [:h4.ui.header "Task Item"]
   [flex [bold "ID: "] [:span (pr-str id)]]
   [flex [bold "Description: "] [:span description]]
   [flex {:style {:margin-bottom "1em"}} [bold "Number: "] [:span number]]
   [:button.ui.button.mini {:on-click #(m/set-integer!! this :task/number :value (inc number))} "Inc"]
   [:button.ui.button.mini {:on-click #(m/toggle!! this :ui/show-debug?)}
    (str (if show-debug? "Hide" "Show") " debug")]
   (fu/props-data-debug this show-debug?)])

(def ui-task-item (c/factory TaskItem {:keyfn :task/id}))


;; maybe it's a better design to juse keep the case of the keywords
;; then when you manually type them there is one less translation step.
(rf/reg-sub
  ::TaskList-total
  (fn [[_ this]] (rf/subscribe [::TaskList-all-tasks this]))
  (fn [items] (reduce (fn [a i] (+ a (:task/number i))) 0 items)))

(defn total-component [this]
  (log/info "render total")
  (let [total @(rf/subscribe [::TaskList-total this])]
    [:h3 "Total re-frame: " total]))

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
  (let [local-total (reduce (fn [a i] (+ a (:task/number i))) 0 all-tasks)]
    [:div "This is the list of tasks"
     [:div.ui.divider]
     [:h3 "fulcro total: " local-total]
     [total-component this]
     (map ui-task-item all-tasks)]))

(def ui-task-list (c/factory TaskList))

(defn task-item-card
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
