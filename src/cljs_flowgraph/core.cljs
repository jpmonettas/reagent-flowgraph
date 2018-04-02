(ns cljs-flowgraph.core
  (:require [reagent.core :as r]
            [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn break]]))

(defonce comp-hiccup
  {'let (fn [d]
          [:div (str (:id d) " Let " (:width d) "x" (:height d))])
   'map (fn [d]
          [:div {:style {:background-color "#c7d493"}}
           (str (:id d) " Map "(:width d) "x" (:height d))])
   '+ (fn [d]
        [:div (str (:id d) " + "(:width d) "x" (:height d))])
   'x (fn [d]
        [:div
         [:b (:id d)] [:span "X"]
         [:ul
          [:li (str "Width: " (:width d))]
          [:li (str "Height: " (:height d))]]])})


(def *hgap* 50)
(def *vgap* 50)

(defn annotate-tree [tree]
  (let [x-numbers (atom {})
        aux (fn aux [t depth]
              (-> t
                  (assoc :depth depth
                         :depth-order (-> (swap! x-numbers update depth (fnil inc 0))
                                (get depth)))
                  (update :childs (fn [chlds]
                                    (mapv (fn [c]
                                            (aux c (inc depth)))
                                          chlds)))))]
    (aux tree 0)))

(defn tree-contours [t]
  (->> (tree-seq :childs :childs t)
       (group-by :depth)
       (map (fn [[l nodes]]
              (let [fnode (apply (partial min-key :depth-order) nodes)
                    lnode (apply (partial max-key :depth-order) nodes)]
               [l (:x fnode) (+ (:x lnode) (:width lnode))])))
       (sort-by first)
       (reduce (fn [r [_ left right]]
                 (-> r
                     (update :left conj left)
                     (update :right conj right)))
               {:left [] :right []})))


(defn max-distance [ns1 ns2]
  (apply max (map #(Math/abs (- %1 %2)) ns1 ns2)))

(defn push-tree [t delta]
  (-> t
      (update :x #(+ % delta))
      (update :childs (fn [chlds] (mapv #(push-tree % delta) chlds)))))


(defn layout-tree
  ([node] (layout-tree node 0))
  ([{:keys [width height depth childs id] :as node} y]
   (if (not-empty childs)
     (let [layout-childs (mapv #(layout-tree % (+ y height *vgap*)) childs)
           pushed-childs (loop [pusheds [(first layout-childs)]
                                [c & r] (rest layout-childs)]
                           (if c
                             (let [right-contour (:right (tree-contours (last pusheds)))
                                   left-contour (:left (tree-contours c))
                                   delta (+ (max-distance right-contour left-contour) *hgap*)]
                               (recur (conj pusheds (push-tree c delta)) r))
                             pusheds))
           firstc (first pushed-childs)
           lastc (last pushed-childs)
           childs-width (- (+ (:x lastc) (:width lastc)) (:x firstc))]
       (assoc node
              :x (+ (/ childs-width 2) (:x firstc))
              :childs pushed-childs
              :y y))
     (assoc node :x 0 :y y))))

(defn layout [t]
  (-> t
      annotate-tree
      layout-tree))

(defn assoc-sizes [n sizes-map]
  (let [[width height] (get sizes-map (:id n))]
   (assoc n
          :width width
          :height height
          :childs (mapv #(assoc-sizes % sizes-map) (:childs n)))))

(defn calculate-and-redraw [panel-node-comp nodes-a]
  (let [dn (r/dom-node panel-node-comp)
        dom-width (.-offsetWidth dn)
        dom-height (.-offsetHeight dn)
        dom-sizes (reduce (fn [r n]
                            (assoc r (.-id n) [(.-offsetWidth n) (.-offsetHeight n)]))
                          {}
                          (array-seq (.-childNodes dn)))
        nodes-with-sizes (layout (assoc-sizes @nodes-a dom-sizes))]
    (.log js/console "Nodes recalculated " nodes-with-sizes)
    (reset! nodes-a nodes-with-sizes)))

(defn node [{:keys [id x y width height type] :as n}]
  [:div
   {:id id
    :style {:position :absolute
            :top y :left x
            :border "1px solid black"
            :padding "10px"
            :border-radius "10px"}}
   ((get comp-hiccup type) n)])

(defn flowgraph [nodes width height]
  (let [nodes-a (r/atom nodes)]
   (r/create-class
    {:component-did-mount (fn [this] (calculate-and-redraw this nodes-a))
     ;; :component-did-update (fn [this] (calculate-and-redraw this nodes-a))
     :component-will-receive-props (fn [this [_ new-nodes]] (reset! nodes-a new-nodes))
     :reagent-render (fn [_]
                       (let [nodes-seq (tree-seq :childs :childs @nodes-a)
                             links (for [n nodes-seq
                                         cn (:childs n)]
                                     [(str (:id n) (:id cn))
                                      (+ (:x n) (/ (:width n) 2))
                                      (+ (:y n) (:height n))
                                      (+ (:x cn) (/ (:width cn) 2)) 
                                      (:y cn)])]
                        [:div#panel {:style {:position :absolute
                                             :top 150
                                             :width width
                                             :height height
                                             :background-color "#eee"}}
                         [:svg {:width width
                                :height height}
                          (for [[lid x1 y1 x2 y2] links]
                            ^{:key lid}
                            [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2
                                    :style {:stroke :red
                                            :stroke-width 2}}])]
                         (for [n nodes-seq]
                           ^{:key (:id n)} [node n])]))})))
 
(defonce app-state (r/atom {:id "1" :type 'let
                            :childs [{:id "11" :type 'map 
                                      :childs [{:id "111" :type 'x}
                                               {:id "112" :type 'x}
                                               {:id "113" :type 'x}
                                               {:id "114" :type 'x}]}
                                     {:id "12" :type '+
                                      :childs [{:id "121" :type 'x 
                                                :childs [{:id "1211" :type 'x }]}
                                               {:id "122" :type 'x :width 26 :height 20}
                                               {:id "123" :type 'x :width 26 :height 20}
                                               {:id "124" :type 'x :width 26 :height 20}]}]}))



(defn app []
  [:div
   [flowgraph @app-state 1500 500]])

(defn init []
  (r/render [app]
            (.getElementById js/document "app"))
  (.log js/console "Done!!!!!")) 


#_(swap! app-state assoc-in ["1" :label] "baaaaaaa")
