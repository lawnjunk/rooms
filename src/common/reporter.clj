(ns common.reporter
  (:require [clojure.core.async 
             :refer [go-loop alt! chan put!]]))


(def loge-channel (chan 100))
(def log-channel (chan 100))

(defn log 
  [& args]
  (put! log-channel args))

(defn log-err
  [& args]
  (put! loge-channel args))

(go-loop []
         (alt! 
           log-channel ([args] (apply println args))
           loge-channel ([args] 
                         (binding [*out* *err*]
                           (apply println args))))
         (recur))

