---
title: making websites with clojure
date: April 03 2018
preview: In the clojure world making websites is simple, like tropic thunder simple jack simple. There's one function that handles every request and every response and it's made up of a bunch of other functions and they all operate on the same data structure, a clojure map representing an http request and that one function returns a response which is another clojure map.

It looks like this...
---

In the clojure world making websites is simple, like tropic thunder simple jack simple. There's one function that handles every request and every response and it's made up of a bunch of other functions and they all operate on the same data structure, a clojure map representing an http request and that one function returns a response which is another clojure map.

It looks like this:

request -> function -> response

or like this:

```clojure
(defn handler [request]
  (if (and (= (:uri request) "/")
           (= (:request-method request) :get))
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body "hello, world!"}
    {:status 404
     :headers {"Content-Type" "text/html"}
     :body "not found"}))

(handler {:uri "/" :request-method :get}) ; => {:status 200 ...}
```

That's it! This simple concept is almost too simple, like it can't be that simple or that easy. There's got to be a catch how can you make a whole website with authentication and emailing and credit card handling and social features and... from just one function?

Well, I'm glad you asked! The first thing that I should tell you is that there's going to be quite a few more functions, but if you can imagine them all sort of coming together to be one function, it makes it easier to think about. The second thing is I need to quote *the quote* about how it's better to have 100 function operating on 1 data structure than 10 functions operating on 10 data structures or something.

Anyway, to get a more complete app you first need to pass functions to other functions, instead of just the request map, like this:

```clojure
(defn middleware [handler]
  (fn [request]
    (handler request)))
```

So now you can chain these functions together and still have "one function".

```clojure
(defn home [request]
  {:status 200
   :body "home"})

(defn sign-in [request]
  {:status 200
   :body "sign-in"})

(defn first-handler []
  (fn [request]
    (let [{:keys [uri request-method]} request]
      (case
        (= uri "/") (home request)
        (= uri "/sign-in") (sign-in request)))))

(defn logger [handler]
  (fn [request]
    (let [response (handler request)]
      (println (:request-method request) (:uri request))
      response)))

(def app (-> (first-handler)
             (logger)))
```

Things are still looking ugly, but it gets better with coast on clojure :)

Still working on a that whole series of posts going deep, so stay tuned or just [check out it and spoil the surprise](https://github.com/coast-framework/coast)
