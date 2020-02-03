(ns common.util
  (:import (clojure.lang Agent Ref Atom) 
           (java.util UUID)
           (java.net ServerSocket Socket InetAddress)
           (java.io BufferedReader BufferedWriter)))

(defn and? 
  [& args]
  (fn [data] (reduce #(and %1 (%2 data)) true args)))

(defn or? 
  [& args]
  (fn [data] (reduce #(or %1 (%2 data)) false args)))

(defn in-deref-is? [predicate]
  (fn [data] (predicate @data)))

(defmacro make-instance-predicate
  [class]
  `(fn [x#] (instance? ~class x#)))
(def ref? (make-instance-predicate Ref))
(def atom? (make-instance-predicate Atom))
(def agent? (make-instance-predicate Agent))
(def socket? (make-instance-predicate Socket))
(def reader? (make-instance-predicate BufferedReader))
(def writer? (make-instance-predicate BufferedReader))
(def inet-address? (make-instance-predicate InetAddress))
(def server-socket? (make-instance-predicate ServerSocket))

(defn reference?
  [type? value?] 
  (fn [data]
    (and (type? data) (value? @data))))

(defn make-uuid [] (UUID/randomUUID))

(defn kill-future
  "If a future is running stop it"
  [f]
  (if-not (or (future-done? f) (future-cancelled? f))
    (future-cancel f)))

(defn str->bytes
  [text]
  (.getBytes text))

(defn prn-bytes
  "prn into a array of bytes"
  [data]
  (str->bytes (prn-str data)))

