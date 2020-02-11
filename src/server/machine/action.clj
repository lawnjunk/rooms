(ns server.machine.action
  (:require [common.machine :as machine]))

(defn- handle-disconnect 
  [request]
  (let [{:keys [server connection]} request]
    (.>text connection "BYE BYE!")
    (.disconnect! server connection))
  request)

(def disconnect-machine 
  (machine/make {:in 10
                 :out 10
                 :threads 1
                 :transducer (map handle-disconnect)
                 }))

(defn- handle-echo 
  [request]
  (let [{:keys [connection data]} request]
    (.>text connection (println-str data)))
  request)

(def echo-machine 
  (machine/make {:in 10
                 :out 10
                 :threads 1
                 :transducer (map handle-echo)}))
