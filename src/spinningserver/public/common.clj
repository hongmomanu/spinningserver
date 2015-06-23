(ns spinningserver.public.common
  (:use compojure.core)
  (:require
            [clojure.data.json :as json]
            )
  )


(defn write-ObjectId [k v]
    (let [condition (class v)]
        (cond (= org.bson.types.ObjectId condition) (str v) (= java.util.Date condition)
        (str (java.sql.Date. (.getTime v))) :else v)

    )

    )

(defn lazy-contains? [col key]
  (some #{key} col))

(def datapath (str (System/getProperty "user.dir") "/"))
(def applymoney 5)
(def quickapplymoney 10)
(def applyquicktime -30)



