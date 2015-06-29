(ns spinningserver.controller.customer
  (:use compojure.core  org.httpkit.server)
  (:require [spinningserver.db.core :as db]
            ;;[spinningserver.public.common :as common]
            [noir.response :as resp]
            [clojure.data.json :as json]
            [clj-time.local :as l]
            [clj-time.format :as f]
            [monger.joda-time]
            [spinningserver.public.common :as commonfunc]
            [spinningserver.controller.factory :as factory]
            [monger.operators :refer :all]
            [clj-time.core :as t]
            )

    (:import [org.bson.types ObjectId]
              )
  )


(defn customer-process [docwithcustomer]
  (conj {:customerinfo (db/get-customer-byid (ObjectId. (:customerid docwithcustomer)))}
          ;{:factoryinfo (db/get-factory-byid (ObjectId. (:factoryid docwithcustomer))) }
    {}

    )
  )

(defn getmycustomersbyid [userid]
  (let [
         myfactorys (db/get-relation-customer {:factoryid userid})
         factoryallcustomers (map #(customer-process %) myfactorys)
         ;customers (apply concat  factoryallcustomers)
         ;filters (filter (fn [x]
         ;                  (not= (:_id (:customerinfo x)) userid))
         ;          customers)
         ]


    (resp/json factoryallcustomers)

    )

  )

(defn ismyfactorysbyid [customerid factoryid]

  (let [
         user (first (db/get-factorys-by-cond {:factoryid factoryid :usertype 0}))
         num (count (db/get-relation-customer {:customerid customerid :factoryid (str (:_id user))}))
         ]

        (if(> num 0) (resp/json {:success true :user user}) (resp/json {:success false :user user}))
    )

  )



(defn getmyfactorysbyid [customerid isreturn]
  (let [
         myfactorys (db/get-relation-customer {:customerid customerid})
         factoryinfo (map #(
                             let [factoryuser  (db/get-factory-byid (ObjectId. (:factoryid %)))
                                  factoryinfo (db/get-factoryinfo-byid (ObjectId. (:factoryid factoryuser)))
                                  ]
                             (conj {:factoryuser factoryuser} {:factoryinfo factoryinfo})

                             ) myfactorys)
         ]
    (if isreturn (resp/json factoryinfo) factoryinfo)
    )


  )

(defn applyforfactory [customerid factoryid ]

  (let [
         myapply (db/get-apply-by-pid-dic {:applyid customerid :factoryid factoryid :ispay true})

         myapply (when myapply (conj myapply {:nums (db/get-message-num
                                                      {:fromid factoryid :msgtime
                                                      { "$gte" (:applytime myapply)
                                                        "$lte" (l/local-now) }}
                                                       )}))
         ]
    (println myapply)
    (resp/json myapply)
    )


  )

(defn makeapplyforfactory [customerid factoryid ]

  (try
    (let [
           money (db/get-money-byid customerid)
           money-factory (db/get-money-byid factoryid)

           totalmoney (if (nil? money) 0 (:totalmoney money))
           totalmoney-factory (if (nil? money-factory) 0 (:totalmoney money-factory))
           ]

      (if (>= totalmoney commonfunc/applymoney)(do

                                                 (db/make-apply-by-pid-dic {:applyid customerid :factoryid factoryid}
                                                              {:applyid customerid :needmoney commonfunc/applymoney :isreply false :factoryid factoryid :applytime (l/local-now)})

                                                            (db/update-money-byid {:userid customerid} {:totalmoney (- totalmoney commonfunc/applymoney)}
                                                    )
                                                 (db/update-money-byid {:userid factoryid} {:totalmoney (+ totalmoney-factory commonfunc/applymoney)}
                                                    )
                                                 (db/make-apply-by-pid-dic {:applyid customerid :factoryid factoryid} {:ispay true})

                                                          (resp/json {:success true})
                                                          )(resp/json {:success false :message
                                                        (str "余额" totalmoney "元,不足支付")}))
      ;(resp/json {:success true})
      )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      ))


  )

(defn getmoneybyid [userid]

  (try
    (let [
           money (db/get-money-byid userid)
           money (if (nil? money) 0 (:totalmoney money))
           ]

      (resp/json {:success true :money money})
      )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )
    )

  )

