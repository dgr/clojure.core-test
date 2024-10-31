(ns clojure.core-test.add-watch
  (:require [clojure.test :as t :refer [is]]))

(def testvar 40)

(t/deftest watch-atom
  ;; checks atoms and interspersed multiple watches
  (let [state (volatile! [])
        tester1 (fn [key ref old new] (vswap! state conj {:key key :ref ref :old old :new new :tester 1}))
        tester2 (fn [key ref old new] (vswap! state conj {:key key :ref ref :old old :new new :tester 2}))
        err (fn [key ref old new]
              (throw (ex-info "test" {:key key :ref ref :old old :new new :tester :err})))
        a (atom 0)
        r (atom 10)
        update! (fn []
                  (let [do-update (fn [x op]
                                    (try
                                      (op x inc)
                                      (catch clojure.lang.ExceptionInfo e
                                        (let [{:keys [old] :as data} (ex-data e)]
                                          (vswap! state conj data)))))]
                    (do-update a swap!)
                    (do-update r swap!)
                    (do-update #'testvar alter-var-root)))
        keyed (fn [k s] (filter #(= k (:key %)) s))]

    ;; check removing something that isn't there
    (is (= a (remove-watch a :one)))
    ;; check removing nil
    (is (= a (remove-watch a nil)))

    ;; add a watch to the atom
    (is (= a (add-watch a :a tester1)))
    (is (= r (add-watch r :r tester1)))
    (is (= #'testvar (add-watch #'testvar :v tester1)))
    (update!)
    
    ;; add a second watch to the atom - new key
    (add-watch a :s tester2)
    (add-watch r :s tester2)
    (add-watch #'testvar :s tester2)
    (update!)

    ;; replace the first watch by reusing the keys
    (add-watch a :a tester2)
    (add-watch r :r tester2)
    (add-watch #'testvar :v tester2)
    (update!)

    ;; remove the first watche
    (is (= a (remove-watch a :a)))
    (is (= r (remove-watch r :r)))
    (is (= #'testvar (remove-watch #'testvar :v)))
    (update!)

    ;; check progress
    (let [checkdata [{:key :a :ref a :old 0 :new 1 :tester 1}
                     {:key :r :ref r :old 10 :new 11 :tester 1}
                     {:key :v :ref #'testvar :old 40 :new 41 :tester 1}

                     {:key :a :ref a :old 1 :new 2 :tester 1}
                     {:key :r :ref r :old 11 :new 12 :tester 1}
                     {:key :v :ref #'testvar :old 41 :new 42 :tester 1}
                     {:key :s :ref a :old 1 :new 2 :tester 2}
                     {:key :s :ref r :old 11 :new 12 :tester 2}
                     {:key :s :ref #'testvar :old 41 :new 42 :tester 2}

                     {:key :a :ref a :old 2 :new 3 :tester 2}
                     {:key :r :ref r :old 12 :new 13 :tester 2}
                     {:key :v :ref #'testvar :old 42 :new 43 :tester 2}
                     {:key :s :ref a :old 2 :new 3 :tester 2}
                     {:key :s :ref r :old 12 :new 13 :tester 2}
                     {:key :s :ref #'testvar :old 42 :new 43 :tester 2}

                     {:key :s :ref a :old 3 :new 4 :tester 2}
                     {:key :s :ref r :old 13 :new 14 :tester 2}
                     {:key :s :ref #'testvar :old 43 :new 44 :tester 2}]]
      (is (= (keyed :a checkdata) (keyed :a @state)))
      (is (= (keyed :r checkdata) (keyed :r @state)))
      (is (= (keyed :v checkdata) (keyed :v @state)))
      (vreset! state [])

      ;; remove the second watche - should be no updates
      (remove-watch a :s)
      (remove-watch r :s)
      (remove-watch #'testvar :s)
      (update!)
      (is (empty? @state))

      ;; add the first again, and check if it still works
      (add-watch a :a tester1)
      (add-watch r :r tester1)
      (add-watch #'testvar :v tester1)
      (update!)

      (is (= [{:key :a :ref a :old 5 :new 6 :tester 1}] (keyed :a @state)))
      (is (= [{:key :r :ref r :old 15 :new 16 :tester 1}] (keyed :r @state)))
      (is (= [{:key :v :ref #'testvar :old 45 :new 46 :tester 1}] (keyed :v @state)))

      ;; add error watch
      (add-watch a :e err)
      (add-watch r :e err)
      (add-watch #'testvar :e err)
      (update!)

      ;; The final watch may or may not have gone to :a before the error
      ;; so remove this if it is there
      (is (= #{{:key :a :ref a :old 5 :new 6 :tester 1}}
             (disj (set (keyed :a @state))
                   {:key :a :ref a :old 6 :new 7 :tester 1})))
      (is (= #{{:key :r :ref r :old 15 :new 16 :tester 1}}
             (disj (set (keyed :r @state))
                   {:key :r :ref r :old 16 :new 17 :tester 1})))
      (is (= #{{:key :v :ref #'testvar :old 45 :new 46 :tester 1}}
             (disj (set (keyed :v @state))
                   {:key :v :ref #'testvar :old 46 :new 47 :tester 1})))
      
      (is (= #{{:key :e :ref a :old 6 :new 7 :tester :err}
               {:key :e :ref r :old 16 :new 17 :tester :err}
               {:key :e :ref #'testvar :old 46 :new 47 :tester :err}}
             (set (keyed :e @state)))))))

#?(:cljs nil
   :default
   (t/deftest watch-ref
     (let [state (volatile! [])
           tester1 (fn [key ref old new] (vswap! state conj {:key key :ref ref :old old :new new :tester 1}))
           tester2 (fn [key ref old new] (vswap! state conj {:key key :ref ref :old old :new new :tester 2}))
           err (fn [key ref old new]
                 (throw (ex-info "test" {:key key :ref ref :old old :new new :tester :err})))
           r (ref 10)
           update! (fn []
                     (try
                       (dosync (ref-set r (inc @r)))
                       (catch clojure.lang.ExceptionInfo e
                         (let [{:keys [old] :as data} (ex-data e)]
                           (vswap! state conj data)))))
           keyed (fn [k s] (filter #(= k (:key %)) s))]

       ;; add a watch to the ref
       (is (= r (add-watch r :r tester1)))
       (update!)
       
       ;; add a second watch to the ref
       (add-watch r :s tester2)
       (update!)

       ;; replace the first watch by reusing the key
       (add-watch r :r tester2)
       (update!)

       ;; remove the first watch
       (is (= r (remove-watch r :r)))
       (update!)

       ;; check progress
       (let [checkdata [{:key :r :ref r :old 10 :new 11 :tester 1}

                        {:key :r :ref r :old 11 :new 12 :tester 1}
                        {:key :s :ref r :old 11 :new 12 :tester 2}

                        {:key :r :ref r :old 12 :new 13 :tester 2}
                        {:key :s :ref r :old 12 :new 13 :tester 2}

                        {:key :s :ref r :old 13 :new 14 :tester 2}]]
         (is (= (keyed :r checkdata) (keyed :r @state)))
         (vreset! state [])

         ;; remove the second watch - should be no updates
         (remove-watch r :s)
         (update!)
         (is (empty? @state))

         ;; add the first again, and check if is still works
         (add-watch r :r tester1)
         (update!)

         (is (= [{:key :r :ref r :old 15 :new 16 :tester 1}] (keyed :r @state)))

         ;; add error watch
         (add-watch r :e err)
         (update!)

         ;; The final watch may or may not have gone to :r before the error
         ;; so remove this if it is there
         (is (= #{{:key :r :ref r :old 15 :new 16 :tester 1}}
                (disj (set (keyed :r @state))
                      {:key :r :ref r :old 16 :new 17 :tester 1})))
         (is (= [{:key :e :ref r :old 16 :new 17 :tester :err}]
                (keyed :e @state)))))))

#?(:clj (defn sleep [n] (Thread/sleep n)))

#?(:cljs nil
   :default
   (t/deftest watch-agents
     (let [state (volatile! [])
           tester1 (fn [key ref old new]
                     (when (not= old new)
                       (vswap! state conj {:key key :old old :new new :tester 1})))
           tester2 (fn [key ref old new]
                     (when (not= old new)
                       (vswap! state conj {:key key :old old :new new :tester 2})))
           agent-end (promise)
           err (fn [key ref old new]
                 (deliver agent-end :done)
                 (throw (ex-info "test" {:key key :old old :new new :tester :err})))
           g (agent 20)
           update! (fn []
                     (when-let [e (agent-error g)]
                       (vswap! state conj (ex-data e))
                       (restart-agent g :ready))
                     (send g inc))
           keyed (fn [k s] (set (filter #(= k (:key %)) s)))]

       ;; add a watch to the agent
       (is (= g (add-watch g :g tester1)))
       (update!)
       (await g)
       (is (= [{:key :g :old 20 :new 21 :tester 1}]
              @state))
       
       ;; add a second watch - new key
       (add-watch g :s tester2)
       (update!)
       (await g)
       (is (= #{{:key :g :old 20 :new 21 :tester 1}
                {:key :g :old 21 :new 22 :tester 1}
                {:key :s :old 21 :new 22 :tester 2}}
              (set @state)))

       ;; replace the first watch by reusing the key
       (add-watch g :g tester2)
       (update!)
       (await g)
       (is (= #{{:key :g :old 20 :new 21 :tester 1}
                {:key :g :old 21 :new 22 :tester 1}
                {:key :s :old 21 :new 22 :tester 2}
                {:key :g :old 22 :new 23 :tester 2}
                {:key :s :old 22 :new 23 :tester 2}}
              (set @state)))

       ;; remove the first watch
       (is (= g (remove-watch g :g)))
       (update!)
       (await g)
       (is (= #{{:key :g :old 20 :new 21 :tester 1}
                {:key :g :old 21 :new 22 :tester 1}
                {:key :s :old 21 :new 22 :tester 2}
                {:key :g :old 22 :new 23 :tester 2}
                {:key :s :old 22 :new 23 :tester 2}
                {:key :s :old 23 :new 24 :tester 2}}
              (set @state)))

       ;; remove the second watches - should be no updates
       (remove-watch g :s)
       (update!)
       (await g)
       (is (= #{{:key :g :old 20 :new 21 :tester 1}
                {:key :g :old 21 :new 22 :tester 1}
                {:key :s :old 21 :new 22 :tester 2}
                {:key :g :old 22 :new 23 :tester 2}
                {:key :s :old 22 :new 23 :tester 2}
                {:key :s :old 23 :new 24 :tester 2}}
              (set @state)))

       ;; add the first again, and check if it still works
       (add-watch g :g tester1)
       (update!)
       (await g)
       (is (= #{{:key :g :old 20 :new 21 :tester 1}
                {:key :g :old 21 :new 22 :tester 1}
                {:key :s :old 21 :new 22 :tester 2}
                {:key :g :old 22 :new 23 :tester 2}
                {:key :s :old 22 :new 23 :tester 2}
                {:key :s :old 23 :new 24 :tester 2}
                {:key :g :old 25 :new 26 :tester 1}}
              (set @state)))

       ;; add error watches
       (add-watch g :e err)
       (update!)
       (deref agent-end)
       (sleep 1)
       (if-let [e (agent-error g)]
         (do
           (is (= {:key :e :old 26 :new 27 :tester :err} (ex-data e)))
           ;; The final call may not have gone to tester 1
           (is (= #{{:key :g :old 20 :new 21 :tester 1}
                    {:key :g :old 21 :new 22 :tester 1}
                    {:key :s :old 21 :new 22 :tester 2}
                    {:key :g :old 22 :new 23 :tester 2}
                    {:key :s :old 22 :new 23 :tester 2}
                    {:key :s :old 23 :new 24 :tester 2}
                    {:key :g :old 25 :new 26 :tester 1}}
                  (disj (set @state) {:key :g :old 26 :new 27 :tester 1}))))
         (println "Unexpected lack of error"))

       (shutdown-agents))))