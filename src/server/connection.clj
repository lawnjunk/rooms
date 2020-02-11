(ns server.connection
  (:require [common.util :as util] 
            [common.io :as io]
            [clojure.java.io :as jio]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]))

(s/def ::uuid uuid?)
(s/def ::type #{:connection})
(s/def ::socket util/socket?)
(s/def ::reader util/reader?)
(s/def ::writer util/writer?)
(s/def ::closed (util/reference? util/atom? boolean?
s/def ::connection 
  s/keys :req-un [::uuid ::type ::socket ::reader ::writer ::closed ]))
(def assert-connection #(s/assert ::connection %))

(defn- close-reader [reader] (.close reader))
(defn- close-writer [writer] (doto writer .flush .close))
(def ^:private close-socket close-reader)

(defprotocol ConnectionProtocol
  (close-connection! [this])
  (closed? [this])
  (open? [this]))

(defn -close-connection! 
  "close a connetion"
  [connection]
  (when (.open? connection)
    (close-socket (:socket connection))
    (close-reader (:reader connection))
    (close-writer (:writer connection))
    (reset! (:closed connection) true))
  connection)

(defn -closed? 
  "has close-connection! ben run?"
  [connection]
  (or @(:closed connection) (.isClosed (:socket connection))))

(def -open? (complement -closed?))

(defrecord Connection [uuid type socket reader writer closed]
  io/InputOutput
    (>text [this text] (io/-write (:writer this) text))
    (>data [this text] (io/-write-data (:writer this) text))
    (<text [this] (io/-read-line (:reader this)))
    (<data [this] (io/-read-data (:reader this)))
  ConnectionProtocol
    (close-connection! [this] (-close-connection! this))
    (closed? [this] (-closed? this))
    (open? [this] (-open? this))
    )

(defn make 
  "creates a connection from a socket"
  [socket]
  (s/assert util/socket? socket)
  (assert-connection 
    (map->Connection {:uuid (util/make-uuid)
                      :type :connection
                      :socket socket
                      :reader (jio/reader socket)
                      :writer (jio/writer socket)
                      :closed (atom false)})))

