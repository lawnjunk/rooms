(ns server.machine.flatten-action
  (:require [common.machine :as machine]))

(defn- flatten-action
  [request]
  (merge request (:data request)))

(defn make 
  "flatten (request.data) onto the request"
  []
  (machine/make {:in 100
                 :out 100
                 :threads 4
                 :transducer (map flatten-action)}))