(defn makemoneybyuserid [userid addmoney isreturn]

  (try
    (let [
           money (db/get-money-byid userid)
           ;totalmoney (:totalmoney money)
           totalmoney (if (nil? money)  0 (:totalmoney money))
           addmoney (read-string addmoney)
           ]

      (db/update-money-byid {:userid userid} {:totalmoney (+ totalmoney addmoney)})

      (when isreturn (resp/json {:success true :message  (+ totalmoney addmoney)}))

      )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      ))

  )

(defn makemoneybyuseridwithapply [userid money factoryid]

  (try
    (do
      (makemoneybyuserid userid money false)
      (makeapplyforfactory userid factoryid)
        )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )
    )

  )


(defn backmoneybyuseridwithapply [userid  factoryid]

  (let [
         needmoney (:needmoney (db/get-apply-by-pid-dic {:applyid userid :factoryid factoryid}))
         applymoney needmoney

         ]

    (try
      (do
        (makemoneybyuserid userid (str "" applymoney) false)
        (makemoneybyuserid factoryid (str "-" applymoney) false)
        (db/make-apply-by-pid-dic {:applyid userid :factoryid factoryid} {:ispay false})
        (resp/json {:success true})
        )
      (catch Exception ex
        (println (.getMessage ex))
        (resp/json {:success false :message (.getMessage ex)})
        )
      )


    )



  )

(defn backmoneybyfactorywithapply [customerid  factoryid channel-hub-key]

  (let [
         needmoney (:needmoney (db/get-apply-by-pid-dic {:applyid customerid :factoryid factoryid}))
         applymoney needmoney
         ]

    (try
      (do
        (makemoneybyuserid customerid (str "" applymoney) false)
        (makemoneybyuserid factoryid (str "-" applymoney) false)
        (db/make-apply-by-pid-dic {:applyid customerid :factoryid factoryid} {:isreply true})
        (factory/sendmsgtocustomer channel-hub-key factoryid customerid "此次诊断已退款")
        )
      (catch Exception ex
        (println (.getMessage ex))
        (resp/json {:success false :message (.getMessage ex)})
        )
      )

    )



  )

(defn continuewithapply [userid  factoryid]
  (try
    (do
      (db/make-apply-by-pid-dic {:applyid userid :factoryid factoryid} {:applytime (l/local-now)})
      (resp/json {:success true})
      )

    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )
    )

  )



(defn getquickfactorysbyid  [customerid distance lon lat]
  (println customerid distance lon lat)
  (let [
         myfactorys (getmyfactorysbyid customerid false)
         nearbyfactorys (db/get-factorys-by-cond  { :loc
                                                  { "$nearSphere"
                                                    { "$geometry"
                                                      {:type   "Point"
                                                      :coordinates  [ (read-string lon)  (read-string lat) ]
                                                       }
                                                      "$maxDistance"  (read-string distance)
                                                      } } } )

         allfactorys (if (> (count myfactorys) 0)
                      (let[
                            filternearfactorys (filter (fn [x]
                                                        (not (commonfunc/lazy-contains? myfactorys  x) ))
                                                nearbyfactorys)
                            ]

                        (concat filternearfactorys myfactorys)
                        )

                      nearbyfactorys
                      )
         ]

    (resp/json allfactorys)
    )

  )
(defn applyforsinglefactory [customerid factoryid addmoney channel-hub-key]

  (db/create-applyfactorys {:customerid customerid :factoryid factoryid}
    {:isaccept false :addmoney addmoney :isread false :applytime (l/local-now) :customerid customerid :factoryid factoryid})

  (let [
         user  (db/get-customer-byid  (ObjectId. customerid))
         channel (get @channel-hub-key factoryid)
         applyfactory (db/get-applyingquick {:customerid customerid :factoryid factoryid})
         ]
    (when-not (nil? channel)

      (send! channel (json/write-str {:type "customerquickapply" :data (conj applyfactory {:userinfo  user})}) false)

      (db/create-applyfactorys  {:customerid customerid :factoryid factoryid} {:isaccept false :isread true})

      )

    )




  )
(defn getquickapplying [customerid channel-hub-key]



  (let [
         oldtime (t/plus (l/local-now) (t/minutes commonfunc/applyquicktime) )
         applytrue (db/get-applyingquick {:customerid customerid
                                          :applytime
                                          { "$gte" oldtime }
                                          :isaccept true })

         applyingquick (if(nil? applytrue)(db/get-applyingquick {:customerid customerid
                                                                 :applytime
                                                                 { "$gte" oldtime }
                                                                 :isread false }) nil)
          channel (get @channel-hub-key customerid)
         ]

    (println applyingquick)

    (when-not (nil? applyingquick)
      (send! channel (json/write-str {:type "quickapplying" :data applyingquick} ) false)
      )
    )
  )

