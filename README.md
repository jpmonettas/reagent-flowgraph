# reagent-flowgraph

A reagent component for laying out tree nodes in 2D space.
If you are not using reagent but want to draw trees your own way check [clj-tree-layout](https://github.com/jpmonettas/clj-tree-layout).

## Features

- Tidy tree representations as per [Tilford and Reingold](http://hci.stanford.edu/cs448b/f09/lectures/CS448B-20091021-GraphsAndTrees.pdf)
aesthetics rules.
- Customizable node render function with any reagent component.

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/reagent-flowgraph.svg)](https://clojars.org/reagent-flowgraph)

## Usage

```clojure
(ns tester.core
  (:require [reagent.core :as r]
            [reagent-flowgraph.core :refer [flowgraph]]))

(defonce app-state (r/atom '(+ 1 2 (- 4 2) (/ 123 3) (inc 25))))

(defn app []
  [:div
   [flowgraph @app-state
    :layout-width 1500
    :layout-height 500
    :branch-fn #(when (seq? %) %)
    :childs-fn #(when (seq? %) %)
    :render-fn (fn [n] [:div {:style {:border "1px solid black"
                                      :padding "10px"
                                      :border-radius "10px"}}
                        (str n)] )]])

(defn init []
  (r/render [app] (.getElementById js/document "app")))
```

and that will render

<img src="/doc/images/reagent-tree-example-simple.png?raw=true"/>

A more colorful example. Supouse we want to draw some aspects of the clojurescript analyzer output tree.

```clojure
(ns tester.core
  (:require [reagent.core :as r]
            [reagent-flowgraph.core :refer [flowgraph]]
            [cljs.js :as j]))

(defonce app-state (r/atom {}))

(defmulti render-node :op)

(def styles {:border "1px solid #586e75" :padding "10px" :border-radius "10px"
             :background-color "#002b36" :color "#B58900"})

(defmethod render-node :fn [n]
  [:div {:style (merge styles {:color "#DC322F"})}
    [:b (str "(" (:name (:name n)) " ...)")]])

(defmethod render-node :if [{:keys [then test else]}]
  [:div {:style (merge styles {:color "#CB4B16"})}
   [:div {:style {:text-align :center}} [:b "IF"]]
   [:div {:style {:display :flex}}
    [:div.if-test [:b "test"] [:div (str "'" (:form test) "'")]]
    [:div.if-then [:b "then"] [:div (str "'" (:form then) "'")]]
    [:div.if-else [:b "else"] [:div (str "'" (:form else) "'")]]]])

(defmethod render-node :default [n]
  [:div {:style styles} (str ":op " (:op n))])

(defn app []
  [:div {:style {:background-color "#002b36"}}
   [flowgraph @app-state
    :layout-width 500
    :layout-height 1500
    :branch-fn :children
    :childs-fn :children
    :render-fn render-node
    :line-styles {:stroke-width 2
                  :stroke "#CB4B16"}]])

(defn init []
  (r/render [app] (.getElementById js/document "app"))

  (j/analyze-str (j/empty-state)
                 "(defn factorial [n]
                     (if (zero? n)
                        1
                        (* n (factorial (dec n)))))"
                 #(reset! app-state (:value %))))
```

which will render

<img src="/doc/images/reagent-tree-example-custom.png?raw=true"/>

## Options

#### :layout-width

An integer representing the width of the layout panel.

#### :layout-height

An integer representing the height of the layout panel.

#### :branch-fn

Is a fn that, given a node, returns true if can have
children, even if it currently doesn't.

#### :childs-fn

Is a fn that, given a branch node, returns a seq of its
children.

#### :render-fn

A one parameter fn that can be used as a reagent component. Will receive the full node
as a parameter.

#### :line-styles

A map with styles for the svg lines that join nodes.

## How does it works?

The trick is in rendering all nodes twice. The action goes like this :

- Traverse the tree rendering every node using :render-fn.
- Collect the width and height of every node.
- Now we have node sizes we use a library like [clj-tree-layout](https://github.com/jpmonettas/clj-tree-layout) to calculate nodes positions.
- With positions we can calculate edges.
- Render everything again.
