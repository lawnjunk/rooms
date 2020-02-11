(ns server.read-request
  (:require [clojure.core.async :refer [go-loop]]))

(defn read-request
  "reads edn data from a connection and writes it into a 
  request processor machine"
  [server connection processor]
  (go-loop []
    (try 
      (.put!! processor {:server server
                         :connection connection
                         :data (.<data connection)
                         :success true
                         :type :request
                         })
      (catch java.io.IOException e 
          (.disconnect! server connection))
      (catch Exception e
        (if (=  (.getMessage e) "EOF while reading" )
          (.disconnect! server connection)
          (.put!! processor {:server server
                             :connection connection
                             :data {:action :error :data e}
                             :success false
                             :type :error
                             }))))
    (if (and (.open? connection)
             (.running? server))
      (recur))))
