(ns client.client
  (:require [clojure.spec.alpha :as s]
            [common.util :as util]
            [clojure.java.io :as jio]
            [common.io :as io])
  (:import (java.net Socket InetAddress)))

; spec
(s/def ::uuid uuid?)
(s/def ::type #{:client})
(s/def ::port (util/reference? util/atom? (util/or? nil? (util/and? int? pos?))))
(s/def ::hostname (util/reference? util/atom? (util/or? string? nil?)))
(s/def ::connected (util/reference? util/ref? boolean?))
(s/def ::socket (util/reference? util/ref? (util/or? nil? util/socket?)))
(s/def ::reader (util/reference? util/ref? (util/or? nil? util/reader?)))
(s/def ::writer (util/reference? util/ref? (util/or? nil? util/writer?)))
(s/def ::client (s/and record? 
                       (s/keys :req-un [::uuid 
                                        ::type
                                        ::port 
                                        ::hostname
                                        ::connected
                                        ::socket
                                        ::reader
                                        ::writer])))

(def assert-client #(s/assert ::client %) )

(defprotocol ClientProtocol
  (set-hostname [this hostname])
  (set-port [this port])
  (connect [this])
  (disconnect! [this]))

(defn -set-hostname
  [client hostname]
  (s/assert string? hostname)
  (reset! (:hostname client) hostname)
  client)

(defn -set-port
  [client port]
  (s/assert (util/and? int? pos?) port)
  (reset! (:port client) port)
  (assert-client client))

(defn -connect 
  "connect a client"
  [client]
  (let [{:keys [hostname port]} client
        socket (Socket. @hostname @port)
        reader (jio/reader socket)
        writer (jio/writer socket)]
    (dosync 
      (ref-set (:socket client) socket)
      (ref-set (:reader client) reader)
      (ref-set (:writer client) writer)
      (ref-set (:connected client) true)))
  (assert-client client))

(defn -disconnect!
  "disconnect a client"
  [client]
  (let [{:keys [socket reader writer]} client]
    (map #(.close %) [reader writer socket])
    (dosync 
      (ref-set (:socket client) nil)
      (ref-set (:reader client) nil)
      (ref-set (:writer client) nil)
      (ref-set (:connected client) false)))
  (assert-client client))

(defrecord Client 
  [socket reader writer type uuid port hostname connected]
  io/InputOutput
    (>text [this text] (io/-write @(:writer this) text))
    (>data [this data] (io/-write-data @(:writer this) data))
    (<text  [this] (io/-read-line @(:reader this)))
    (<data  [this] (io/-read-data @(:reader this)))
  ClientProtocol
    (connect [this] (-connect this))
    (disconnect! [this] (-disconnect! this))
    (set-port [this port] (-set-port this port))
    (set-hostname [this hostname] (-set-hostname this hostname)))

(defn make 
  "Create a client"
  ([]
  (make nil nil))
  ([hostname port]
  (assert-client 
    (map->Client {:uuid (util/make-uuid)
                  :type :client
                  :port (atom port)
                  :hostname (atom hostname)
                  :connected (ref false)
                  :socket (ref nil)
                  :reader (ref nil)
                  :writer (ref nil)}))))
