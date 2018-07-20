(ns core
  (:require [clojure.string :as string]
            [markdown.core :as md]
            [clojure.java.io :as io]
            [clojure.data.xml :as xml]))

(def pattern #"__([\w-]+)__")

(defn replacement [match m]
  (let [fallback (first match)
        k (-> match last keyword)]
    (str (get m k fallback))))

(defn fill [m s]
  (string/replace s pattern #(replacement % m)))

(defn front-matter [k s]
  (some-> (re-pattern (format "%s:(.+)" (name (or k :nothing))))
          (re-find s)
          (second)
          (string/trim)))

(def title (partial front-matter :title))
(def date (partial front-matter :date))
(def preview (partial front-matter :preview))
(def published (partial front-matter :published))
(def img (partial front-matter :img))

(defn drop-front-matter [s]
  (some-> (string/replace-first s #"(?s)---\n(.*)\n---" "")
          (string/trim)))

(defn post [filename]
  (let [filepath (format "src/posts/%s" filename)
        s (slurp filepath)
        preview (-> (preview s)
                    (md/md-to-html-string))
        title (title s)
        date (date s)
        published (published s)
        img (img s)
        markdown (drop-front-matter s)
        content (md/md-to-html-string markdown)
        url (format "posts/%s" (string/replace filename #".md" ".html"))]
    {:title title
     :date date
     :url url
     :markdown markdown
     :content content
     :published published
     :img img
     :preview preview}))

(defn render-post-preview [post]
  (let [layout (slurp "src/posts/post.html")]
    (fill post layout)))

(defn render-post [post]
  (let [layout (slurp "src/posts/layout.html")
        post-html (fill post layout)
        html (fill {:content post-html
                    :relative "../"} (slurp "src/layout.html"))]
    (spit (:url post) html)))

(defn posts []
  (->> (io/file "src/posts")
       (file-seq)
       (filter #(.isFile %))
       (map #(.getName %))
       (filter #(.endsWith % ".md"))
       (sort)
       (reverse)
       (map post)
       (filter #(not= "false" (% :published)))))

(defn index []
  (let [s (slurp "src/layout.html")
        content (->> (posts)
                     (map render-post-preview)
                     (string/join "\n"))
        html (fill {:content content
                    :relative ""} (slurp "src/layout.html"))]
    (spit "index.html" html)))

(defn entry [post]
  [:entry
   [:title (:title post)]
   [:updated (:date post)]
   [:author [:name "Sean Walker"]]
   [:link {:href (str "http://swlkr.com/" (:url post))}]
   [:id (str "urn:swlkr-com:feed:post:" (:title post))]
   [:content {:type "html"} (:content post)]])

(defn atom-xml [posts]
  (xml/emit-str
   (xml/sexp-as-element
    [:feed {:xmlns "http://www.w3.org/2005/Atom"}
     [:id "urn:swlkr-com:feed"]
     [:updated (-> posts first :date)]
     [:title {:type "text"} "swlkr"]
     [:link {:rel "self" :href "http://swlkr.com/rss.xml"}]
     (map entry posts)])))

(defn render-rss []
  (doall
   (->> (posts)
        (atom-xml)
        (spit "rss.xml"))))

(defn build []
  (let [posts (posts)]
    (do
      (doall
       (map render-post posts))
      (index)
      (render-rss))))

(defn -main [& args]
  (build))
