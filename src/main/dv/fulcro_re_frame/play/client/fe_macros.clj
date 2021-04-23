(ns dv.fulcro-re-frame.play.client.fe-macros
  (:require
    [clojure.spec.alpha :as s]
    [com.fulcrologic.fulcro.components :as c]
    [dv.fulcro-util :as fu]))

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

(comment
  (make-sub-keyword :goal/id)
  (make-sub-keyword :hi))

(defn re-frame-derive-sub
  "'prop' is non-namespaced simple keyword

 sample invocation:
 (re-frame-derive-sub \"hello.one\" :items 'debug-page)
  "
  [ns-str prop component-name]
  (let [component-name component-name #_(->kebab-case component-name)
        sub-kw         (keyword ns-str (str component-name "-" (make-sub-keyword prop)))
        query-keyword  (keyword ns-str (str component-name "-db->tree"))]
    `(do
       ;(js/console.log ~(str "in derive-sub: " prop))
       (js/console.log ~(str "in derive-sub query-keyword: " query-keyword))
       (js/console.log ~(str "in derive-sub sub-kw: " sub-kw))
       (re-frame.core/reg-sub ~sub-kw
         (fn [[~'_ this# arg#]] (re-frame.core/subscribe [~query-keyword this# arg#]))
         (fn [input#] (get input# ~prop))))))

(comment
  (re-frame-derive-sub "hello.one" :items 'debug-page)
  (re-frame-derive-sub "hello.one" :items 'DebugPage))

;; copied from fulcro.components
(s/def ::args
  (s/cat
    :sym symbol?
    :doc (s/? string?)
    :arglist (s/and vector? #(<= 2 (count %) 5))
    :options (s/? map?)
    :body (s/* any?)))

(defn parse-layer-3-subs
  "If the fulcro component declared any keys in the component to create layer-3 subscriptions of the data tree
  for the component, generate those subscriptions."
  [ns-str args]
  (let [{:keys [sym options]} (s/conform ::args args)]
    (when (contains? options ::subs)
      (let [{::keys [subs]} options]
        (->> subs (map (fn [k] (re-frame-derive-sub ns-str k sym))))))))

(defn db->tree-sub-orig
  [ns-str component-name]
  `(do
     ;(js/console.log "reg-sub: " ~(keyword ns-str (str component-name "-db->tree")))
     (re-frame.core/reg-sub ~(keyword ns-str (str component-name "-db->tree"))
       (fn [db# [~'_ component-instance#]]
         (assert component-instance#
           (str "component instance is nil in re-frame subscription."
             " You likely forgot to pass 'this' to subscribe."))
         (com.fulcrologic.fulcro.algorithms.denormalize/db->tree
           (com.fulcrologic.fulcro.components/get-query
             (com.fulcrologic.fulcro.components/get-class component-instance#) db#)
           (com.fulcrologic.fulcro.components/get-ident component-instance#) db#)))))

(defn db->tree-sub
  "Trying to allow passing this or component + ident for easier repl interaction"
  [ns-str component-name]
  `(do
     ;(js/console.log "reg-sub: " ~(keyword ns-str (str component-name "-db->tree")))
     (re-frame.core/reg-sub ~(keyword ns-str (str component-name "-db->tree"))
       (fn [db# args#]
         (let [args#     (drop 1 args#)
               num-args# (count args#)]
           ;(js/console.log "args: " args#)
           (cond
             (= num-args# 0)
             (throw (js/Error. (str "component instance is nil in re-frame subscription."
                                 " You likely forgot to pass 'this' to subscribe.")))
             ;; only 'this'
             (c/component-instance? (first args#))
             (let [component-instance# (first args#)]
               ;(js/console.log "passed this")
               (com.fulcrologic.fulcro.algorithms.denormalize/db->tree
                 (com.fulcrologic.fulcro.components/get-query
                   (com.fulcrologic.fulcro.components/get-class component-instance#) db#)
                 (com.fulcrologic.fulcro.components/get-ident component-instance#) db#))

             ;; passed component class and ident value
             (= num-args# 2)
             (let [[component-class# ident#] args#]
               (assert (c/component-class? component-class#)
                 (str "The first argument to your subscription is not a component class."
                   " subscription: " ~(keyword ns-str (str component-name "-db->tree"))))

               (assert ident#
                 (str "You passed a nil ident to subscription :"
                   ~(keyword ns-str (str component-name "-db->tree"))))

               (com.fulcrologic.fulcro.algorithms.denormalize/db->tree
                 (com.fulcrologic.fulcro.components/get-query component-class# db#) ident# db#))
             :else
             (throw (js/Error. (str "Too many arguments passed to subscription for: "
                                 ~(keyword ns-str (str component-name "-db->tree")))))))))))

(defmacro defsc-re-frame
  [& args]
  (let [sym          (first args)
        ns-name      (-> &env :ns :name clojure.core/str)
        re-frame-sub (db->tree-sub ns-name sym)
        layer-3-subs (parse-layer-3-subs ns-name args)]
    (if
      (seq layer-3-subs)
      `(do
         ~re-frame-sub
         ~@layer-3-subs
         (com.fulcrologic.fulcro.components/defsc ~@args))
      `(do
         ~re-frame-sub
         (com.fulcrologic.fulcro.components/defsc ~@args)))))

(macroexpand
  '(defsc-re-frame DebugPage
     [this {:keys    [item-list droppable-id]
            :ui/keys [my-number] :as props}]
     {:query             [{:item-list (c/get-query ItemsList2)}
                          :droppable-id
                          :ui/my-number]
      :initLocalState    (fn [_]
                           (log/info "init debug page")
                           #_(load-start-data SPA))
      ::subs             [:items]
      :ident             (fn [_] [:component/id :debug])
      :route-segment     (r/route-segment :debug)
      :initial-state     (fn [_] {:item-list    (c/get-initial-state ItemsList2)
                                  :droppable-id (fu/uuid)
                                  :ui/my-number 0})
      :componentDidMount (fn [this]
                           (log/info "DEBUG did mount"))}
     (let [v    @(rf/subscribe [::DebugPage-db->tree this])
           goal (-> gf/goals first
                  gdm/compute-goal-estimated-duration)]
       (em/theme-provider {:theme {:bg "rgba(200,20,0, 0.6)"}}
         [:div (ui-items-list-2 item-list)]))))
