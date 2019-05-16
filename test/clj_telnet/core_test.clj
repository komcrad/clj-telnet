(ns clj-telnet.core-test
  (:require [clojure.test :refer :all]
            [clj-telnet.core :refer :all]))

(deftest get-telnet-test
  (testing "get-telnet"
    (is (instance? org.apache.commons.net.telnet.TelnetClient
           (get-telnet "telehack.com")))
    (is (instance? org.apache.commons.net.telnet.TelnetClient
                   (get-telnet "telehack.com" 23)))
    (is (thrown? java.net.SocketTimeoutException (get-telnet "1.2.3.4")))
    (is (thrown? java.net.SocketTimeoutException (get-telnet "1.2.3.4" 23)))
    (is (thrown? java.net.SocketTimeoutException (get-telnet "1.2.3.4" 23 :connection-time 3000)))))

(deftest read-until-or-test
  (testing "read-until-or-test"
    (let [telnet (get-telnet "telehack.com" 23)]
      (is (.contains (read-until-or telnet ["not a line" "zrun"])
                     "May the command line live forever."))
      (write telnet "echo hello world")
      (is (.contains (read-until-or telnet ["fake line" "not a line" "ld\r\nhello world\r\n."])
                     "echo hello world\r\nhello world\r\n."))
      (is (= "" (read-until-or telnet ["hello there"] 3000)))
      (kill-telnet telnet))
    (let [telnet (get-telnet "telehack.com" 23)]
      (is (.contains (read-until-or telnet [#"not a line" #"md\d "])
                     "May the command line live forever."))
      (kill-telnet telnet))))

(deftest read-until-test
  (testing "read-util"
    (let [telnet (get-telnet "telehack.com" 23)]
      (is (.contains (read-until telnet "zrun")
                     "May the command line live forever."))
      (kill-telnet telnet))
    (let [telnet (get-telnet "telehack.com" 23)]
      (is (not (clojure.string/ends-with? (read-until telnet "May the command line live forever")
                                          "May the command line live forever ")))
      (kill-telnet telnet))
    (let [telnet (get-telnet "telehack.com" 23)]
      (is (.contains (read-until telnet "2048")
                     "2048"))
      (kill-telnet telnet))))

(deftest read-all-test
  (testing "read-all"
    (let [telnet (get-telnet "telehack.com")]
      (is (.contains (read-all telnet) "Type HELP for a detailed command list"))
      (kill-telnet telnet))))

(deftest write-test
  (testing "write"
    (let [telnet (get-telnet "telehack.com" 23)]
      (read-until telnet "zrun")
      (write telnet "echo hello world")
      (is (.contains (read-until telnet "hello world") "hello world")))))

(deftest kill-telnet-test
  (testing "kill-telnet"
    (let [telnet (get-telnet "telehack.com")]
      (is (= nil (kill-telnet telnet)))
      (is (thrown? java.lang.NullPointerException (kill-telnet telnet))))))

(deftest close-test
  (testing "close-test"
    (is (.contains (with-open [telnet (get-telnet "telehack.com")]
                     (read-until telnet "zrun")) "zrun"))
    (is (.contains (with-open [telnet (get-telnet "telehack.com" 23)]
                     (read-until telnet "zrun")) "zrun"))))
