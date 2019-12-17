(ns clj-telnet.core
  (:gen-class)
  (:import
    [org.apache.commons.net.telnet TelnetClient]
    [java.io PrintStream PrintWriter Closeable]
    [clojure.lang PersistentVector]
    (java.net SocketTimeoutException))
  (:require [clj-telnet.wait :refer [wait-for]]
            [clojure.string :as cs]))

(def ^:dynamic *debug* false)

(defn kill-telnet
  "disconnects telnet-client"
  [^TelnetClient telnet-client]
  (.disconnect telnet-client))

(defn get-telnet
  "returns a telnetclient given server-ip as String and port as int.
  Support options:
  :connet-timeout (default 5000)
  :default-timeout (default 15000)
  Add method close so that the object can be used with with-open."
  ([^String server-ip ^Integer port & {:keys [connect-timeout default-timeout]
                                       :or {connect-timeout 5000 default-timeout 15000}}]
   (let [tc (proxy [TelnetClient Closeable] []
              (close [] (kill-telnet this)))]
     (doto tc
       (.setReaderThread true)
       (.setConnectTimeout connect-timeout)
       (.setDefaultTimeout default-timeout)
       (.connect server-ip port)
       (.setKeepAlive true))))
  ([^String server-ip]
   (get-telnet server-ip 23)))

(defn  print-c-debug [c]
  (when *debug* (print (char c))))

(defn print-data-debug [data]
  (when *debug* (print data)))

(defn- read-a-char
  [in]
  (try
    (let [c (.read in)]
      (print-c-debug c)
      c)
    (catch SocketTimeoutException ste (println "Read error") nil)))

(defn- write-data
  [out data]
  (print-data-debug data)
  (.print out data)
  (.flush out))

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
           (let [s (char (read-a-char in))
                 buf (str result s)]
             (if (some #(condp instance? %1
                          java.lang.String (cs/ends-with? buf %1)
                          java.util.regex.Pattern (re-find %1 buf)
                          :default false) patterns)
               buf
               (recur buf)))
           (do (Thread/sleep 10)
               (recur result)))
         result))))
  ([^TelnetClient telnet ^PersistentVector patterns]
   (read-until-or telnet patterns 0)))

(defn read-until
  "reads the input stream of a telnet client till it finds pattern"
  ([^TelnetClient telnet ^String pattern]
   (read-until-or telnet [pattern]))
  ([^TelnetClient telnet ^String pattern ^long timeout]
   (read-until-or telnet [pattern] timeout)))

(defn read-all
  "Attempts to read all the data from the telnet stream.
  Should probably only be used in repl"
  [^TelnetClient telnet]
  (let [in (.getInputStream telnet)]
    (wait-for 10 1000 (fn [] (> (.available in) 0)))
    (loop [result ""]
      (if (or (> (.available in) 0) (wait-for 10 1000 (fn [] (> (.available in) 0))))
        (recur (str result (char (read-a-char in)))) result))))

(defn write
  "writes to the output stream of a telnet client"
  ([^TelnetClient telnet ^String s cr]
   (let [out (PrintStream. (.getOutputStream telnet))
         data (str s (if cr "\n" ""))]
     (write-data out data)))
  ([^TelnetClient telnet ^String s]
   (write telnet s true)))

(defmacro with-telnet
  "
  "
  [bindings & body]
  `(let ~(subvec bindings 0 2)
     (try ~@body
          (catch Exception e#
            (kill-telnet ~(first bindings))
            (throw e#)))))
