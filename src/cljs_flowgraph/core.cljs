(ns cljs-flowgraph.core
  (:require [reagent.core :as r]))


(defn main []
  [:div "Great"])

(defn init []
  (r/render [main]
            (.getElementById js/document "app"))
  (.log js/console "Done!!!!!")) 


