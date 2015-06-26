(ns spinningserver.routes.customer
  (:require [spinningserver.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [clojure.java.io :as io]
            [spinningserver.controller.customer :as customer]
            [spinningserver.public.websocket :as websocket]
            [spinningserver.controller.factory :as factory]

            ))



(defroutes customer-routes

  (GET "/customer/getmycustomersbyid" [ customerid ] (customer/getmycustomersbyid  customerid ))

  (GET "/customer/getmyfactorysbyid" [ customerid ] (customer/getmyfactorysbyid  customerid true))

  (POST "/customer/ismyfactorysbyid" [ customerid factoryid] (customer/ismyfactorysbyid  customerid factoryid))


  (GET "/customer/getquickfactorysbyid" [ customerid distance lon lat]
    (println 1111 customerid distance lon lat)
    (customer/getquickfactorysbyid  customerid distance lon lat))



  (POST "/customer/sendmyfactoryTocustomer"[customerid factoryid fromcustomerid text] (factory/sendmycustomerTofactory
                                                                           customerid factoryid fromcustomerid text
                                                                           0
                                                                           websocket/channel-hub-key
                                                                           ))



  (POST "/customer/acceptrecommend"[rid ] (factory/acceptrecommend rid 0 websocket/channel-hub-key))

  (GET "/customer/applyforfactory"[customerid factoryid ] (customer/applyforfactory customerid factoryid ))

  (POST "/customer/makeapplyforfactory"[customerid factoryid] (customer/makeapplyforfactory customerid factoryid ))


  (POST "/customer/makemoneybyuserid" [userid money] (customer/makemoneybyuserid userid money true))


  (POST "/customer/makemoneybyuseridwithapply" [userid money factoryid]
    (customer/makemoneybyuseridwithapply userid money factoryid))

 (POST "/customer/backmoneybyuseridwithapply" [userid  factoryid]
    (customer/backmoneybyuseridwithapply userid  factoryid))

  (POST "/customer/continuewithapply" [userid  factoryid]
    (customer/continuewithapply userid  factoryid))

  (POST "/customer/applyforquickfactoryswhocanhelp" [customerid  factoryids addmoney]
    (customer/applyforquickfactoryswhocanhelp customerid  factoryids addmoney websocket/channel-hub-key))

  (POST "/customer/getmoneybyid" [userid]

    (customer/getmoneybyid userid)

    )

  (POST "/customer/newcustomer" [username realname password]

    (customer/newcustomer  username realname password)

    )

  (POST "/customer/addfactorybyid" [customerid factoryid]

    (customer/addfactorybyid customerid factoryid websocket/channel-hub-key)

    )

  (POST "/customer/iscustomerinapplybyfactoryid" [customerid factoryid]

    (customer/iscustomerinapplybyfactoryid customerid factoryid websocket/channel-hub-key)

    )
  (POST "/customer/backmoneybyfactorywithapply" [customerid factoryid]

    (customer/backmoneybyfactorywithapply customerid factoryid websocket/channel-hub-key)

    )


 )
