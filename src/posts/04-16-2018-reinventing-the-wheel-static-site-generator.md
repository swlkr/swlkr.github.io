---
title: Reinventing the wheel with a clojure static site generator
date: April 16 2018
preview: There's probably nothing I love more in this world than coming up with my own solution to an already solved problem. People call it...
---

There's probably nothing I love more in this world than coming up with my own solution to an already solved problem. People call it yak shaving, re-inventing the wheel, what have you, I love it. My latest re-invention is a just-for-me static site generator. It turns markdown files in a folder into html pages fit for reading. There are a few parts to any static site generator, the first of which is file/folder organization.

## Organization

In the root of the project is the index.html, css and javascript files which I started hand-editing just like the good old days. After a few different blog posts, things started to get a little out of hand, I started to make mistakes trying to copy/paste headers/footers. So I decided to make my own static site generator in about 100 lines of clojure. The first thing is that I had to separate where the clojure and html "templates" or really just placeholders for the real generated html would go. I decided on src for clojure code and templates and /posts for the generated html files for posts and to keep everything else in the top level folder. That was that. Now I only had to worry about converting markdown to html and gluing it together with html "layout" files or shared bits of html like the header/footer and css/js file references.

## Code structure

The clojure compiler is "single pass" meaning you can't reference a function until you define it, so all clojure code works from the bottom of the file to the top. To find out where the top is you have to look at the bottom. Classic clojure simplicity. The main idea of the generator is to `slurp` up markdown and html files in `/src/posts`, turn them into a clojure hash map (or dictionary) as all html, then `spit` them into `/posts`. Here's the first part of that:

```clojure
(defn replacement [match m]
  (let [fallback (first match)
        k (-> match last keyword)]
    (str (get m k fallback))))

(defn fill [m s]
  (string/replace s #"__([\w-]+)__" #(replacement % m)))
```

These two functions are really the meat and potatoes of this whole thing, the CRUX of what I'm talking about. These two functions fill in blanks that __look-like-this__ with any variable in a given clojure hash map. It works like this:

```clojure
(fill {:name "sean"} "hello __name__") ; => "hello sean"
```

Not super impressive by itself, it's downright boring, but when you start getting the strings from files, it becomes a templating function:

```clojure
(defn front-matter [k s]
  (some-> (re-pattern (format "%s:(.+)" (name (or k :nothing))))
          (re-find s)
          (second)
          (string/trim)))

(def title (partial front-matter :title))
(def date (partial front-matter :date))
(def preview (partial front-matter :preview))

(defn post [filename]
  (let [filepath (format "src/posts/%s" filename)
        s (slurp filepath)
        preview (-> (preview s)
                    (md/md-to-html-string))
        title (title s)
        date (date s)
        markdown (drop-front-matter s)
        content (md/md-to-html-string markdown)
        url (format "posts/%s" (string/replace filename #".md" ".html"))]
    {:title title
     :date date
     :url url
     :markdown markdown
     :content content
     :preview preview}))
```

So ignoring the markdown frontmatter stuff, this is how a given post gets rendered into html from markdown and a given html template, here it's a map to make the next step easier:

```clojure
(defn render-post-preview [post]
  (let [layout (slurp "src/posts/post.html")]
    (fill post layout)))

(defn render-post [post]
  (let [layout (slurp "src/posts/layout.html")
        post-html (fill post layout)
        html (fill {:content post-html
                    :relative "../"} (slurp "src/layout.html"))]
    (spit (:url post) html)))
```

These two functions render a small preview on the index page and an actual post. Since I don't have to worry about anyone else using this thing, I can just hard code the paths where the html templates are, and I did. Software is so easy when you're the only user.

## The end

So in the end, I've reinvented the wheel, and had a lot of fun doing it. The next time someone says that some piece of software already exists, ignore them immediately and do what you want to do, TREAT YOURSELF to some new code and maybe come up with a better way to do something! If you're curious, the whole thing is [here in a gist](https://gist.github.com/swlkr/65e68ad068d461767bbf78591f3415c4)
