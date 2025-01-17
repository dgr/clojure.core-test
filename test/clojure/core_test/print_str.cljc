(ns clojure.core-test.print-str
  (:require #?(:cljs  [cljs.reader])
            [clojure.test :as t :refer [deftest testing is are]]
            [clojure.core-test.portability #?(:cljs :refer-macros :default :refer)  [when-var-exists]]))

(when-var-exists clojure.core/print-str
 (deftest test-print-str
   (is (= "a string" (print-str "a" "string")))
   (is (= "nil a string A   1 17.0 [:a :b] {:c :d} #{:e}"
          (print-str nil "a" "string" \A \space 1 17.0 [:a :b] {:c :d} #{:e})))))
