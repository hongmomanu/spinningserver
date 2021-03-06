(ns spinningserver.routes.factory
  (:require [spinningserver.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [clojure.java.io :as io]
            [spinningserver.controller.factory :as factory]
            [spinningserver.public.websocket :as websocket]
            ))



(defroutes factory-routes

  (GET "/factory/test" [] (str "test"))
  (POST "/factory/sendmycustomerTofactory"[customerid factoryid fromfactoryid text] (factory/sendmycustomerTofactory
                                                                           customerid factoryid fromfactoryid text
                                                                           1
                                                                           websocket/channel-hub-key
                                                                           ))

  (POST "/factory/sendmyfactoryTocustomer"[customerid factoryid fromfactoryid text] (factory/sendmycustomerTofactory
                                                                           customerid factoryid fromfactoryid text
                                                                           1
                                                                           websocket/channel-hub-key
                                                                           ))

  (POST "/factory/addblacklist" [customerid factoryid ] (factory/addblacklist customerid factoryid ))

  (POST "/factory/acceptrecommend"[rid ] (factory/acceptrecommend rid 1 websocket/channel-hub-key))
  (POST "/factory/acceptquickapply"[aid customerid factoryid addmoney]
    (factory/acceptquickapply aid customerid factoryid addmoney websocket/channel-hub-key))

  (GET "/factory/sendmsgtocustomer" [factoryid customerid message] (factory/sendmsgtocustomer factoryid customerid message))

  (POST "/factory/newfactory" [factoryname factoryaddress factoryinfo  username realname password]

    (factory/newfactory factoryname factoryaddress factoryinfo  username realname password)

    )

  (POST "/factory/newfactoryuser" [ username realname password factoryid usertype]

    (factory/newfactoryuser  username realname password factoryid usertype)

    )
  (POST "/factory/editfactoryuser" [ _id realname password factoryid]

    (factory/editfactoryuser  _id realname password factoryid)

    )
  (POST "/factory/delfactoryuser" [ _id ]

    (factory/delfactoryuser  _id)

    )


  (POST "/factory/updatefactorylocation" [lon lat factoryid]

    (factory/updatefactorylocation lon lat factoryid)

    )

  (POST "/factory/addfactorybyid" [from to ]

    (factory/addfactorybyid from to websocket/channel-hub-key)

    )

 (POST "/factory/getmycustomer" [factoryid]
    (factory/getmycustomer  factoryid)
    )

  (POST "/factory/getfactoryinfobyid" [factoryid]
    (factory/getfactoryinfobyid  factoryid)
    )

  (GET "/factory/getgoodsbyfid" [factoryid]
    (factory/getgoodsbyfid  factoryid)
    )

  (GET "/factory/getordersbyfid" [factoryid status]
    (factory/getordersbyfid  factoryid status)
    )
  (GET "/factory/getordersbycid" [clientid status]
    (factory/getordersbycid  clientid status)
    )

  (GET "/factory/getgoodsbykeyword" [keyword page limit ]
    (factory/getgoodsbykeyword  keyword page limit )
    )

  (POST "/factory/addgoodsbyfid" [factoryid goodsname price unit colors imgs]
    (factory/addgoodsbyfid  factoryid goodsname price unit colors imgs)
    )

  (POST "/factory/gethasnumbygid" [gid]
    (factory/gethasnumbygid  gid)
    )
  (POST "/factory/sendtowork" [_id hasnum]
    (factory/sendtowork  _id hasnum websocket/channel-hub-key)
    )

  (POST "/factory/changestatusbyid" [status oid]
    (factory/changestatusbyid  status oid websocket/channel-hub-key)
    )

  (POST "/factory/makegoodnumsbyid" [num factoryid goodsid]

      (factory/makegoodnumsbyid  num factoryid goodsid)

    )

  (POST "/factory/altergoodsbyfid" [gid goodsname price unit colors imgs]
    (factory/altergoodsbyfid  gid goodsname price unit colors imgs)
    )



 )
