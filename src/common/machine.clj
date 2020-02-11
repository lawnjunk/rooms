(ns common.machine
  (:require [clojure.core.async 
             :refer [put! take! close! chan pipeline]]))

(defprotocol MachineProtocol
  (machine? [this] "is it a machine?")
  (close!! [this] "close in and out chans")
  (put!! [this value] "async/put! to the in chan")
  (take!! [this f] "async/take! from the out chan"))

(defrecord Machine [type in out]
  MachineProtocol
  (machine? [this] 
    (= :machine (:type this))) 
  (close!! [this] 
    (close! (:in this))
    (close! (:out this)))
  (put!! [this value] 
    (put! (:in this) value))
  (take!! [this f] 
    (take! (:out this) f)))

(defn- -factory
  [config]
  (let [{:keys [in out]} config]
    (map->Machine 
      {:type :machine
       :in (chan in)
       :out (chan out)
       })))

(defn -make-with-transducer
  [config]
  (let [result (-factory config)
        {:keys [threads transducer]} config
        {:keys [in out]} result]
    (pipeline threads out transducer in)
    result))

(defn make 
  "Create an map with in and out chans, and
  optionaly configure a pipeline that connects them."
  [config]
  (let [{:keys [in out threads transducer]} config]
    (if (nil? transducer)
      (-factory config)
      (-make-with-transducer config))))
