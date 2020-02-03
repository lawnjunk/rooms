(ns server.server
  (:require [common.util :as util] 
            [clojure.spec.alpha :as s]
            [server.connection :as c])
  (:import (java.net ServerSocket)))

; spec
(s/def ::uuid uuid?)
(s/def ::type #{:server})
(s/def ::port (util/reference? util/atom? (util/or? nil? (util/and? int? (partial < 999)))))
(s/def ::handler (util/reference? util/atom? (util/or? nil? fn?)))
(s/def ::server-socket (util/reference? util/ref? (util/or? nil? util/server-socket?)))
(s/def ::connections (util/reference? util/ref? map?))
(s/def ::closed (util/reference? util/ref? boolean?))
(s/def ::server 
  (s/keys :req-un [::uuid 
                   ::type
                   ::port
                   ::handler
                   ::server-socket 
                   ::connections 
                   ::closed 
                   ]))
(def ^:private assert-server #(s/assert ::server %))

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
  (and (:closed server) (.isClosed @(:server-socket server))))

(def ^:private -running? (complement -closed?))

(defn- -accept-socket 
  "accept the next socket"
  [server]
  (.accept @(:server-socket server)))

(defn- -accept-connection 
  "accept incoming socket -> make it into a connection"
  [server]
  (-> server
      (-accept-socket)
      (c/make)))

(defn- -store-connection
  [server connection]
  (let [connections (:connections server)
        uuid (:uuid connection)]
    (dosync 
      (alter connections assoc uuid connection)))
  server)

(defn- -count-connections 
  "count the server connections"
  [server]
  (count @(:connections server)))

(defn- -disconnect! 
  "close a connection"
  [server connection]
  (let [connections (:connections server)
        uuid (:uuid connection)]
    (println (str "_DISCONNECT_ " uuid))
    (.close-connection! connection)
    (dosync 
      (alter connections dissoc uuid)))
  server)

(defn- -disconnect-all! 
  "disconnect all connections"
  [server]
  (let [connections (vals @(:connections server))]
    (reduce -disconnect! connections)))

(defn- -start
  "start the server"
  [server]
  (let [{:keys [server-socket port closed]} server]
    (dosync 
      (ref-set server-socket (ServerSocket. @port))
      (ref-set closed false)))
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
  (set-handler [this handler])
  (closed? [this])
  (running? [this])
  (accept-connection [this])
  (store-connection [this connection])
  (count-connections [this])
  (disconnect! [this connection])
  (disconnect-all! [this])
  (start [this])
  (stop! [this]))


(defrecord Server 
  [uuid type port handler server-socket connections closed]
  ServerProtocol
  (set-port [this port] (-set-port this port))
  (set-handler [this handler] (-set-handler this handler))
  (closed? [this] (-closed? this))
  (running? [this] (-running? this))
  (accept-connection [this] (-accept-connection this))
  (store-connection [this connection] (-store-connection this connection))
  (count-connections [this] (-count-connections this))
  (disconnect! [this connection] (-disconnect! this connection))
  (disconnect-all! [this] (-disconnect-all! this))
  (start [this] (-start this))
  (stop! [this] (-stop! this)))

(defn make
  "create a server on a given port"
  ([]
    (make nil nil))
  ([port]
    (make port nil))
  ([port handler]
    (s/assert (util/or? nil? (util/and? int? (partial < 999))) port)
    (s/assert (util/or? nil? fn?) handler)
    (assert-server 
      (map->Server {:uuid (util/make-uuid)
                    :type :server
                    :port (atom port) 
                    :handler (atom handler)
                    :server-socket (ref nil)
                    :connections (ref {})
                    :closed (ref true)}))))

(def serv (make 8888 #(println "COOOL")))



;(defn- handle-connection 
  ;[server connection]
  ;(let [{:keys [connection-handler]} server]
    ;(try 
      ;(connection-handler server connection)
      ;(catch Exception e 
        ;(println (str "_ERROR_ " "handle-connection" (.getMessage e)))))
    ;server))

;(defn- log-connection 
  ;[server connection]
  ;(println (str "_NEW_CONNECTION_" (:uuid connection)))
  ;server)

;(defn- future-connect 
  ;"begins the accept connection future"
  ;[server]
  ;(assoc server :future-connect 
         ;(future 
           ;(while (running? server)
             ;(let [connection (accept-connection server)]
               ;(-> server 
                   ;(.store-connection connection)
                   ;(handle-connection connection)
                   ;(log-connection connection)
                   ;assert-server))))))



;; TODO: implament a call and response protocal to determine if the pipe is broken
;(defn- future-disconnect
  ;"check for closed connections and remove them from :connections"
  ;[server]
  ;(assoc server :future-disconnect 
         ;(future 
           ;(while (running? server)
              ;(let [connections (:connections server)]
                ;(for [[uuid connection] @connections]
                  ;(assert-server
                    ;(if (c/-closed? connection)
                      ;(-disconnect! server connection)
                      ;server))))))))


