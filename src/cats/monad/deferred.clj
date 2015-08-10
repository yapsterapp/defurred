(ns cats.monad.deferred
  (:require [cats.core :as m]
            [cats.context :as ctx]
            [cats.protocols :as proto]
            [cats.monad.either :as either]
            [manifold.deferred :as d]))

(defn with-value
  "wrap a value in a Deferred"
  [v]
  (let [d (d/deferred)]
    (d/success! d v)
    d))

(def ^{:no-doc true}
  deferred-monad
  (reify
    proto/Functor
    (fmap [mn f mv]
      (let [ctx (ctx/get-current mv)
            d (d/deferred)]
        (d/on-realized
         mv
         (fn [v] (d/success!
                  d
                  (m/with-monad ctx (f v))))
         (fn [x] (d/error! d x)))
        d))

    proto/Applicative
    (pure [_ v]
      (with-value v))

    (fapply [mn af av]
      (d/chain af
               (fn [afv]
                 (proto/fmap mn afv av))))

    proto/Monad
    (mreturn [_ v]
      (with-value v))

    (mbind [mn mv f]
      (let [ctx (ctx/get-current mv)]
        (d/chain mv
                 (fn [v]
                   (m/with-monad ctx
                     (f v))))))))

(extend-type manifold.deferred.Deferred
  proto/Context
  (get-context [_] deferred-monad))

(extend-type manifold.deferred.SuccessDeferred
  proto/Context
  (get-context [_] deferred-monad))

(extend-type manifold.deferred.ErrorDeferred
  proto/Context
  (get-context [_] deferred-monad))

(defn either-deferred
  "convert the Deferred error handling model to
   an Either value"
  ([d] (either-deferred d identity))
  ([d error-transformer]
   (let [ed (d/deferred)]
     (d/on-realized
      d
      (fn [v] (either/right v))
      (fn [x] (either/left (error-transformer x)))))))

(def either-deferred-monad
  (either/either-transformer deferred-monad))
