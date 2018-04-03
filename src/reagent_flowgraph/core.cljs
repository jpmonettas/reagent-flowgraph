(ns reagent-flowgraph.core
  (:require [reagent.core :as r]
            [clj-tree-layout.core :refer [layout-tree] :as tl]
            [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn break]]))


(defn dimension-and-redraw [panel-node-comp internal-nodes dimensions-map-a childs-fn branch-fn]
  (let [dn (r/dom-node panel-node-comp)
        dom-width (.-offsetWidth dn)
        dom-height (.-offsetHeight dn)
        dom-sizes (reduce (fn [r n]
                            (assoc r (.-id n) [(.-offsetWidth n) (.-offsetHeight n)]))
                          {}
                          (array-seq (.-childNodes dn)))
        dimensions (layout-tree internal-nodes
                                {:sizes dom-sizes
                                 :childs-fn :childs
                                 :id-fn :node-id
                                 :branch-fn :childs
                                 :h-gap 5
                                 :v-gap 50})]
    (.log js/console "Dimensions recalculated " dimensions)
    (reset! dimensions-map-a dimensions)))

(defn node [id n x y render-fn]
  [:div
   {:id id
    :style {:position :absolute
            :top y :left x}}
   (render-fn n)])

(defn build-internal-tree [t branch-fn childs-fn]
  {:node-id (str (random-uuid))
   :original-node t
   :childs (mapv #(build-internal-tree % branch-fn childs-fn)
                 (childs-fn t))})

(defn flowgraph [nodes & {:keys [layout-width layout-height branch-fn childs-fn render-fn]}]
  (let [internal-nodes (build-internal-tree nodes branch-fn childs-fn)
        dimensions-map-a (r/atom {})]
   (r/create-class
    {:component-did-mount (fn [this] (dimension-and-redraw this internal-nodes dimensions-map-a childs-fn branch-fn))
     ;; :component-did-update (fn [this] (calculate-and-redraw this nodes-a))
     ;; :component-will-receive-props (fn [this [_ new-nodes]] (reset! nodes-a new-nodes))
     :reagent-render (fn [_]
                       (let [nodes-seq (tree-seq :childs :childs internal-nodes)
                             dimensions @dimensions-map-a
                             links (when (not-empty dimensions)
                                     (clog (for [n nodes-seq
                                            cn (:childs n)]
                                             (let [{x :x y :y width :width height :height} (get dimensions (:node-id n))
                                                   {cx :x cy :y cwidth :width cheight :height} (get dimensions (:node-id cn))]
                                          [(str (:node-id n) (:node-id cn))
                                           (+ x (/ width 2))
                                           (+ y height)
                                           (+ cx (/ cwidth 2))
                                           cy]))
                                           :js))]
                         [:div#panel {:style {:position :relative}}

                         ;; Links
                         [:svg {:width layout-width
                                :height layout-height}
                          (for [[lid x1 y1 x2 y2] links]
                            ^{:key lid}
                            [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2
                                    :style {:stroke :red
                                            :stroke-width 2}}])]

                         ;; Nodes
                         (for [n nodes-seq]
                           (let [{:keys [x y]} (get dimensions (:node-id n))]
                             ^{:key (:node-id n)} [node (:node-id n) (:original-node n) x y render-fn]))]))})))
