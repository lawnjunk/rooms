(ns server.machine.filter-valid-request
  (:require [common.machine :as machine]
            [common.util :as util]
            [clojure.string :as s]))

(defn- has-action?
  [request]
  (not (nil? (get-in request [:data :action]))))

(defn- has-error? 
  [request]
  (= :error (:type request)))

(defn- valid-request?
  "FILTER OUT VALID ACTIONS and ERRORS"
  [request]
  ((util/or? has-action? has-error?) request))

(defn make
  []
  (machine/make {
                 :in 100
                 :out 100
                 :threads 4
                 :transducer (filter valid-request?)
                 }))
