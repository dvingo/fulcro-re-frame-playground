(ns dv.fulcro-re-frame.play.client.ui.task-page
  (:require
    [com.fulcrologic.fulcro.components :as c :refer [defsc]]
    [dv.fulcro-re-frame.play.client.ui.task-item :refer
     [ui-task-list TaskList TaskForm ui-task-form TaskItem]]))

(defsc TaskPage
  [this {:keys [task-list task-form]}]
  {:query         [{:task-list (c/get-query TaskList)}
                   {:task-form (c/get-query TaskForm)}]
   :route-segment ["tasks"]
   :initial-state (fn [_] {:task-form (c/get-initial-state TaskForm)
                           :task-list (c/get-initial-state TaskList)})
   :ident         (fn [_] [:component/id :root])}
  [:div
   (ui-task-list task-list)])
