(ns clj-telnet.core-test
  (:require [clojure.test :refer :all]
            [clj-telnet.core :refer :all]))

(deftest get-telnet-test
  (testing "get-telnet"
    (is (= "class org.apache.commons.net.telnet.TelnetClient"
           (str (type (get-telnet "rainmaker.wunderground.com")))))
    (is (= "class org.apache.commons.net.telnet.TelnetClient"
           (str (type (get-telnet "rainmaker.wunderground.com" 23)))))
    (is (thrown? java.net.SocketTimeoutException (get-telnet "1.2.3.4" 23)))
    (is (thrown? java.net.SocketTimeoutException (get-telnet "1.2.3.4")))))

(deftest read-until-test
  (testing "read-util"
    (let [telnet (get-telnet "rainmaker.wunderground.com" 23)]
      (is (clojure.string/ends-with? (read-until telnet "National Weather Service") 
                                     "National Weather Service"))
      (kill-telnet telnet))
    (let [telnet (get-telnet "rainmaker.wunderground.com" 23)]
      (is (not (clojure.string/ends-with? (read-until telnet "National Weather Service") 
                                     "National Weather Service ")))
      (kill-telnet telnet))
    (let [telnet (get-telnet "192.168.212.216" 23)]
      (read-until telnet "login: ")
      (write telnet "rlcuser")
      (read-until telnet "Password: ")
      (write telnet "920016")
      (read-until telnet "~]$")
      (write telnet "su -")
      (read-until telnet "Password: ")
      (write telnet "920016")
      (read-until telnet "~]#")
      (write telnet "pkill in.telnetd")
      (is (thrown? java.lang.IllegalArgumentException
                   (read-until telnet "]#"))))))

(deftest write-test
  (testing "write"
    (let [telnet (get-telnet "rainmaker.wunderground.com" 23)]
      (read-until telnet "Press Return ")
      (write telnet "\r")
      (read-until telnet "code-- ")
      (write telnet "STL")
      (let [s (read-until telnet "X to exit: ")]
        (is (.contains s "Forecast for St Louis, MO")))
      (write telnet "X")
      (kill-telnet telnet))))

(deftest kill-telnet-test
  (testing "kill-telnet"
    (let [telnet (get-telnet "rainmaker.wunderground.com")]
      (is (= nil (kill-telnet telnet)))
      (is (thrown? java.lang.NullPointerException (kill-telnet telnet))))))
