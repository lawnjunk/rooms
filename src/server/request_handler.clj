(ns server.request-handler
  (:require [common.machine :as machine]
            [common.util :as util]
            ))

(defn- request-has-action?
  [request]
  (not (nil? (get-in request [:data :action]))))

(defn- request-has-error? 
  [request]
  (= :error (:type request)))

(defn- request-valid?
  "FILTER OUT VALID ACTIONS and ERRORS"
  [request]
  ((util/or? request-has-action? request-has-error?) request))

(defn- flatten-action
  [request]
  (-> request
      (dissoc :data)
      (merge (:data request))))

(def ^:private handle-request
  (comp 
    (filter request-valid?)
    (map flatten-action)))

(defn make
  "create a request-handler machine"
  []
  (machine/make {:in 100
                 :out 100
                 :threads 10
                 :transducer handle-request}))

