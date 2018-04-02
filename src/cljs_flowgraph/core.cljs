(ns cljs-flowgraph.core
  (:require [reagent.core :as r]
            [clj-tree-layout.core :refer [layout-tree]]
            [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn break]]))

(defonce comp-hiccup {}
  #_{'let (fn [d]
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


(defn assoc-sizes [n sizes-map]
  (let [[width height] (get sizes-map (-> n :data :id))]
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
        nodes-with-sizes (layout-tree (assoc-sizes @nodes-a dom-sizes)
                                      {:data-fn :data
                                       :children-fn :childs
                                       :width-fn :width
                                       :height-fn :height})]
    (.log js/console "Nodes recalculated " nodes-with-sizes)
    (reset! nodes-a nodes-with-sizes)))

(defn node [{:keys [data x y width height] :as n}]
  [:div
   {:id (:id data)
    :style {:position :absolute
            :top y :left x
            :border "1px solid black"
            :padding "10px"
            :border-radius "10px"}}
   ((get comp-hiccup (:type data) (constantly [:div (:id data)])) data)])

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
                                     [(str (-> n :data :id) (-> cn :data :id))
                                      (+ (:x n) (/ (:width n) 2))
                                      (+ (:y n) (:height n))
                                      (+ (:x cn) (/ (:width cn) 2)) 
                                      (:y cn)])]
                        [:div#panel {:style {:position :absolute
                                             :top 150
                                             :width width
                                             :height height}}

                         ;; Links
                         [:svg {:width width
                                :height height}
                          (for [[lid x1 y1 x2 y2] links]
                            ^{:key (str "L" lid)}
                            [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2
                                    :style {:stroke :red
                                            :stroke-width 2}}])]

                         ;; Nodes
                         (for [n nodes-seq]
                           ^{:key (->> n :data :id (str "N"))} [node n])]))})))
 
(defonce app-state (r/atom {:data {:id "1" :type 'let}
                            :childs [{:data {:id "11" :type 'map} 
                                      :childs [{:data {:id "111" :type 'x}}
                                               {:data {:id "112" :type 'x}}
                                               {:data {:id "113" :type 'x}}
                                               {:data {:id "114" :type 'x}}]}
                                     {:data {:id "12" :type '+}
                                      :childs [{:data {:id "121" :type 'x} 
                                                :childs [{:data {:id "1211" :type 'x} }]}
                                               {:data {:id "122" :type 'x}}
                                               {:data {:id "123" :type 'x}}
                                               {:data {:id "124" :type 'x}}]}]}))



(defn app []
  [:div
   [flowgraph @app-state 1500 500]])

(defn init []
  (r/render [app]
            (.getElementById js/document "app"))
  (.log js/console "Done!!!!!")) 


#_(swap! app-state assoc-in ["1" :label] "baaaaaaa")
