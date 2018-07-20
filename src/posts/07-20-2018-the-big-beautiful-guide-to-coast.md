---
title: The BIG, BEAUTIFUL Guide to Coast on Clojure
date: July 20 2018
preview: Getting started with coast on clojure is easy! That's kind of the whole point of the thing. Let's make a blog called... bloggg. Points for originality
---

Getting started with coast on clojure is easy! That's kind of the whole point of the thing. Let's make a blog called... bloggg. Points for originality.

## Start here

```bash
coast new bloggg
cd bloggg
git init
git commit -am "Initial commit"
```

Alright, now there's a folder called bloggg, here's what's inside:

```bash
.
├── Makefile
├── README.md
├── bin
│   └── nrepl.clj
├── deps.edn
├── resources
│   └── public
│       ├── css
│       │   └── app.css
│       └── js
│           └── app.js
├── src
│   ├── components.clj
│   ├── controllers
│   │   └── home.clj
│   ├── routes.clj
│   ├── server.clj
│   └── views
│       ├── errors.clj
│       └── home.clj
└── test
└── server_test.clj

9 directories, 13 files
```

Not too much stuff, but still enough to be confusing. Here's how it's laid out, the src/routes.clj is where everything goes down, it's the map to the rest of the app, every function that runs code and every url is here. There's also components which you can use to make your own custom html elements with clojure, so you don't have to worry about styles and things across elements or complex css issues where like some things have padding and some don't. The first thing I do in all of my coast projects is add a css framework, in this case it's tachyons:

```bash
wget -P resources/public/css https://raw.githubusercontent.com/tachyons-css/tachyons/master/css/tachyons.min.css
```

Now let's add that to the layout function which wraps all of your views in some common html, add this underneath the `[:head]` part

```clojure
(defn layout [request body]
  [:html
    [:head
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:link {:href "/css/app.css" :type "text/css" :rel "stylesheet"}]
     ; right here
     [:link {:href "/css/tachyons.min.css" :type "text/css" :rel "stylesheet"}]]
    [:body
     body
     [:script {:src "/js/app.js" :type "text/javascript"}]]])
```

Let's run the app to see our changes:

```bash
make nrepl
```

Then connect to that repl server on port 7888. With atom and proto-repl this looks like Option + Cmd + Y then Enter. Now that you're in a repl navigate over to server.clj and run the -main function to start the development server and subsequently reload any code: (-main).

This is the beauty of clojure, code reloading is built into the language, so any major changes to your code even outside of the framework will get picked up with a call to (-main)! Amaze. Alright. It's time to get down to business and write some code.

## Logging

Now head on over to localhost:1337 in your browser.
Sweet, we have some html rendering! That's half the battle! You'll also notice in the same terminal where the repl server is running, there's output every time you visit a page that looks like this:

```bash
GET / controllers.home/index 200 text/html; charset=utf-8 12ms
```

That's the coast logger working there and it only shows you a few things currently: the http method, the url, the route function that got called, the http response and which content type the response was along with how long the whole thing took.

## Database migrations

Let's add authors (since you can't just have posts lying around without someone to write them) to the data with some sweet, sweet schema action.

```bash
coast gen schema add-authors
```

The name here isn't super important, it's just nice to refer to things by name
You should have a new file in `resources/migrations/` named `{timestamp}-add-authors`. Let's add some text to it:

```clojure
; {timestamp}-add-authors

[{:db/ident :author/nickname
  :db/type "text"}

 {:db/ident :author/email
  :db/type "text"}

 {:db/col :author/password
  :db/type "text"
  :db/nil? false}]
```

Before we try to run a migration, let's make a new database for this project. The database name is a combination of the name of the project and the current environment which by default is "dev" there are three possible environments, dev, test and prod, although for the sake of this tutorial, I'll be focusing on dev.

```bash
make db/create

Database bloggg_dev created successfully
```

Now let's add the authors schema to the db:

