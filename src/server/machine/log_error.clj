(ns server.machine.log-error
  (:require [common.machine :as machine]))

(defn- log-error 
  [request]
  (let [{:keys [error]} request]
      (binding [*out* *err*]
        (println "_ERROR_" (.getMessage error)))
    request))

(defn make
  []
  (machine/make {:in 50
                 :out 10
                 :threads 2
                 :transducer (map log-error)}))
