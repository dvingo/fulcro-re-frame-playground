(ns dv.fulcro-re-frame.play.client.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    ;[dv.fulcro-re-frame.play.client.prn-debug :refer [pprint-str]]
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [re-frame.db :as rdb]))

(defonce SPA
  (->
    (app/fulcro-app
      {:render-middleware (fn [_ render] (r/as-element (render)))
       :render-root!      rdom/render})
    (assoc ::app/state-atom rdb/app-db)))
