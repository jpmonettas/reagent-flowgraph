(ns cljs-flowgraph.core
  (:require [reagent.core :as r]))


(defonce nodes (r/atom {1 {:id 1 :x 0 :y 0 :width 0 :height 0 :label "Holaaaaaaaaaa"}
                    2 {:id 2 :x 0 :y 0 :width 0 :height 0 :label "Test"}
                    3 {:id 3 :x 0 :y 0 :width 0 :height 0 :label "Blaaaaaaaaaaaaaaaaaaa"}}))

(defn check-and-re-draw [node-component]
  (let [dn (r/dom-node node-component)
        {:keys [id width height]} (r/props node-component)
        dom-width (.-offsetWidth dn)
        dom-height (.-offsetHeight dn)]
    (if (or (not= width dom-width)
            (not= height dom-height))
      (swap! nodes update id #(assoc % :width dom-width :height dom-height)))))

(defn node [{:keys [id x y width height label]}]
  (r/create-class
   {:component-did-mount (fn [this] (r/after-render #(check-and-re-draw this)))
    :component-will-receive-props (fn [this _] (r/after-render #(check-and-re-draw this)))
    :reagent-render (fn [{:keys [id x y width height label]}]
                      [:div
                       {:style {:position :absolute
                                :top y :left width}}
                       label])}))

(defn main []
  [:div#panel 
   (for [n (vals @nodes)]
     ^{:key (:id n)} [node n])])

(defn init []
  (r/render [main]
            (.getElementById js/document "app"))
  (.log js/console "Done!!!!!")) 


