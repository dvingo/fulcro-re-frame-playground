(ns dv.fulcro-re-frame.play.client.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [dv.fulcro-re-frame.play.client.prn-debug :refer [pprint-str]]
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [taoensso.timbre :as log]))

(defonce SPA
  (app/fulcro-app
    {:render-middleware (fn [this render] (r/as-element (render)))
     :render-root!      rdom/render}))
