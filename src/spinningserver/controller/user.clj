(ns spinningserver.controller.user
  (:use compojure.core)
  (:require [spinningserver.db.core :as db]
            ;[spinningserver.public.common :as common]
            [noir.response :as resp]
            [clojure.data.json :as json]
            [monger.json]
            )
  (:import [org.bson.types ObjectId]
           )
  )




(defn getuserlocation [id]
    (let [ a (db/get-user id)]

    (json/write-str a )
    ;(resp/json [{:foo "bar"}])

    )

  )


(defn getfactorys []
    (let [factorys (db/get-factorys)]
        (json/write-str factorys)
    )
)

(defn getfactorysbyid [id]
    (let [
           rids (concat
                  (map #(ObjectId. (:rid %)) (db/get-relation-factory {:factoryid id} ))
                  (map #(ObjectId. (:factoryid %)) (db/get-relation-factory {:rid id}))
                  )
           factorys (db/get-factorys-byid  rids)
           ]
        (json/write-str factorys)
    )
)

(defn getcustomersbyid [id]
    (let [
           rids (map #(ObjectId. (:customerid %)) (db/get-relation-customer {:factoryid id} ))

           customers (db/get-customers-byid  rids)
           ]
        (json/write-str customers)
    )
)
(defn patientlogin [user pass]

   (json/write-str {:success true})
  )
(defn getmenbers [factoryid]

  (let [
         factorys (db/get-factorys-by-cond {:factoryid factoryid })
         ]

        (json/write-str factorys)

    )
  )

(defn factorylogin [username password]
  ;(println "22222222222222")
    (let [
        factory (db/get-factory-byusername username)

    ]
      (if (and factory (= password (:password factory)))(json/write-str {:success true :user factory
                                          :factoryinfo (db/get-factoryinfo-byid (ObjectId. (:factoryid factory)))})
        (json/write-str {:success false}))

    )
)
(defn customerlogin [username password]
    (let [
        customer (db/get-customer-byusername username)

    ]

    (if (and customer (= password (:password customer)))(json/write-str {:success true :user (conj customer {:usertype 3})})
    (json/write-str {:success false}))
    )
)

