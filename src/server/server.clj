(ns server.server
  (:require [common.util :as util] 
            [clojure.spec.alpha :as s]
            [server.connection :as c]
            [common.machine :as machine]
            [common.reporter :refer [log log-err]]
            [server.request-handler :as request-handler]
            [server.read-request :refer [read-request]]
            [clojure.core.async :refer [go-loop >! <! pub mult tap chan]])
  (:import (java.net ServerSocket)))

(defn- assert-server 
  [server]
  ;(s/assert ::server server)
  server)

(defn- -set-port
  "set the server port"
  [server port]
  (s/assert (util/or? nil? (util/and? int? (partial < 999))) port)
  (dosync (ref-set (:port server) port))
  server)

(defn- -set-handler
  "set the server handler"
  [server handler]
  (s/assert (util/or? nil? fn?) handler)
  (dosync (ref-set (:handler server) handler))
  server)

(defn- -closed? 
  [server]
  @(:closed server))

(def ^:private -running? (complement -closed?))

(defn- -accept-socket 
  "accept the next socket"
  [server]
  (.accept @(:server-socket server)))

(defn -store-connection
  [server connection]
  (let [connections (:connections server)
        uuid (:uuid connection)]
    (dosync 
      (alter connections assoc uuid connection)))
  connection)

(defn- -accept-connection 
  "accept incoming socket -> make it into a connection"
  [server]
  (-> server 
    (-accept-socket) 
    (c/make)
    ((partial -store-connection server))))

(defn- -count-connections 
  "count the server connections"
  [server]
  (count @(:connections server)))

(defn- -disconnect! 
  "close a connection"
  [server connection]
  (let [connections (:connections server)
        uuid (:uuid connection)]
    (log "_DISCONNECT_" uuid)
    (try 
      (.close-connection! connection))
    (dosync 
      (alter connections dissoc uuid)))
  server)

; TODO FIX
(defn- -disconnect-all! 
  "disconnect all connections"
  [server]
  (let [connections (vals @(:connections server))]
    (run! (partial -disconnect! server) connections)
    server))

(defn- handle-connections 
  [server]
  (go-loop [] 
    (when (.running? server)
      (try 
        (let [connection (.accept-connection server)]
          (log "_NEW_CONNECTION_" (:uuid connection))
          (read-request server connection (:request-handler-machine server)))
        (catch Exception e
          (log-err "FAILED TO ACCEPT CONNECTION" e)))
      (recur))))

(defn- -start
  "start the server"
  [server]
  (let [{:keys [server-socket port closed]} server]
    (dosync 
      (ref-set server-socket (ServerSocket. @port))
      (ref-set closed false)))
  (handle-connections server)
  server)

(defn- -stop!
  "stop the server"
  [server]
  (.disconnect-all! server)
  (let [{:keys [server-socket closed]} server]
    (.close @server-socket)
    (dosync 
      (ref-set server-socket nil)
      (ref-set closed true)))
  server)

(defprotocol ServerProtocol
  (set-port [this port])
  (closed? [this])
  (running? [this])
  (accept-connection [this])
  (count-connections [this])
  (disconnect! [this connection])
  (disconnect-all! [this])
  (start [this])
  (stop! [this]))

(defrecord Server 
  [uuid type port server-socket connections closed]
  ServerProtocol
  (set-port [this port] (-set-port this port))
  (closed? [this] (-closed? this))
  (running? [this] (-running? this))
  (accept-connection [this] (-accept-connection this))
  (count-connections [this] (-count-connections this))
  (disconnect! [this connection] (-disconnect! this connection))
  (disconnect-all! [this] (-disconnect-all! this))
  (start [this] (-start this))
  (stop! [this] (-stop! this)))

(defn make
  "create a server on a given port"
  ([]
    (make nil))
  ([port]
    [make port]
    (s/assert (util/or? nil? (util/and? int? (partial < 999))) port)
    (let [request-handler-machine (request-handler/make)
          request-mult (mult (:out request-handler-machine))
          action-tap (tap request-mult  (chan 100))
          action-pub (pub action-tap :action)]
        (map->Server {:uuid (util/make-uuid)
                      :type :server
                      :port (atom port) 
                      :server-socket (ref nil)
                      :connections (ref {})
                      :request-handler-machine request-handler-machine
                      :request-mult request-mult
                      :action-pub action-pub
                      :closed (ref true)}))))

