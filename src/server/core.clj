(ns server.core
  (:require [server.server :as server]
            [server.machine.action :as action]
            [common.reporter :refer [log]]
            [clojure.core.async
             :refer [<! >! take! put! go go-loop pub sub chan tap]])
  (:gen-class))

; STATE
(def PORT 8888)
(def serv (server/make PORT))
(def action-pub (:action-pub serv))
(def log-chan (tap (:request-mult serv) (chan 100)))

; log all of the requests
(go-loop [] 
         (let [{:keys [action data]} (<! log-chan)]
           (clojure.pprint/pprint {:action action :data data}))
         (if (.running? serv) (recur)))

;action handlers
(sub action-pub :disconnect (:in action/disconnect-machine))
(sub action-pub :echo (:in action/echo-machine))


; TODO MOVE TO SERVER?

(defn -main
  "THE SERVER"
  [& args]
  (.start serv)
  (log "STARTING THE SERVER ON PORT" PORT)
  (loop [] 
    (let [input (.trim (read-line))]
      (if (= input "quit")
        (.stop! serv)))
    (if (.running? serv) (recur))))