```bash
make db/migrate

-- Migrating:  20180717140955_add_authors ---------------------

[#:db{:ident :author/nickname, :type text} #:db{:ident :author/email, :type text} #:db{:col :author/password, :type text, :nil? false}]

-- 20180717140955_add_authors ---------------------

20180717140955_add_authors migrated successfully
```

## Routing

Sweet, it worked with some nice output too! Alright, now let's our authors sign in, out and up. We'll start with sign up. Add a new route and a new file named author:

```bash
touch src/author.clj
```

Here are the routes we need, one to show the sign up form and the other to POST it.

```clojure
(ns routes
  (:require [coast.router]))

(def routes [[:get "/" `home/index :home]
             ; -- these two here --
             [:get "/sign-up" `author/sign-up :author/sign-up]
             [:post "/authors" `author/create :author/create]])
             ; -- these two here --

(def url-for (coast.router/url-for-routes routes))
(def action-for (coast.router/action-for-routes routes))
```

Before we continue we're going to need another dependency. Coast doesn't come with any password hashing stuff, so we're going to grab [buddy.hashers](https://funcool.github.io/buddy-hashers/latest/) to get it going. Stop your repl and add this to `deps.edn` under the `:deps` key

```clojure
buddy/buddy-hashers {:mvn/version "1.3.0"}
```

## Pages

Normally, I would be writing about models, views and controllers, but with how small clojure code is and with some recent advancements in database access in coast, I can safely put all code related to pages in one file named after the pages related to it, here's what I mean:

```clojure
; src/author.clj

(ns author
  (:require [routes :refer [url-for]]
            [coast.validation :as v]
            [coast.error :refer [rescue]]
            [coast.responses :as res]
            [coast.db :as db]
            [buddy.hashers :as hashers]))


(defn sign-up [req]
  [:div
    (form-for ::create
      [:label {:for "author/nickname"}]
      [:input {:type "text" :name "author/nickname"}]

      [:label {:for "author/email"}]
      [:input {:type "email" :name "author/email"}]

      [:label {:for "author/password"}]
      [:input {:type "text" :name "author/password"}]

      [:label {:for "author/password-confirmation"}]
      [:input {:type "text" :name "author/password-confirmation"}]

      [:input {:type "submit" :value "Sign Up"}])])


(defn encrypt-password [m]
  (assoc m :author/password (hashers/derive (:password m))))


(defn create [req]
  (let [[author errors] (-> (:params req)
                            (v/validate [[:required [::nickname ::email ::password]]
                                         [:equal [::password ::password-confirmation]
                                          "dont match"]
                                         [:min-length 12 ::password]])
                            (encrypt-password)
                            (dissoc ::password-confirmation)
                            (db/transact)
                            (rescue))]
    (if (nil? errors)
      (-> (res/redirect (url-for :home/index))
          (res/flash "Welcome to bloggg!"))
      (views.author/new (merge req errors)))))
```

Alright, so this is kind of a lot of stuff. It's overwhelming, no doubt. You have to start somewhere, might as well start here. Here's the breakdown:

The first `ns` part is kind of a mess vs something like rails where you don't have to require anything and you have the whole framework available at your fingertips. I'm still working on this, not quite sure how to make something similar yet...

```clojure
(ns controllers.author
  (:require [routes :refer [url-for]]
            [coast.validation :as v]
            [coast.error :refer [rescue]]
            [coast.responses :as res]
            [coast.db :as db]
            [buddy.hashers :as hashers]))
