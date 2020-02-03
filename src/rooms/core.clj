(ns rooms.core
  (:require [clojure.pprint :refer [pprint]]
            [client.client :as client]
            [server.server :as server]
            [common.io :as io]
            [clojure.java.io :as jio]) 

  (:gen-class))


(def s (server/make 8888))
(def c (client/make "localhost" 8888))


;(defn echo-action 
  ;[serv cone mess]
  ;(con/prn-write mess))

;(defn server-router
  ;[server connection message]
  ;(let [{:keys [action data]} message]
    ;(case action 
      ;(:echo (echo-action server connection message))
      ;(:fail (println "_ERRROR_" (prn-str message)))
      ;(println "UNKNOWN ACTION" action))))

;(defn get-message 
  ;[server connection] 
  ;(try 
    ;(con/edn-readline connection)
    ;(catch Exception e {:action :fail :data e})))

;(defn server-handler 
  ;[server connection]
  ;(println "new connection")
  ;(con/prn-write connection {:action :greet :data "HELLO WORLD"})
  ;(while (s/running? server)
    ;(let [message (get-message server connection)]
      ;(server-router server connection message))))

;(def server (s/start (s/make-server 8883 server-handler)))

;(def client (client/connect (client/make-client "localhost" 8883)))

;(con/prn-write client {:action :echo :data "GOOD BYE WORLD"})


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (.start s)
  (.connect c)
  (.store-connection s (.accept-connection s))
  (pprint s)
  (pprint c)
  (Thread/sleep 1000)
  (let [connection (first (vals  @(:connections s))) ]
    (println "CONNECTIOn" (:uuid connection))
    (future
      (println "MESSAGE" (.<text c)))
    (.>text connection "SUPP BOIII?")
    (println "wat")
    )
  (println "HAHAH" (.count-connections s))
  (println "Hello, World!")
  (.stop! s)
  (shutdown-agents)
  )
