(ns common.io
  (:require [clojure.java.io :as jio]
            [clojure.edn :as edn]
            )
  (:import (java.net Socket)
           (java.io BufferedReader BufferedWriter PushbackReader)))

(defprotocol InputOutput
  (>text [this text])
  (>data [this text])
  (<text [this])
  (<data [this]))

(defn -write
  [writer text]
    (.write writer (println-str text))
    (.flush writer))

(defn -write-data
  [writer data]
  (-write writer (prn-str data)))

(defn -read-line 
  [reader] 
  (binding [*in* reader]
    (read-line)))

(defn -read-data
  [reader]
  (edn/read (PushbackReader. reader)))

