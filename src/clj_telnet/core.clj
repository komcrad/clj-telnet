(ns clj-telnet.core
  (:gen-class)
  (:import
    [org.apache.commons.net.telnet TelnetClient]
    [java.io PrintStream PrintWriter]
    [clojure.lang PersistentVector])
  (:require [clj-telnet.wait :refer [wait-for]]
            [clojure.string :as cs]))

(defn get-telnet
  "returns a telnetclient given server-ip as String and port as int.
  Support options:
  :connet-timeout (default 5000)
  :default-timeout (default 15000)"
  ([^String server-ip ^Integer port & {:keys [connect-timeout default-timeout]
                                       :or {connect-timeout 5000 default-timeout 15000}}]
   (let [tc (TelnetClient.)]
     (.setConnectTimeout tc connect-timeout)
     (.setDefaultTimeout tc default-timeout)
     (.connect tc server-ip port)
     (.setKeepAlive tc true)
     tc))
  ([^String server-ip]
   (get-telnet server-ip 23)))

(defn kill-telnet
  "disconnects telnet-client"
  [^TelnetClient telnet-client]
  (.disconnect telnet-client))

(defn read-until-or
  "reads the input stream of a telnet client until it finds pattern or the timeout
  (milliseconds) is reached returns read data as a string.
  patterns is a vector of Strings of Regexs."
  ([^TelnetClient telnet ^PersistentVector patterns ^long timeout]
   (let [in (.getInputStream telnet)
         start-time (System/currentTimeMillis)]
     (loop [result ""]
       (if (or (= 0 timeout) (< (- (System/currentTimeMillis) start-time) timeout))
         (if (< 0 (.available in))
           (let [s (char (.read in))
                 buf (str result s)]
             (if (some #(condp instance? %1
                          java.lang.String (cs/ends-with? buf %1)
                          java.util.regex.Pattern (re-find %1 buf)
                          :default false) patterns)
               buf
               (recur buf)))
           (do (Thread/sleep 50)
               (recur result)))
         result))))
  ([^TelnetClient telnet ^PersistentVector patterns]
   (read-until-or telnet patterns 0)))

(defn read-until
  "reads the input stream of a telnet client till it finds pattern"
  [^TelnetClient telnet ^String pattern]
  (read-until-or telnet [pattern]))

(defn read-all
  "Attempts to read all the data from the telnet stream.
  Should probably only be used in repl"
  [^TelnetClient telnet]
  (let [in (.getInputStream telnet)]
    (wait-for 10 1000 (fn [] (> (.available in) 0)))
    (loop [result ""]
      (if (or (> (.available in) 0) (wait-for 10 1000 (fn [] (> (.available in) 0))))
        (recur (str result (char (.read in)))) result))))

(defn write
  "writes to the output stream of a telnet client"
  [^TelnetClient telnet ^String s]
  (let [out (PrintStream. (.getOutputStream telnet))]
    (doto out
      (.println s)
      (.flush))))

(defn with-telnet
  [telnet f]
  (try (f telnet)
       (catch Exception e
         (kill-telnet telnet)
         (throw e))))
