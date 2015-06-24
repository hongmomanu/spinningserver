(ns spinningserver.routes.user
  (:require [spinningserver.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [clojure.java.io :as io]
            [spinningserver.controller.user :as user]
            ))



(defroutes user-routes

  (GET "/user/getuserlocation" [] (user/getuserlocation 1))
  (GET "/user/getfactorys" [] (user/getfactorys ))
  (GET "/user/getfactorysbyid" [id] (user/getfactorysbyid id))
  (GET "/user/getcustomersbyid" [id] (user/getcustomersbyid id))
  (POST "/user/factorylogin" [username password] (user/factorylogin username password))
  (POST "/user/customerlogin" [username password] (user/customerlogin username password))
  (POST "/user/getmenbers" [factoryid] (user/getmenbers factoryid))

 )
