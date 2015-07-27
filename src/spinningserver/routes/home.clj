(ns spinningserver.routes.home
  (:require [spinningserver.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [noir.io :as nio]
            [noir.response :as nresp]
            [clj-time.coerce :as c]
            [clj-time.local :as l]
            [ring.util.response :refer [file-response]]
            [spinningserver.public.common :as commonfunc]
            ))

(defn home-page []
  ;(println (str commonfunc/datapath "upload/"))
  (layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (POST "/common/uploadfile"  [file ]

    (println "okk")
    (let [
           uploadpath  (str commonfunc/datapath "upload/")
           timenow (c/to-long  (l/local-now))
           filename (str timenow (:filename file))
           ]
      (println filename)
      (nio/upload-file uploadpath  (conj file {:filename filename}))
      (nresp/json {:success true :filename filename})
      )

    )

  (GET "/install/:filename" req

    (println req)
    (str req)

    ;;(file-response (str commonfunc/datapath "upload/" filename))
    )

  (GET "/files/:filename" [filename]
    (file-response (str commonfunc/datapath "upload/" filename))
    )

  (GET "/about" [] (about-page)))

