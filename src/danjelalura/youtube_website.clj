(ns danjelalura.youtube-website
  (:gen-class)
  (:require
   [ring.adapter.jetty :as adapter]
   [ring.util.response :refer [response]]
   [compojure.core     :refer [defroutes GET POST]]
   [compojure.route    :refer [not-found]]
   [clj-http.client    :as http-client]
   [clojure.data.json  :as json]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def youtube-url-channel-practicalli
  (str "https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&channelId=UCLsiVY-kWVH1EqgEtZiREJw&key=" (System/getenv "YOUTUBE_API_KEY")))


(def practicalli-channel-playlists-full-details
   (get
    (json/read-str
     (:body
      (http-client/get youtube-url-channel-practicalli)))
    "items"))

(defn playlist-names
  "Extract YouTube id and title for each Playlist found in the channel"
  [all-playlists]
  (into {}
        (for [playlist all-playlists
              :let     [id (get playlist "id")
                        title (get-in playlist ["snippet" "title"])]]
          {id title})))

; List of Ids

(defn playlist-ids
  [all-playlists]
  (for [playlist all-playlists
        :let [id (get playlist "id")]]
    id))



(defn youtube-url-channel-practicalli-playlist
  "data about a playlist"
  [playlist-id]
     (str "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,id&playlistId=" playlist-id "&key=" (System/getenv "YOUTUBE_API_KEY")))

#_(def youtube-url-channel-practicalli-playlist-study-group
  (str "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,id&playlistId=PLpr9V-R8ZxiDjyU7cQYWOEFBDR1t7t0wv&key=" (System/getenv "YOUTUBE_API_KEY")))

(youtube-url-channel-practicalli-playlist (first (playlist-ids practicalli-channel-playlists-full-details)))


(defn getPlayListData
  [playlist-id]
  (get
   (json/read-str
    (:body
     (http-client/get
      (youtube-url-channel-practicalli-playlist playlist-id))))
   "items"))


(defn playlist-items
  [playlist]
  (into {}
        (for [item playlist
              :let [videoId (get-in item ["snippet" "resourceId" "videoId"])
                    title (get-in item ["snippet" "title"])
                    url (get-in item ["snippet" "thumbnails" "default" "url"])]]

          {videoId {:title title :url url}})))

(def study-group
  (response
   (str
    (playlist-items
     (getPlayListData "PLpr9V-R8ZxiDjyU7cQYWOEFBDR1t7t0wv")))))

(defroutes webapp
  (GET "/" [] (response "home-page"))
  (GET "/study-group" [] study-group)
  (not-found
   "<h1>Page not found.</h1>"))

(defn jetty-shutdown-timed
  "Shutdown server after specific time,
  allows time for threads to complete.
  Stops taking new requests immediately by
  closing the HTTP listener and freeing the port."
  [server]
  (.setStopTimeout server 100)
  (.setStopAtShutdown server true))


(defonce server
  (adapter/run-jetty
   #'webapp
   {:port         8000
    :join?        false
    :configurator jetty-shutdown-timed}))

(.start server)
(.stop server)
