(ns railscasts-scraper.core
  (:require [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]
            [clojure.java.io :as io])
  (:gen-class))

; link to file
(def episode-list "railscasts-episodes.txt")

(defn get-number-of-pages []
  (-> "http://railscasts.com/?view=list&type=free"
      (java.net.URL.)
      (html/html-resource)
      (html/select [:div.episodes :div.pagination [(html/right :a.next_page)]])
      (first)
      (html/text)
      (Integer/parseInt)))

(defn get-episodes-per-page [page-number]
  (-> page-number))

(defn create-episode-list []
  ; See this nested threading noise. Never do this. I'm just seeing how awful
  ; it actually is.  But I've done that for you. So don't. Please.
  (spit episode-list "")
  (for [episode-url (->> (-> "https://sysadmincasts.com/episodes/"
                         (java.net.URL.)
                         (html/html-resource)
                         (html/select [[:a (html/attr-starts :href "/episodes/")]]))
                      (map #(-> % :attrs :href))
                      (into #{})
                      (map #(java.net.URL. (str "https://sysadmincasts.com" %))))
        :let [url (-> episode-url
                      (html/html-resource)
                      (html/select [:div.about [:a (html/attr-ends :href ".mp4")]])
                      (-> first :attrs :href)
                      (str "\n"))]]
    (do (print "Queueing " url)
        (spit episode-list url :append true))))

(defn download-videos []
  (with-open [episode-reader (io/reader episode-list)]
    (doall
      (for [line (line-seq episode-reader)
            :let [filename (-> line (clojure.string/split #"\/") last)
                  file     (io/file "episodes" filename)]
            :when (not (= "" line))]
        (if (.exists file)
          (println file " already exists so not downloading")
          (do (println "Downloading " line)
              (-> line
                  (client/get { :as :stream })
                  (:body)
                  (io/copy file))))))))

(defn -main [& args]
  (println "Creating episode list in " episode-list)
  (create-episode-list)
  (println "Downloading videos")
  (download-videos)
  (println "Finished"))
