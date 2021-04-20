(ns dv.fulcro-re-frame.play.client.ui.styles.global-styles)

(defn page-styles [theme]
  {":root"                {:box-sizing "border-box"}
   "*, ::after, ::before" {:box-sizing "inherit"}
   :body
                          {:background-color (:bg theme)
                           :background-image (:bg-image theme)
                           :color            (:fg theme)
                           :font-size        "1.2rem"
                           :line-height      1.3
                           :font-family      "helvetica, sans-serif"}})

(defn semantic-styles [{:keys [fg container-bg]}]
  {".ui.segment.ui.segment"
   {:color            fg
    :background-color container-bg}
   ".ui.header.ui.header"
   {:color fg}})

(defn global-styles
  [theme]
  (merge
    (page-styles theme)
    (semantic-styles theme)))
