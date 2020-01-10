(ns clj-telnet.core
  (:gen-class)
  (:import
    [org.apache.commons.net.telnet TelnetClient]
    [java.io PrintStream PrintWriter Closeable InputStreamReader]
    [clojure.lang PersistentVector]
    (java.net SocketTimeoutException))
  (:require [clj-telnet.wait :refer [wait-for]]
            [clojure.string :as cs]))

(defn kill-telnet
  "disconnects telnet-client"
  [^TelnetClient telnet-client]
  (.disconnect telnet-client))

;; TODO: Should set an input data buffer, or an SocketTimeoutException exception will be raised
;; when some data not read.
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

(defn ^:dynamic *read-in-char*
  "read-in-char will be called when read a char, so can be binded to a function that can do something such as logging."
  [c])

(defn ^:dynamic *write-out-data*
  "write-out-data will be called when write some data, so can be binded to a function that can do something such as logging."
  [data])

(defn ^:dynamic *read-err*
  "read-err will be called when some SocketTimeoutException exception occours."
  [e] (throw e))

(def ^:dynamic *charset*
  "Charset for read-until, read-until-or, read-all, write"
  "UTF-8")

(defn- read-a-char  [in]
  (try
    (let [c (.read in)]
      (*read-in-char* c)
      c)
    (catch Throwable t (*read-err* t))))

(defn- write-data
  [out data]
  (*write-out-data* data)
  (.print out data)
  (.flush out))

(defn read-until-or
  "reads the input stream of a telnet client until it finds pattern or the timeout
  (milliseconds) is reached returns read data as a string.
  patterns is a vector of Strings of Regexs."
  ([^TelnetClient telnet ^PersistentVector patterns ^long timeout]
   (let [in (-> telnet
                .getInputStream
                (InputStreamReader. *charset*))
         start-time (System/currentTimeMillis)]
     (loop [result ""]
       (if (or (= 0 timeout) (< (- (System/currentTimeMillis) start-time) timeout))
         (if (.ready in)
           (let [c (read-a-char in)
                 s (if c (char c))
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
  (let [in (-> telnet
               .getInputStream
               (InputStreamReader. *charset*))]
    (wait-for 10 1000 (fn [] (.ready in)))
    (loop [result ""]
      (if (or (.ready in) (wait-for 10 1000 (fn [] (.ready in))))
        (recur (str result (char (read-a-char in)))) result))))

(defn write
  "writes to the output stream of a telnet client"
  ([^TelnetClient telnet ^String s cr]
   (let [out (PrintStream. (.getOutputStream telnet) true *charset*)
         data (str s (if cr "\n" ""))]
     (write-data out data)))
  ([^TelnetClient telnet ^String s]
   (write telnet s true)))

; TODO this needs to have bindings like let
(defmacro with-telnet
  ""
  [bindings & body]
  `(let ~(subvec bindings 0 2)
     (try ~@body
          (catch Exception e#
            (kill-telnet ~(first bindings))
            (throw e#)))))
