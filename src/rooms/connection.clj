(ns rooms.connection
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s])
  (:import (java.net Socket InetAddress)
           (java.io BufferedReader BufferedWriter)))

(defn socket? [x] (instance? Socket x))
(defn inet-address? [x] (instance? InetAddress x))
(defn reader? [x] (instance? BufferedReader x))
(defn writer? [x] (instance? BufferedWriter x))

(s/def ::socket socket?)
(s/def ::inet-address inet-address?)
(s/def ::reader reader?)
(s/def ::writer writer?)
(s/def ::closed boolean?)
(s/def ::connection 
  (s/keys :req-un [::socket ::inet-address ::reader ::writer ::closed]))
(def assert-connection #(s/assert ::connection %))

(defn make-connection
  "createa a connection from a socket"
  [socket]
  (assert-connection {:socket socket
                      :inet-address (.getInetAddress socket)
                      :reader (io/reader socket)
                      :writer (io/writer socket)
                      :closed false
                      }))

(defn- close-reader [reader] (.close reader))
(defn- close-writer [writer] (doto writer .flush .close))
(def close-socket close-reader)

(defn close-connection! 
  "close a connetion"
  [connection]
  (close-reader (:reader connection))
  (close-writer (:writer connection))
  (close-socket (:socket connection))
  (assert-connection (assoc connection :closed true)))

(defn connected? 
  "is the socket connected?"
  [connection]
  (.isConnected (:socket connection)))

(defn closed? 
  "has close-connection! ben run?"
  [connection]
  (:closed connection))
