(ns cats.monad.deferred-spec
  (:require [clojure.test :as t]
            [manifold.deferred :as d]
            [cats.protocols :as pt]
            [cats.monad.deferred :as dm]
            [cats.monad.either :as either]
            [cats.core :as m]))

(t/deftest deferred-as-functor
  (let [dv (m/pure dm/deferred-monad 1)]
    (t/is (= 2 (deref (m/fmap inc dv))))))

(t/deftest deferred-as-monad-1
  (let [dv (m/pure dm/deferred-monad 1)]
    (t/is (= 2 (deref (m/>>= dv (fn [x] (m/return (inc x)))))))))

(t/deftest deferred-as-monad-2
  (let [dv1 (d/deferred)
        dv2 (d/deferred)
        dv3 (d/deferred)
        r   (m/mlet [x dv1
                     y dv2
                     z dv3]
                    (m/return (+ x y z)))]

    (d/success! dv1 1)
    (d/success! dv2 1)
    (d/success! dv3 1)
    (t/is (= 3 (+ @dv1 @dv2 @dv3)))))

(t/deftest first-monad-law-left-identity
  (let [dv1 (m/pure dm/deferred-monad 4)
        dv2 (m/pure dm/deferred-monad 4)
        vl  (m/>>= dv2 dm/with-value)]
    (t/is (= @dv1
             @vl))))

(t/deftest second-monad-law-right-identity
  (let [dv1 (dm/with-value 2)
        rs  (m/>>= (dm/with-value 2) m/return)]
    (t/is (= @dv1 @rs))))

(t/deftest third-monad-law-associativity
  (let [rs1 (m/>>= (m/mlet [x  (dm/with-value 2)
                            y  (dm/with-value (inc x))]
                           (m/return y))
                   (fn [y] (dm/with-value (inc y))))
        rs2 (m/>>= (dm/with-value 2)
                   (fn [x] (m/>>= (dm/with-value (inc x))
                                  (fn [y] (dm/with-value (inc y))))))]
    (t/is (= @rs1 @rs2))))

(def defeither-m (either/either-transformer dm/deferred-monad))

(t/deftest deferred-transformer-tests
  (t/testing "deferred combination with either"
    (let [funcright (fn [x] (dm/with-value (either/right x)))
          funcleft (fn [x] (dm/with-value (either/left x)))
          r1 (m/with-monad defeither-m
               (m/mlet [x (funcright 1)
                        y (funcright 2)]
                       (m/return (+ x y))))

          r2 (m/with-monad defeither-m
               (m/mlet [x (funcright 1)
                        y (funcleft :foo)
                        z (funcright 2)]
                       (m/return (+ x y))))]

      (t/is (= (either/right 3) @r1))
      (t/is (= (either/left :foo) @r2)))))
