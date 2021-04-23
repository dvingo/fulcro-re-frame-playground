An experiment to combine re-frame subscriptions with fulcro components.


This repository shows one way of combining fulcro and re-frame to try to leverage the best of both.

It is essentially all of fulcro and only re-frame subscriptions.

# Goals

- Direct (props tunneling) component updates via fulcro.
- UI components that render derived data are re-rendered without needing to store denormalized data in the fulcro db.

# Methods to achieve the goals

- Make the fulcro state atom the re-frame.db/app-db and add reagent rendering support:
  https://github.com/dvingo/fulcro-re-frame-playground/blob/e3db968dd3dd4d2825ff2d5598a7ae568caf8c4b/src/main/dv/fulcro_re_frame/play/client/application.cljs

- Add a wrapper macro of `defsc` which:
  - Creates a re-frame subscription returning the fulcro component's `db->tree` whose query is `[::ComponentName-db->tree this]`
  - Allow specifying further re-frame subscriptions to specific props from a fulcro component's query.
    
https://github.com/dvingo/fulcro-re-frame-playground/blob/e3db968dd3dd4d2825ff2d5598a7ae568caf8c4b/src/main/dv/fulcro_re_frame/play/client/fe_macros.clj#L125
    
Using the above you can then write further derived re-frame subscriptions using those primitive subscriptions and 
create reagent components that are re-rendered when this derived data is changed without rendering from root.

This sample application demonstrates the simple scenario of displaying a count in a parent component (a derived fact) 
of a list of children's data.

A synchronous fulcro mutation is used to demonstrate the fulcro parent component's failure to re-render and show a re-frame
component re-rendering due to the subscription graph triggering an update.

See it running here:

https://dvingo.github.io/fulcro-re-frame-playground/

This is an experiment and there will likely be issues if you, for example, want to render a fulcro component from within
a reagent component (I think you should be able to pass the `db->tree` to the desired fulcro component using a subscription
but there are propbably gotchas).

# Usage:

If you want to try it out, just clone this repository there is a minimal API:

Include the macro in your component ns:
```clojure
(:require-macros [dv.fulcro-re-frame.play.client.fe-macros :as rfm :refer [defsc-re-frame]])
```

Define you fulcro component to support subscriptions:

```clojure
(defsc-re-frame TaskList [_ _] ;; same as usual fulcro component
```
Using this macro will result in re-frame subsciptions being generated.
If you want more fine grained subscriptions setup for only certain props of a fulcro component query annotate your fulcro component with:

```clojure
(defsc-re-frame YourComponent 
  [this props]
  {:query [:abc/prop1]
   ::rfm/subs  [:abc/prop1]}
  [:div  ;; your usual render
   ])
```

To get reagent re-rendering you need to make a separate component and cannot rely on using `(@rf/subscribe ..)` within the fulcro
component's render function:

```clojure 
(defn my-re-frame-component [this]
    (let [my-fulcro-prop @(rf/subscribe [::YourComponent-abc-prop1 this])]
      [:h3 "re-frame value: " my-fulcro-prop]))
```

Where `this` is the fulcro component instance.

You can also use the subscription name helper: `(sub-name YourComponent :abc/prop)` which returns the appropriate keyword for you.

Full example:

```clojure
(declare YourComponent)

(defn my-re-frame-component [this]
    (let [my-fulcro-prop @(rf/subscribe [::YourComponent-abc-prop this])]
    [:h3 "re-frame value: " my-fulcro-prop]))

(defsc-re-frame YourComponent
  [this props]
  {:query [:abc/prop1 :abc/id]
   :ident :abc/id
   ::rfm/subs [:abc/prop1]}
  [:div
   [my-re-frame-component this]])
```

You can use the generated subscriptions to compute derived data, and also combine subscriptions from multiple fulcro components,
or from any data in the app-db:

```clojure
(rf/reg-sub
  ::TaskList-total
  (fn [[_ this]] (rf/subscribe [(sub-name TaskList :all-tasks) this]
                   ;; ^ same as [::TaskList-all-tasks this]
                   ))
  (fn [items] (reduce (fn [a i] (+ a (:task/number i))) 0 items)))
```

# At the REPL

The generated subscriptions support passing a component instance, or passing a component and an ident to enable
seeing the output of your subscriptions in a REPL:

```clojure 
@(rf/subscribe [(sub-name TaskList :all-tasks) TaskList #uuid "...."])
```

# Development setup

## Shadow cljs tasks

In one terminal:

```bash
make
```
this runs `yarn` and starts the shadow-cljs server.

Wait for this to complete, then, in another terminal run:

```bash
make fe
```

This starts the shadow cljs watches.

The file `scripts/start_dev.sh` (invoked by `make fe`) generates a pprint helper ClojureScript file to
allow requiring cljs.pprint during development and not having to remove the namespace for production builds.

If you don't use `make fe` to build your ClojureScript this namespace won't exist on the first build.

Likewise, when building for production use: `make fe-release`


----

github pages is configured to render the html found under the 'docs' directory. The release build is copied there.
