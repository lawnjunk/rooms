(ns rooms.player
  (:require [clojure.spec.alpha :as s]))

(s/def ::name string?)
(s/def ::hp (s/and int? #(> % -1) #(< % 101)))
; TODO make a item spec
(s/def ::items (s/map-of keyword? map?))
(s/def ::player (s/keys :req-un [::name ::hp ::items]))
(def assert-player #(s/assert ::player %))

(defn make-player
  "create a player"
  [name]
  (assert-player {:name name
                  :items {}
                  :hp 100}))

(defn- health-limiter
  [num]
  (cond  
    (< num 0) 0
    (> num 100) 100
    :else num))

(defn sub-health 
  "subtract health from a player"
  [player num]
  (assert-player 
    (update player :hp #(health-limiter (- % num)))))

(defn add-health 
  "subtract health from a player"
  [player num]
  (assert-player 
    (update player :hp #(health-limiter (+ % num)))))
