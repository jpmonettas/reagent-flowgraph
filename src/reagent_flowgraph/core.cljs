(ns reagent-flowgraph.core
  (:require [reagent.core :as r]
            [clj-tree-layout.core :refer [layout-tree] :as tl]))

(defn merge-dimensions [t dimensions]
  (-> t
      (merge (get dimensions (:node-id t)))
      (update :childs #(mapv (fn [c] (merge-dimensions c dimensions)) %))))

(defn dimension-and-redraw [panel-node-comp internal-nodes-a childs-fn branch-fn]
  (let [dn (r/dom-node panel-node-comp)
        dom-width (.-offsetWidth dn)
        dom-height (.-offsetHeight dn)
        dom-sizes (reduce (fn [r n]
                            (if (-> n .-dataset .-type (= "node"))
                              (assoc r (.-id n) [(.-offsetWidth n) (.-offsetHeight n)])
                              r))
                          {}
                          (array-seq (.-childNodes dn)))
        dimensions (layout-tree @internal-nodes-a
                                {:sizes dom-sizes
                                 :childs-fn :childs
                                 :id-fn :node-id
                                 :branch-fn :childs
                                 :h-gap 5
                                 :v-gap 50})
        dimensioned-nodes (merge-dimensions @internal-nodes-a dimensions)]
    (.log js/console "Dimensions recalculated " dimensions)
    (when (not= @internal-nodes-a dimensioned-nodes)
      (reset! internal-nodes-a dimensioned-nodes))))

(defn node [id n x y render-fn]
  [:div
   {:id id
    :data-type :node
    :style {:position :absolute
            :top (or y 0) :left (or x 0)}}
   (render-fn n)])

(defn build-internal-tree [t branch-fn childs-fn]
  {:node-id (str (random-uuid))
   :original-node t
   :childs (mapv #(build-internal-tree % branch-fn childs-fn)
                 (childs-fn t))})

(defn flowgraph [nodes & {:keys [layout-width layout-height branch-fn childs-fn render-fn line-styles]
                          :or {line-styles {:stroke :red
                                            :stroke-width 2}} }]
  (let [internal-nodes-a (r/atom (build-internal-tree nodes branch-fn childs-fn))
        draws (atom 0)]
   (r/create-class
    {:component-did-mount (fn [this]
                            (.log js/console "Component mounted, recalculating")
                            (dimension-and-redraw this internal-nodes-a childs-fn branch-fn))
     :component-did-update (fn [this]
                             (.log js/console "Component updated. Draws " @draws)
                             (when (<= @draws 2)
                               (.log js/console "Component updated, recalculating")
                               (swap! draws inc)
                               (dimension-and-redraw this internal-nodes-a childs-fn branch-fn)))
     :component-will-receive-props (fn [this [old-nodes new-nodes]]
                                     (.log js/console "Component got new props, resetting internal atom.")
                                     (reset! draws 0)
                                     (reset! internal-nodes-a (build-internal-tree new-nodes branch-fn childs-fn)))
     :reagent-render (fn []
                       (let [nodes-seq (tree-seq :childs :childs @internal-nodes-a)
                             links (when (-> nodes-seq first :width) ;; only calculate links when we have sizes
                                     (for [n nodes-seq
                                           cn (:childs n)]
                                       (let [{x :x y :y width :width height :height} n
                                             {cx :x cy :y cwidth :width cheight :height} cn]
                                         [(str (:node-id n) (:node-id cn))
                                          (+ x (/ width 2))
                                          (+ y height)
                                          (+ cx (/ cwidth 2))
                                          cy])))]
                         [:div#panel {:style {:position :relative}}

                         ;; Links
                         [:svg {:width layout-width
                                :height layout-height}
                          (for [[lid x1 y1 x2 y2] links]
                            ^{:key lid}
                            [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2
                                    :style line-styles}])]

                         ;; Nodes
                         (for [n nodes-seq]
                           (let [{:keys [x y]} n]
                             ^{:key (:node-id n)} [node (:node-id n) (:original-node n) x y render-fn]))]))})))
