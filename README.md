# cats-defurred

A [Manifold Deferred](https://github.com/ztellman/manifold) monad for
[cats library](https://github.com/funcool/cats).
Clojure only at this point, since Manifold is currently Clojure only


The simplest way to use _cats-defurred_ in a Clojure project is by including
it as a dependency in your *_project.clj_*:

```clojure
[defurred "0.0.1"]
```

## Getting Started ##

_cats-defurred_ is essentially the same as
[cats-channel](https://github.com/funcool/canal), but uses a
[Manifold](https://github.com/ztellman/manifold)
[Deferred](https://github.com/ztellman/manifold/blob/master/docs/deferred.md)
object as the container for asynchronously realized values, instead of a
_core.async_ _channel_

A _Deferred_ can be used as a functor :

```clojure
(require '[cats.core :as m])
(require '[cats.monad.deferred :as d])
(require '[manifold.deferred :refer [deferred success!]])

;; Declare arbitrary Deferred with initial value
(def dval (d/with-value 2))

;; Use Deferred as a functor
@(m/fmap inc dval)
;; => 3
```

_Deferred_ also fulfills the monad abstraction :

```clojure
(def result (m/mlet [a (d/with-value 2)
                     b (d/with-value 3)]
              (m/return (+ a b))))
@result
;; => 5
```

now the cool stuff - combining the deferred monad with error monads :

```clojure
(require '[cats.monad.either :as either])

;; Declare a monad transformer
(def either-deferred-m
  (either/either-transformer d/deferred-monad))

;; A success example
@(m/with-monad either-deferred-m
    (m/mlet [a (d/with-value (either/right 2))
             b (d/with-value (either/right 3))]
         (m/return (+ a b))))
;; => #<Right [5]>
```

and demonstrating the error short-circuiting which makes
for compact and comprehensible async implementations :

```clojure
@(m/with-monad either-deferred-m
    (m/mlet [a (d/with-value (either/left "Some error"))
             b (d/with-value (either/right 3))]
      (m/return (+ a b))))
;; => #<Left [Some error]>
```



## License

Copyright © 2015 Employee Republic LImited

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
