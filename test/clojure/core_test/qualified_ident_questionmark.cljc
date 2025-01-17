(ns clojure.core-test.qualified-ident-questionmark
  (:require #?(:cljs  [cljs.reader])
            [clojure.test :as t :refer [deftest testing is are]]
            [clojure.core-test.portability #?(:cljs :refer-macros :default :refer)  [when-var-exists]]))

(when-var-exists clojure.core/qualified-ident?
 (deftest test-qualified-ident?
   (are [expected x] (= expected (qualified-ident? x))
     false :a-keyword
     false 'a-symbol
     true  :a-ns/a-keyword
     true  'a-ns/a-keyword
     false "a string"
     false 0
     false 0N
     false 0.0
     #?@(:cljs []
         :default
         [false 1/2])
     false 0.0M
     false false
     false true
     false nil)))