(defn getquickaccept [customerid channel-hub-key]
  (let [
         oldtime (t/plus (l/local-now) (t/minutes commonfunc/applyquicktime) )
         applyaccepted (db/get-apply-by-pid {:applyid customerid
                                          :applytime
                                          { "$gte" oldtime }
                                          :ispay true })

         applynotsaying (db/get-apply-by-pid {:applyid customerid
                                              :applytime
                                              { "$lte" oldtime }
                                              :ispay true })


         applynotsaying (filter (fn [x]
                                  (= (db/get-message-num
                                       {:fromid (:factoryid x) :msgtime
                                       { "$gte" (:applytime x)
                                         "$lte" (l/local-now) }}
                                       ) 0))
                          applynotsaying)




         applyaccepted (concat applyaccepted applynotsaying)


         channel (get @channel-hub-key customerid)
         ]


    (println applyaccepted)
    (dorun (map #(send! channel (json/write-str {:type "quickaccept" :data (db/get-factory-byid (ObjectId. (:factoryid %)))} ) false) applyaccepted))


    )


  )
(defn iscustomerinapplybyfactoryid [customerid factoryid channel-hub-key]

  (let [
         oldtime (t/plus (l/local-now) (t/minutes commonfunc/applyquicktime) )

         applyaccepted (db/get-apply-by-pid-dic {:applyid customerid
                                             :factoryid factoryid
                                             :applytime
                                             { "$gte" oldtime }
                                              :isreply false
                                             :ispay true })

         ]
    (if (nil? applyaccepted) (resp/json {:success false :msg "用户未申请或已退款"})
      (resp/json {:success true}))

  )

  )
(defn applyforquickfactoryswhocanhelp [customerid factoryids addmoney channel-hub-key]

  (let [
         money (db/get-money-byid customerid)
         money (if (nil? money) 0 (:totalmoney money))
         needmoney (+ commonfunc/quickapplymoney (read-string addmoney))
         factoryids (json/read-str factoryids)

         ]
    (if (>= money needmoney) (try
                               (do
                                 (dorun (map #(applyforsinglefactory customerid % (read-string addmoney) channel-hub-key) factoryids))

                                 (resp/json {:success true})
                                 )

                               (catch Exception ex
                                 (println (.getMessage ex))
                                 (resp/json {:success false :message (.getMessage ex)})
                                 )
                               )

      (resp/json {:success false :message (str "余额" money "元,不足支付")})

      )

    )


  ;(resp/json {:success true})


  )

(defn newcustomer [ username realname password]

  (try
    (let [
           customer (db/get-customer-byusername username)


           ]
      (if (nil? customer)
                            (resp/json {:success true :message (conj (db/make-new-customer
                                                                 {:username username
                                                                  :realname realname
                                                                  :password password

                                                                  }) {:usertype 3})})
                             (resp/json {:success true :message "用户已存在"}))

      )

    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )

    )

  )


(defn addfactorybyid [customerid factoryid channel-hub-key]

  (println "2222222eeeee")
  (println customerid factoryid)

  (try
    (let [
           rels (db/get-relation-customer {:factoryid factoryid :customerid customerid})


           channel (get @channel-hub-key factoryid)

           ]

      (if (> (count rels) 0) (resp/json {:success false :message  "关系已经存在"} ) (

                                                             do

                                                               (db/makefactorysvscustomers {:factoryid factoryid :customerid customerid} {:factoryid factoryid :customerid customerid})

                                                               (future (send! channel (json/write-str {:type "scanadd" :data (conj {:fromtype 0} (db/get-customer-byid (ObjectId. customerid)))} ) false))

                                                                                (future (do (factory/chatprocess {:type "factorychat" :fromtype 1
                                                                                                      :from factoryid :to customerid
                                                                                                      :content "已添加您作为我的患者" :imgid -1} channel-hub-key)

                                                                                          (factory/chatprocess {:type "factorychat" :fromtype 0
                                                                                                               :from customerid :to factoryid
                                                                                                               :content "已添加您作为我的医生" :imgid -1} channel-hub-key)

                                                                                          ))
                                                                                (resp/json {:success true})

                                                               )

                                                             )




      )

    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )

    )

  )





