---
title: coast.beta: an easy way to make websites with clojure
date: March 18 2018
preview: When it’s time to make a website, what do you really want? You want to associate a url with a bit of code. ~~You don’t want route files and~~ you don’t want to be googling dependencies left and right, you want...
---

When it’s time to make a website, what do you really want? You want to associate a url with a bit of code. ~~You don’t want route files and~~ you don’t want to be googling dependencies left and right, you want to stand up a website in as few lines of code as possible, the closer to zero code you can get, the better. That’s what the goal of coast.beta is, you can generate crud websites from the command line, living the dream.

## Routing

This is a route

```clojure
[:get "/" home]
```

This is a route in coast

```clojure
(defn hello [request]
  [:div "hello world!"])

(def routes [[:get "/" hello]])

(def app (coast/app routes))
```

Routes in coast are a vector of vectors, the routes that are on top get matched first

```clojure
(defn hello [request]
  [:div "hello world!"])

(defn goodbye [request]
  [:div "goodbye, cruel world!"])

(def routes [[:get "/" hello]
             [:get "/" goodbye]])

(def app (coast/app routes))

(app {:request-method :get :uri "/"}) ; => <div>hello world!</div>
```


Here's a more complete example for something like auth with [buddy](https://funcool.github.io/buddy-auth/latest/)

```clojure
(ns routes
  (:require [coast.router :refer [get post put delete wrap-routes]]
            [controllers.home :as c.home]
            [controllers.users :as c.users]
            [buddy.auth])
  (:refer-clojure :exclude [get]))

(defn wrap-auth [handler]
  (fn [request]
    (if (buddy.auth/authenticated? request)
      (handler request)
      (coast.responses/forbidden
        [:div "I'm sorry dave, I can't let you do that."]))))

(def auth (-> (get "/users/:id" c.users/show)
  (wrap-routes middleware/wrap-auth)))

(def public (get "/" c.home/index))

(def routes (concat public auth))
```

## Models

Since clojure doesn’t have objects, there isn't an ORM. Prepare yourself for the raw, awesome power of SQL. Similar to the way you want to tie a url to function that emits html, you also want to tie a function to a bit of SQL on the other end. There are other ways to do this, but the best way I’ve found is instead of trying to treat SQL like a data structure or use another DSL that’s missing joins or something, just use SQL. Here’s a SQL file with comments

```sql
-- resources/sql/posts.db.sql
-- name: list
select *
from posts
order by created_at
offset :offset
limit :limit
```

Here a clojure function with the same name as the name in the sql comments.

```clojure
(ns db.posts
  (:require [coast.db :refer [defq])
  (:refer-clojure :exclude [list]))

(defq list "/resources/sql/posts.sql")

(list {:offset 0 :limit 10}) ; => [{:id 1 :title "" :body ""}]
```

When you call that function, you get data. It seems like there’s something missing, but there isn’t.

## Views

Unlike sql I don’t have a fondness for closing angle brackets in html, so I did away with it in favor of a clojure vector based representation of html:

```clojure
(defn layout [request body]
  [:html
    [:head
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:meta {:charset "utf-8"}]

      [:link {:href "/css/app.css" :type "text/css" :rel "stylesheet"}]
      [:script {:src "/js/app.js" :type "text/javascript"}]]
    [:body
      body]])
```

And that’s that. Models, views, controllers and routes.

## Generators

Why write all of this code when you don’t have to. Generate code statically, not in a macro and now we have the best of all worlds, quick code generation that you can edit and it’s just sitting there statically! Macros would have been similar but less visible.

```bash
coast gen migration create-posts title:text body:text
coast gen model posts
coast gen controller posts
coast gen view posts
coast gen mvc posts
```

## Testing

There is testing too, not like capital T testing, since I usually make websites with no users, there’s no point to test anything. What I do do 😐 though is test my app at the repl, don’t even need a running http server to test things, you can make your coast app, bind it and call it as a function that takes a ring request map.

```clojure
; src/controllers/home.clj
(ns controllers.home)

(defn index [request]
  [:div "hello world!"])

; routes.clj
(ns routes
  (:require [coast.router :refer [get]]
            [controllers.home :as c.home]))
  (:refer-clojure :exclude [get])

(def routes (get "/" c.home/index))

; server.clj
(ns server
  (:require [coast.beta :as coast]))
            [routes :refer [routes]]

(def app (coast/app routes))

(app {:request-method :get :uri "/"}) ; => <div>hello world</div>
```

The way coast (and clojure ring websites in general) works is that you then pass this function to an HTTP server that does all the dirty work of actually handling sockets and things, pretty cool right?

## Background Jobs

I have come up with a very basic background jobs system, but it’s kind of a piece of garbage, it just polls the database every 10 seconds looking for jobs  to run. Here’s what jobs look like

```bash
# set up the jobs table
coast gen jobs
make db/migrate
```

Now that you have the jobs schema in the database, you can go ahead and start queuing stuff up

```clojure
(ns emails)

(defn send [m]
  ; doesn't actually send any emails
  (-> (select-keys m [:to :from :subject :text :html])
      (println))
```

Then when it’s time to queue up a job, you can do this

```clojure
(:require [coast.jobs :as jobs])

(jobs/queue #'emails/send {:to "" :from "" :subject "" :text ""})
```

You can also schedule things to happen in the future

```clojure
(:require [coast.jobs :as jobs]
          [coast.time :as time])

(jobs/queue #'emails/send {:to "" :from "" :subject "" :text ""} (time/at 20 :minutes/from-now))
```

That’s [coast.beta](https://github.com/swlkr/coast)
