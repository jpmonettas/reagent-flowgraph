# reagent-flowchart

A reagent component for laying out tree nodes in 2D space.

## Features

- Tidy tree representations as per [Tilford and Reingold](http://hci.stanford.edu/cs448b/f09/lectures/CS448B-20091021-GraphsAndTrees.pdf)
aesthetics rules.
- Customizable node render function with any reagent component.

## Installation

## Usage

```clojure
(ns tester.core
  (:require [reagent.core :as r]
            [reagent-flowgraph.core :refer [flowgraph]]))


(defonce app-state (r/atom '(+ 1 2 (- 4 2) (/ 123 3) (inc 25))))

(defmulti render-node symbol?)

(defmethod render-node true [n]
  [:div {:style {:background-color :yellow
                 :border "1px solid black"
                 :padding "10px"
                 :border-radius "10px"}
         :on-click #(.log js/console n " clicked!")}
   (str n)])

(defmethod render-node :default [n]
  [:div (str n)])

(defn app []
  [:div
   [:h3 "Reagent tree layout component test"]
   [flowgraph @app-state
    :layout-width 1500 
    :layout-height 500
    :branch-fn #(when (seq? %) %)
    :childs-fn #(when (seq? %) %)
    :render-fn render-node]])

(r/render [app] (.getElementById js/document "app")) 
```

and that will render 

<img src="/doc/images/reagent-tree.png?raw=true"/>

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

#### :render-fn render-node

A one parameter fn that can be used as a reagent component. Will receive the full node 
as a parameter.