```

We want `url-for` because referring to routes by name is so much cooler than strings. For this particular piece, we're encrypting a password, so we'll need `buddy.hashers`, we'll also be recovering from business logic errors so we need `coast.error` for `raise` and `rescue` which I'm going to talk more about soon. Also redirecting on form submit so add in `coast.responses` and we'll be saving the params to the db after running it through a few functions, so here comes `coast.db`.

## HTML (a.ka. Views)

```clojure
(defn sign-up [req]
  [:div
    (form-for ::create
      [:label {:for "author/nickname"}]
      [:input {:type "text" :name "author/nickname"}]

      [:label {:for "author/email"}]
      [:input {:type "email" :name "author/email"}]

      [:label {:for "author/password"}]
      [:input {:type "text" :name "author/password"}]

      [:label {:for "author/password-confirmation"}]
      [:input {:type "text" :name "author/password-confirmation"}]

      [:input {:type "submit" :value "Sign Up"}])])
```

It's time to talk about `form-for`. Why not just use `[:form {:method "POST" :action "/sign-up"}]`? Because! CSRF protection is built into coast, and in order for it work, you need to use the `form-for` function from `components`. Things like `url-for` are nice to use with named routes because you already specified the html verb and url back in `routes.clj` you don't need to do it again, unless you want to. This:

```clojure
(form {:method :post :url "/sign-up"})
```

and this

```clojure
(form-for ::create) ; or
(form-for :author/create) ; same thing because we're in (ns author)
```

are the same thing, in fact `form-for` is a **pure** function _(FP alert)_ that is partially applied with routes and returns a map representing the http method and url for the given route name. Slick. 💫

One thing that is kind of awkward looking at this function is the mixing of hiccup with regular clojure functions. I am working on that, but stick with coast and this will get better with time.

## The Request

Moving on from that next bit, let's tackle the actual function that gets called from the client, `create`.

```clojure
(defn create [req]
  (let [[author errors] (-> (:params req)
                            (v/validate [[:required [::nickname ::email ::password]]
                                         [:equal [::password ::password-confirmation]
                                          "dont match"]
                                         [:min-length 12 ::password]])
                            (encrypt-password)
                            (dissoc ::password-confirmation)
                            (db/transact)
                            (rescue))]
    (if (nil? errors)
      (-> (res/redirect (url-for :home/index))
          (res/flash "Welcome to bloggg!"))
      (sign-up (merge req errors)))))
```

The first thing is that this function takes one argument, `req` and here is what it looks like:

```clojure
; the req argument
{:request-method :post
 :uri "/authors"
 :params {:author/nickname ""
          :author/email ""
          :author/password ""
          :author/password-confirmation ""
          :csrf-token ""}}
 ; ... there's a lot more stuff, but this is what we care about
```

If you're interested in knowing about the whole thing you can either `println` it or [head over here and do some reading](https://github.com/ring-clojure/ring/wiki/Concepts#requests).

So hopefully now it's clear what the rest of the code is doing:

1. Validate the values of those keys with `validate`
2. Encrypt the password with `encrypt-password`
3. Select only the keys we need for inserting into the db
4. Save our keys and values to the database with `db/transact`
5. Handle any validation or common database errors with `rescue`

The next thing that is really cool is the thread-first macro `->` this thing "threads" the first argument through each of the functions in the same set of parens `()`. `(:params req)` does not get "threaded", that's just a normal key access function, getting the params out of the map from earlier.

## Validations

The first function is the `v/validate` function which is actually just a wrapper around [this library](https://github.com/jkk/verily)

```clojure
(-> ;(:params req)
    (v/validate [[:required [::nickname ::email ::password]]
                 [:equal [::password ::password-confirmation]
                  "dont match"]
                 [:min-length 12 ::password]])
    ;(encrypt-password)
    ;(select-keys [::nickname ::email ::password])
    ;(db/transact)
    ;(rescue))
```

This is similar to something like rails where you define the validation with a class representing some database table. The next function is `encrypt-password`.

## Encryption

```clojure
(defn encrypt-password [m]
  (assoc m ::password (hashers/derive (::password m))))

