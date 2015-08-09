(ns cats.monad.deferred
  (:require [cats.core :as m]
            [cats.context :as ctx]
            [cats.protocols :as proto]
            [manifold.deferred :as d]))

(def ^{:no-doc true}
  deferred-monad
  (reify
    proto/Functor
    (fmap [mn f mv]
      (let [ctx (ctx/get-current mv)
            d (d/deferred)]
        (d/chain mv (fn [v]
                      (d/success!
                       d
                       ;; Set double monad to properly handle
                       ;; monad transformers
                       (m/with-monad ctx
                         (f v)))))
        d))

    proto/Applicative
    (pure [_ v]
      (let [d (d/deferred)]
        (d/success! d v)
        d))

    (fapply [mn af av]
      (d/chain af
               (fn [afv]
                 (proto/fmap mn afv av))))

    proto/Monad
    (mreturn [_ v]
      (let [d (d/deferred)]
        (d/success! d v)
        d))

    (mbind [mn mv f]
      (let [ctx (ctx/get-current mv)
            d (d/deferred)]
        (d/chain mv (fn [v]
                      (m/with-monad ctx
                        (d/chain (f v)
                                 (fn [nv]
                                   (d/success! d nv))))))
        d))))

(extend-type manifold.deferred.Deferred
  proto/Context
  (get-context [_] deferred-monad))

(defn with-value
  [v]
  (let [d (d/deferred)]
    (d/success! d v)
    d))
