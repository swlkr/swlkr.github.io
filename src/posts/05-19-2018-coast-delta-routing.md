---
title: coast.delta routing
date: May 19 2018
preview: Who needs or wants backwards compatibility anyway? I sure don't. Here's what's new with routing   in coast.delta...
img: https://cdn-images-1.medium.com/max/2000/1*ppRT8I5gJASlDkv7otAMHw.jpeg
---

It's remained pretty much the same, except for support for named routes! Finally! Two things had to happen, one, I had to start referring to routes by symbol instead of directly by function due to a circular reference error and two, I had to change most of the user facing api code, [woops](https://www.youtube.com/watch?v=Gzj723LkRJY). Here's what it looks like.

```clojure
(def routes [[:get "/" `controllers.home/index :home]]
```

That last element in the vector is the name. If you don't give it a name, it defaults to
the keyword version of the symbol you give it, so in this case it would be :controllers.home/index.

Here's how you look up routes by name

```clojure
(ns routes
  (:require [coast.router :as router]))

(def routes [[:get "/" `controllers.home/index :home]
             [:get "/items/:id" `controllers.items/show :items/show]])

(def url-for (router/url-for-routes routes))

(url-for :home) ; => "/"

(url-for :items/show {:id 1}) ; => "/items/1"
```

## Forms

There's also a little something for forms which I never liked before, but now I do

```clojure
; routes.clj
(ns routes
  (:require [coast.router :as router]))

(def routes [[:get "/" `controllers.home/index :home]
             [:post "/" `controllers.home/create :create]
             [:put "/:id" `controllers.home/update :update]])

(def url-for (router/url-for-routes routes))
(def action-for (router/action-for-routes routes))

; views/home.clj
(ns views.home
  (:require [routes :refer [action-for]]
            [coast.components :refer [form]]))

(defn index [request]
  (form (action-for :create))) ; => <form method="post" action="/">

(defn edit [request]
  (form (action-for :update {:id 123}))) ; => <form method="post" action="/123?_method=put">
```

I really love this routing with coast and it probably won't change again, it's data, they're named, it's simple and it's beautiful. Hope you like it too. If you're curious about coast and want to give it a shot and you're stuck, send me an email or DM me on twitter any time!