(-> ;(:params req)
    ;(v/validate [[:required [::nickname ::email ::password]]
    ;             [:equal [::password ::password-confirmation]
    ;              "dont match"]
    ;             [:min-length 12 ::password]])
    ;(encrypt-password)
    ;(select-keys [::nickname ::email ::password])
    ;(db/transact)
    ;(rescue))

```

## Parameter Whitelist

After this thing is encrypted, we don't need the password-confirmation anymore, we only want `::nickname`, `::email` and `::password`, so we select them with `select-keys`. After that we *finally* get to insert this data into the db with `transact` and we handle any errors from `raise` with `rescue`

```clojure
(-> ;(:params req)
    ;(v/validate [[:required [::nickname ::email ::password]]
    ;             [:equal [::password ::password-confirmation]
    ;              "dont match"]
    ;             [:min-length 12 ::password]])
    ;(encrypt-password)
    (select-keys [::nickname ::email ::password])
    ;(db/transact)
    ;(rescue))

```

## Insert into the database

At this point, we should have everything we need to insert into the database, and that's here

```clojure
(-> ;(:params req)
    ;(v/validate [[:required [::nickname ::email ::password]]
    ;             [:equal [::password ::password-confirmation]
    ;              "dont match"]
    ;             [:min-length 12 ::password]])
    ;(encrypt-password)
    ;(select-keys [::nickname ::email ::password])
    (db/transact)
    ;(rescue))
```

`db/transact` is really cool, it does an upsert so there's no need to worry about whether you're updating or inserting or what have you, just send it maps and you're good to go.

## Errors, Raise and Rescue

```clojure
(let [[author errors] (-> (:params req)
                          (v/validate [[:required [::nickname ::email ::password]]
                                       [:equal [::password ::password-confirmation]
                                        "dont match"]
                                       [:min-length 12 ::password]])
                          (encrypt-password)
                          (select-keys [::nickname ::email ::password])
                          (db/transact)
                          (rescue))
                      ])

; here's what errors looks like in the case of an empty ::nickname input

{:coast.errors/raise true
 :errors {:author/nickname "Nickname cannot be blank"}
 :coast.validation/error :validation}
```

`rescue` is a macro that calls `try` and `catch` on an argument that you pass it, it could be a function, or a series of functions, doesn't matter. It will `catch` errors with `ex-info` with a `{:coast.errors/raise true}` key in them and return them as the second argument in that vector `[author errors]`. It's kind of like a really naive `maybe`. Coast has some built in error handling things around postgres not null and unique errors along with turning validation errors into a map like this `{:author/name "Name cannot be blank"}`. You can customize the message similar to what you saw in the `validate` function.

After all of that there is still some code! Doing the right thing on error or redirecting:

```clojure
(if (nil? errors)
  (-> (res/redirect (url-for :home/index))
      (res/flash "Welcome to bloggg!"))
  (sign-up (merge req errors)))
```

Here if there are no errors, then it's time to redirect to the index page with a flash message and show the new post, which we haven't written anything for that yet, but we'll get there.

If there are errors, then we'll just show the new view again with an `:errors` key and handle those errors in the form, like so:

```clojure
(defn sign-up [req]
  [:div
    (form-for ::create
      [:div {:class "red"}]
        (-> req :errors :author/nickname)
      [:label {:for "author/nickname"}]
      [:input {:type "text" :name "author/nickname"}]

      [:div {:class "red"}]
        (-> req :errors :author/email)
      [:label {:for "author/email"}]
      [:input {:type "email" :name "author/email"}]

      [:div {:class "red"}]
        (-> req :errors :author/password)
      [:label {:for "author/password"}]
      [:input {:type "text" :name "author/password"}]

      [:label {:for "author/password-confirmation"}]
      [:input {:type "text" :name "author/password-confirmation"}]

      [:input {:type "submit" :value "Sign Up"}])])
```

This is such a common pattern, coast will soon abstract some of this away for you, but for right now, it's here in this tutorial.

Stay tuned for the next part where We make this thing a lot prettier and "componentize" some of the html elements...
