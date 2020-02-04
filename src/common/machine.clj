; create STATE MACHINES from two channals
; probbably a mistake and redundent
(ns common.machine
  (:require [clojure.core.async :as as
             :refer [>! <! put! take! chan]]))

(defn- -machine? 
  "is this a machine?"
  [m] 
  (try (= (:type m) :machine)
       (catch Exception e false)))

(defn- -close!
  [this]
  (as/close! (:in this))
  (as/close! (:out this)))

(defn -m-out-or-chan
  [m]
  (if (-machine? m) (:out m) m))

(defn -m-in-or-chan
  [m]
  (if (-machine? m) (:in m) m))

(defn- ->>! 
  ">! to the input"
  [m value]
  (>! (:in m) value))

(defn- -<<! 
  "<! from the output"
  [m value]
  (<! (:out m)))

(defn- -put!!
  "put! to the input"
  [m value]
  (put! (:in m) value))

(defn- -take!!
  "take! from the output"
  [m f]
  (take! (:out m) f))

(defn- -make-mult
  "create a mult from the output"
  [m]
  (as/mult (:out m)))

(defn -merge-map 
  [m with ]
  (conj (map -m-out-or-chan with) (:out m)))

(defn -make-merge 
  "create a merge chan from the output of machines and/or chans"
  ([m with]
    (as/merge (-merge-map m with)))
  ([m with buf-or-n]
   (as/merge (-merge-map m with) buf-or-n)))

(defn- -make-pub 
  "create a pub from the output"
  ([m f]
   (as/pub (:out m) f))
  ([m f b]
   (as/pub (:out m) f b)))

(defn- -pipe-to
  "pipe to a machine or chan"
  ([this m-or-c]
    (as/pipe (:out this) (-m-in-or-chan m-or-c)))
  ([this m-or-c close?]
    (as/pipe (:out this) (-m-in-or-chan m-or-c) close?)))

(defn- -pipe-from 
  "pipe from a machine or chan"
  ([this m-or-c]
    (as/pipe (-m-out-or-chan m-or-c) (:in this)))
  ([this m-or-c close?]
    (as/pipe (-m-out-or-chan m-or-c) (:in this) close?)))

(defn- -tap-to 
  "tap to a mult"
  ([this mult]
   (as/tap mult (:in this)))
  ([this mult close?]
   (as/tap mult (:in this) close?)))

(defn- -sub-to
  "sub to a publisher"
  ([this pub topic]
   (as/sub pub topic (:in this)))
  ([this pub topic close?]
   (as/sub pub topic (:in this) close?)))

(defprotocol MachineProtocol
  (machine? [this])
  (close! [this])
  (>>! [this value])
  (<<! [this])
  (put!! [this value])
  (take!! [this f])
  (make-mult [this])
  (make-merge [this with] [this with buf-or-n])
  (make-pub [this f] [this f b])
  (pipe-to [this m-or-c])
  (pipe-from [this m-or-c])
  (tap-to [this mult] [this mult close?])
  (sub-to [this pub topic] [this pub topic close?]))

; TODO SIMPLIFY THIS WITH A DAM MACRO
(defmacro add-method
  [name args]
  (let [title# (name)
        f# (symbol (str \- title#))]
    `(~title# ~args (~f# ~@args))))


(defrecord Machine [type in out]
  MachineProtocol
  (machine? [this] (-machine? this))
  (close! [this] (-close! this))
  (>>! [this value] (->>! this value))
  (<<! [this] (-<<! this))
  (put!! [this value] (-put!! this value))
  (take!! [this f] (-take!! this f))
  (make-mult [this] (-make-mult this))
  (make-merge [this with] (-make-merge this with))
  (make-merge [this with buf-or-n] (-make-merge this with buf-or-n))
  (make-pub [this f] (-make-pub this f))
  (make-pub [this f b] (-make-pub this f b))
  (pipe-to [this m-or-c] (-pipe-to this m-or-c))
  (pipe-from [this m-or-c] (-pipe-from this m-or-c))
  (tap-to [this mult] (-tap-to this mult))
  (tap-to [this mult close?] (-tap-to this mult close?))
  (sub-to [this pub topic] (-sub-to this pub topic))
  (sub-to [this pub topic close?] (-sub-to this pub topic close?)))

;TODO DESIGN BETTER MAKE FUNCTIONS
(defn- -make 
  [in out]
  (map->Machine 
    {:type :machine
     :in (chan in)
     :out (chan out)
     }))

(defmacro make
  ([conf]
   `(make ~conf ~['in 'out]))
  ([conf args & body]
  `(let [result# (~-make ~@conf)
         in# (:in result#)
         out# (:out result#)]
     ((fn ~args ~@body) in# out#)
       result#)))

(defn make-with-transducer
  [config]
  (let [{:keys [in out threads transducer]} config]
    (make 
      [in out] 
      [in# out#] 
      (as/pipeline threads out# transducer in#))))
