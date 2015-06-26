  (ns spinningserver.db.core
  (:require
    [yesql.core :refer [defqueries]]
    [clojure.java.io :as io]
    [monger.core :as mg]
    [monger.collection :as mc]
    [monger.operators :refer :all]
    [monger.query :refer [with-collection find options paginate] ]
    ))

#_(def db-store (str (.getName (io/file ".")) "/site.db"))

#_(def db-spec
  {:classname   "org.h2.Driver"
   :subprotocol "h2"
   :subname     db-store
   :make-pool?  true
   :naming      {:keys   clojure.string/lower-case
                 :fields clojure.string/upper-case}})

#_(defqueries "sql/queries.sql" {:connection db-spec})

  (defonce db (let [uri (get (System/getenv) "MONGOHQ_URL" "mongodb://jack:1313@127.0.0.1/spinningapp")
                    {:keys [conn db]} (mg/connect-via-uri uri)]
                db))


  (defn create-user [user]
    (mc/insert db "users" user))

  (defn update-user [id first-name last-name email]
    (mc/update db "users" {:id id}
      {$set {:first_name first-name
             :last_name last-name
             :email email}}))

  (defn get-user [id]

    (mc/find-maps
      db "userslocation" {:userid id}  [:userid])
    )

  (defn get-factorys[]
    (mc/find-maps
      db "factoryuser"
      )

    )

  

  (defn update-factory [cond modified]
    (mc/update db "factoryuser" cond {$set modified} )
    )

  (defn make-new-factory-user [user]
    (mc/insert-and-return db "factoryuser" user)
    )

  (defn get-factorys-by-cond [cond]

    (mc/find-maps
      db "factoryuser" cond
      )

    )

  (defn create-message [message]
    (mc/insert-and-return db "messages" message)

    )

  (defn get-message [cond]
    (mc/find-maps db "messages" cond)

    )
  (defn get-message-num [cond]
    (mc/count db "messages" cond)
    )
 

  (defn update-message [cond modified]

    (mc/update db "messages" cond {$set modified} {:multi true})

    )

  (defn get-factory-byusername [username]
    (mc/find-one-as-map
      db "factoryuser" {:username username}
      )
    )
  (defn get-customer-byusername [username]
    (mc/find-one-as-map
      db "customeruser" {:username username}
      )
    )

  (defn get-factory-byfactoryname [factoryname]
    (mc/find-one-as-map
      db "factoryinfo" {:factoryname factoryname}
      )
    )
  (defn get-factoryinfo-byid [oid]
    (mc/find-map-by-id
      db "factoryinfo" oid
      )
    )
  (defn get-goods-byid [oid]
    (mc/find-map-by-id
      db "factorygoods" oid
      )
    )

  (defn get-goods-by-keyword [keyword page limit]

    (with-collection db "factorygoods"
      (find {:goodsname {$regex (str ".*" keyword ".*")}})
      (paginate :page page :per-page limit))
    )

  (defn get-goods-by-cond [cond]
    (mc/find-maps
      db "factorygoods" cond
      )
    )
  (defn make-new-goods [good]
    (mc/insert db "factorygoods" good)
    )

  (defn alter-goods [modified cond]
    (mc/update db "factorygoods" cond {$set modified} {:multi true})
    )

  (defn make-new-customer [customer]
    (mc/insert-and-return db "customeruser" customer)
    )

  (defn make-new-factory [factory]
    (mc/insert-and-return db "factoryinfo" factory)
    )

  (defn get-factorys-byid [ids]
    (mc/find-maps
      db "factoryuser" {:_id {$in ids}}
      )
    )


  (defn get-relation-factory [cond]
    (mc/find-maps db "factorysvsfactorys" cond )
    )

  (defn get-relation-customer [cond]
    (mc/find-maps db "factorysvscustomers" cond )
    )


  (defn get-customers-byid [ids]
    (mc/find-maps
      db "customeruser" {:_id {$in ids}}
      )
    )



  (defn get-customer-byid [oid]
    (mc/find-map-by-id
      db "customeruser" oid
      )
    )

  (defn get-enumerate-by-type [type]

    (mc/find-maps
      db "enumerate" {:enumeratetype type}
      )

    )


  (defn get-factory-byid [oid]
    (mc/find-map-by-id db "factoryuser" oid)

    )

  (defn makerecommend [cond recommend]

    (mc/update db "recommend" cond {$set recommend} {:upsert true})

    )


  (defn makefactorysvscustomers [cond recommend]

    (mc/update db "factorysvscustomers" cond {$set recommend} {:upsert true})

    )

  (defn makefactorysvsfactorys [data]

    (mc/insert db "factorysvsfactorys" data)

    )

  (defn findrecommend [cond]
    (mc/find-one-as-map
      db "recommend" cond
      )
    )

  (defn findrecommends [cond]
    (mc/find-maps
      db "recommend" cond
      )
    )


  (defn update-recommend [cond modified]

    (mc/update db "recommend" cond {$set modified} {:multi true})

    )

  (defn update-recommend-return [data]
    (mc/save-and-return db "recommend" )
    )

  (defn update-custompush [cond modified]
    (mc/update db "custompush" cond {$set modified} {:upsert true})
    )

  (defn createblacklist [cond modified]
    (mc/update db "blacklist" cond {$set modified} {:upsert true})
    )

  (defn create-applyfactorys [cond modified]
    (mc/update db  "applyfactorys" cond {$set modified} {:upsert true})
    )
  (defn update-applyfactorys [cond modified]
    (mc/update db  "applyfactorys" cond {$set modified} )
    )

  (defn get-applyingquick [cond]
    (mc/find-one-as-map db "applyfactorys" cond)
    )

  (defn get-applyingquick-list [cond]
    (mc/find-maps db "applyfactorys" cond)
    )

  (defn get-custompush  [cond]
    (mc/find-one-as-map
      db "custompush" cond
      )

    )
  (defn get-apply-by-pid-dic [cond]

    (mc/find-one-as-map
      db "applyquick" cond
      )
    )

  (defn get-apply-by-pid [cond]

    (mc/find-maps
      db "applyquick" cond
      )
    )

  (defn make-apply-by-pid-dic [cond modified]

    (mc/update db "applyquick" cond {$set modified} {:upsert true})

    )

  (defn get-money-byid [userid]
    (mc/find-one-as-map
      db "money" {:userid userid}
      )

    )

  (defn update-money-byid [cond modified]
    (mc/update db "money" cond {$set modified} {:upsert true})
    )
