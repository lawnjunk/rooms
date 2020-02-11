(ns client.core 
  (:require [common.machine :as machine
             :refer [take!! put!!]]
            [common.reporter :refer [log]]
            [clojure.core.async :refer [go <!]])
  (:gen-class))

(defn -main
  "THE CLIENT"
  [& args]
  (log "STARTING THE CLIENT ON PORT BLA BLA"))
