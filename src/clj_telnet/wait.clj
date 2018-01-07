(ns clj-telnet.wait
  (:gen-class))

(defn wait-for
  "polls to see if f returns true with interval (milliseconds) and timeout (milliseconds)"
  [interval timeout f]
  (let [start (. System currentTimeMillis)]
    (loop [current-time (. System currentTimeMillis)]
      (if (< (- current-time start) timeout)
        (if (f) true (do (. Thread sleep interval) (recur (. System currentTimeMillis))))
        false))))
