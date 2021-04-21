(ns dv.fulcro-re-frame.play.client.ui.root
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as c :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.ui-state-machines :as sm]
    [dv.fulcro-reitit :as fr]
    [dv.cljs-emotion-reagent :refer [global-style theme-provider]]
    [dv.fulcro-re-frame.play.client.ui.task-item :refer [ui-task-list TaskList TaskForm ui-task-form]]
    [dv.fulcro-re-frame.play.client.application :refer [SPA]]
    [dv.fulcro-re-frame.play.client.ui.task-page :refer [TaskPage]]
    [dv.fulcro-re-frame.play.client.ui.styles.app-styles :as styles]
    [dv.fulcro-re-frame.play.client.ui.styles.global-styles :refer [global-styles]]
    [dv.fulcro-re-frame.play.client.ui.styles.style-themes :as themes]
    [taoensso.timbre :as log]))

(dr/defrouter TopRouter
  [this {:keys [current-state route-factory route-props]}]
  {:router-targets [TaskPage]})

(def ui-top-router (c/factory TopRouter))

(defsc PageContainer [this {:root/keys [router] :as props}]
  {:query         [{:root/router (c/get-query TopRouter)}
                   [::sm/asm-id ::TopRouter]]
   :ident         (fn [] [:component/id :page-container])
   :initial-state (fn [_] {:root/router (c/get-initial-state TopRouter {})})}
  [:div.ui.container
   [:div.ui.secondary.pointing.menu
    [:a.item {:href (fr/route-href :default)} "Home"]]
   (ui-top-router router)])

(fr/register-fulcro-router! SPA TopRouter)

(def ui-page-container (c/factory PageContainer))

(defsc Root [this {:root/keys [page-container style-theme]}]
  {:query         [{:root/page-container (c/get-query PageContainer)}
                   :root/style-theme]
   :initial-state (fn [_] {:root/page-container (c/get-initial-state PageContainer {})
                           :root/style-theme themes/light-theme})}
  (theme-provider
    {:theme style-theme}
    (global-style (global-styles style-theme))
    [:button {:on-click #(styles/toggle-app-styles! this style-theme)} "Switch Theme"]
    (ui-page-container page-container)))
