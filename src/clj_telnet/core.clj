(ns clj-telnet.core
  (:gen-class)
  (:import
    [org.apache.commons.net.telnet TelnetClient]
    [java.net InetSocketAddress Socket]
    [java.io PrintStream PrintWriter]
    [clojure.lang PersistentVector])
  (:require [clj-telnet.wait :refer [wait-for]]))

(defn get-telnet
  "returns a telnetclient given server-ip as String and port as int"
  ([^String server-ip ^Integer port]
   ;test if server will connect on port
   (let [s (new java.net.Socket)]
     (. s connect (new java.net.InetSocketAddress server-ip port) 5000)
     (.close s))
  (let [tc (TelnetClient.)]
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
  "reads the input stream of a telnet client till it finds pattern"
  [^TelnetClient telnet ^PersistentVector patterns]
  (let [in (.getInputStream telnet)]
    (loop [result ""]
      (let [s (char (.read in))]
        (if (some #(= s (last %)) patterns)
          (if (some #(clojure.string/ends-with? (str result s) %) patterns)
            (str result s)
            (recur (str result s)))
          (recur (str result s)))))))

(defn read-until
  "reads the input stream of a telnet client till it finds pattern"
  [^TelnetClient telnet ^String pattern]
  (read-until-or telnet [pattern]))

(defn read-all
  [^TelnetClient telnet]
  "Attempts to read all the data from the telnet stream.
   Should probably only be used in repl"
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
