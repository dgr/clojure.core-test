(ns clojure.core-test.byte
  (:require [clojure.test :as t :refer [deftest testing is are]]))

(deftest test-byte
  ;; There is no platform independent predicate to test for a
  ;; byte (e.g., `byte?`). In ClojureJVM, it's an instance of
  ;; `java.lang.Byte`, but there is no predicate for it. Here, we just
  ;; test whether it's a fixed-length integer of some sort.
  (is (int? (byte 0)))
  #?@(:cljs nil
      :default
      [(is (instance? java.lang.Byte (byte 0)))])

  ;; Check conversions and rounding from other numeric types
  (are [expected x] (= expected (byte x))
    -128 -128
    0    0
    127  127
    1    1N
    0    0N
    -1   -1N
    1    1.0M
    0    0.0M
    -1   -1.0M
    1    1.1
    -1   -1.1
    1    1.9
    1    3/2
    -1   -3/2
    0    1/10
    0    -1/10
    1    1.1M
    -1   -1.1M)

  ;; `byte` throws outside the range of 127 ... -128.
  (is (thrown? IllegalArgumentException (byte -128.000001)))
  (is (thrown? IllegalArgumentException (byte -129)))
  (is (thrown? IllegalArgumentException (byte 128)))
  (is (thrown? IllegalArgumentException (byte 127.000001)))

  ;; Check handling of other types
  (is (thrown? ClassCastException (byte "0")))
  (is (thrown? ClassCastException (byte :0)))
  (is (thrown? ClassCastException (byte [0])))
  (is (thrown? Exception (byte nil))))

