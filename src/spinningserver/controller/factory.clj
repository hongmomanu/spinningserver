(ns spinningserver.controller.factory
  (:use compojure.core  org.httpkit.server)
  (:require [spinningserver.db.core :as db]
            [spinningserver.public.common :as commonfunc]
            [noir.response :as resp]
            [clojure.data.json :as json]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [monger.operators :refer :all]
            [monger.joda-time]
            )

    (:import [org.bson.types ObjectId]
              )
  )

(declare noreadrecommend-process sendrecommendconfirm chatprocess)
(defn noreadrecommend-process [noreadrecommend]
  (let [
         fromid (:fromid noreadrecommend)
         factoryid (:factoryid noreadrecommend)
         customerid (:customerid noreadrecommend)
         rectype (:rectype noreadrecommend)
         frominfo (if (= rectype 1) (db/get-factory-byid  (ObjectId. fromid))
                    (db/get-customer-byid  (ObjectId. fromid))
                    )
         factoryinfo (db/get-factory-byid (ObjectId. factoryid))

         customerinfo (db/get-customer-byid  (ObjectId. customerid))

         ]
    (conj noreadrecommend {:frominfo frominfo :customerinfo customerinfo :factoryinfo factoryinfo})

    )
  )
(defn getnoread [id readtype channel-hub-key]
  (let [
         noreadmessage  (db/get-message {:toid  id :isread false :fromtype 1})
         noreadmessage-customer  (db/get-message {:toid  id :isread false :fromtype 0})


         noreadrecommend (if (= 1 readtype)
                           (db/findrecommends {:factoryid id :isreadbyfactory false })
                           (db/findrecommends {:customerid id :isreadbycustomer false })
                           )
         channel (get @channel-hub-key id)
        ;user (db/get-factory-byid  (ObjectId. id))
        noreadmessage-userinfo (map #(conj % {:userinfo (:userinfo (db/get-factory-byid  (ObjectId. (:fromid %))))}) noreadmessage)
         noreadmessage-customerinfo (map #(conj % {:userinfo (db/get-customer-byid  (ObjectId. (:fromid %)))}) noreadmessage-customer)
         noreadrecommend-userinfo (map #(noreadrecommend-process %) noreadrecommend)
        ]
    ;(println )
    (send! channel (json/write-str {:type "factorychat" :data
    (concat noreadmessage-userinfo noreadmessage-customerinfo)} ) false)

    (send! channel (json/write-str {:type "recommend" :data noreadrecommend-userinfo} ) false)

    (db/update-message  {:toid id} {:isread true})
    (if (= 1 readtype) (db/update-recommend   {:factoryid id} {:isreadbyfactory true})
      (db/update-recommend   {:customerid id} {:isreadbycustomer true}))
    )

  )

(defn updatefactorylocation [lon lat factoryid]

  (println lon lat factoryid)

  (try
    (do
      (db/update-factory {:_id (ObjectId. factoryid)} {:loc.coordinates
                                                     [ (read-string lon)
                                                       (read-string lat) ] })
      (resp/json {:success true })
      )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      ))

  )
(defn getquickapplying [factoryid channel-hub-key]
  (let [
         oldtime (t/plus (l/local-now) (t/minutes commonfunc/applyquicktime) )
         applynoread (db/get-applyingquick-list {:factoryid factoryid
                                          :applytime
                                          { "$gte" oldtime }
                                          :isread false })

         filterapply (filter (fn [x]
                               (nil? (db/get-applyingquick {:customerid (:customerid x)
                                                            :applytime
                                                            { "$gte" oldtime }
                                                            :isaccept true }) ))
                       applynoread)

         channel (get @channel-hub-key factoryid)

         ]



    (dorun (map #(do
             (send! channel (json/write-str {:type "customerquickapply"
                                            :data (conj % {:userinfo (db/get-customer-byid (ObjectId. (:customerid %)))})} ) false)
                   (db/update-applyfactorys {:_id (:_id %)} {:isread true})
            )
      filterapply))


    )


  )

(defn acceptquickapply [rid customerid factoryid addmoney channel-hub-key]

  (try
    (let [

           oldtime (t/plus (l/local-now) (t/minutes commonfunc/applyquicktime) )

           applytrue (db/get-applyingquick {:customerid customerid
                                            :applytime
                                            { "$gte" oldtime }
                                            :isaccept true })

           addmoney (read-string addmoney)

           channel (get @channel-hub-key customerid)
           ]
      (if (nil? applytrue)
        (let [
               money (db/get-money-byid factoryid)
               totalmoney (:totalmoney money)
               totalmoney (if (nil? totalmoney) 0 totalmoney)
               customermoney (db/get-money-byid customerid)
               ptotalmoney (:totalmoney customermoney)
               ptotalmoney (if (nil? ptotalmoney) 0 ptotalmoney)
               needmoney (+ addmoney commonfunc/quickapplymoney)
               ]
          (db/update-applyfactorys {:_id (ObjectId. rid)} {:isaccept true} )

          (db/update-money-byid {:userid factoryid} {:totalmoney (+ totalmoney needmoney)})
          (db/update-money-byid {:userid customerid} {:totalmoney (- ptotalmoney needmoney)})

          (db/make-apply-by-pid-dic {:applyid customerid :factoryid factoryid}
            {:applyid customerid :needmoney needmoney :isreply false :factoryid factoryid :applytime (l/local-now) :ispay true})

          (when-not (nil? channel)
            (send! channel (json/write-str {:type "quickaccept" :data (db/get-factory-byid (ObjectId. factoryid))} ) false)
            )
          (resp/json {:success true})
          )
        (resp/json {:success false :msg "已被其他医生抢救了"})
        )


      )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )
    )


  )




(defn acceptrecommend [rid type channel-hub-key]
  (try
    (do
      (let [
              update (if (= 1 type) (db/update-recommend  {:_id (ObjectId. rid)} {:isfactoryaccepted true} )
                       (db/update-recommend  {:_id (ObjectId. rid)} {:iscustomeraccepted true} )
                       )
              updateobj (db/findrecommend {:_id (ObjectId. rid)})
             ]
        (future (sendrecommendconfirm updateobj channel-hub-key))
        (resp/json {:success true})
        )

      )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      ))
  )

(defn sendrecommendconfirm [recommend channel-hub-key]

  (println "sendrecommendconfirm")
  (println recommend)
  (when (and (:isfactoryaccepted recommend) (:iscustomeraccepted recommend))

     (let [
           customerid (:customerid recommend)

           factoryid (:factoryid recommend)

           info (noreadrecommend-process recommend)

           channelp (get @channel-hub-key customerid)

           channeld (get @channel-hub-key factoryid)

           ]

       (db/makefactorysvscustomers {:factoryid factoryid :customerid customerid} {:factoryid factoryid :customerid customerid})



       (chatprocess {:type "factorychat" :fromtype 0
                     :from customerid :to factoryid
                     :content "已添加您作为我的医生" :imgid -1} channel-hub-key)


       (chatprocess {:type "factorychat" :fromtype 1
                     :from factoryid :to customerid
                     :content "已添加您作为我的患者" :imgid -1} channel-hub-key)




       #_(when-not (nil? channelp)
         (send! channelp (json/write-str {:type "recommendconfirm" :data info} ) false)
         ;(db/update-recommend  {:_id recommendid} {:isreadbycustomer true} )
         )

       #_(when-not (nil? channeld)
         (send! channeld (json/write-str {:type "recommendconfirm" :data info} ) false)
         ;(db/update-recommend  {:_id recommendid} {:isreadbyfactory true} )
         )

      )

    )



  )

(defn getmycustomer [factoryid]
  (resp/json (db/get-relation-customer {:factoryid factoryid}))
  )


;;factory recommend
(defn sendmycustomerTofactory [customerid factoryid fromfactoryid text rectype channel-hub-key ]

    (try

      (do
        (if (> (count (db/get-relation-customer {:customerid customerid :factoryid factoryid})) 0)
          (resp/json {:success false :message "关系已建立，无需推荐"})
          (let [
                 channelp (get @channel-hub-key customerid)
                 channeld (get @channel-hub-key factoryid)
                 recommend (db/makerecommend {:customerid customerid :factoryid factoryid} {:customerid customerid :factoryid factoryid :fromid fromfactoryid
                                                                                        :isfactoryaccepted false :iscustomeraccepted false :rectype rectype
                                                                                            :text text
                                                                                        :isreadbyfactory false :isreadbycustomer false})

                 recommendmap (db/findrecommend {:customerid customerid :factoryid factoryid})
                 recommendid (:_id recommendmap)

                 customer (db/get-customer-byid (ObjectId. customerid))
                 factory (db/get-factory-byid  (ObjectId. factoryid))
                 ]

            (when-not (nil? channelp)
              (println "channelp")
              (send! channelp (json/write-str {:type "recommend" :data [(noreadrecommend-process recommendmap)]} ) false)
              (db/update-recommend  {:_id recommendid} {:isreadbycustomer true} )
              )

            (when-not (nil? channeld)
              (println "channeld")
              (send! channeld (json/write-str {:type "recommend" :data [(noreadrecommend-process recommendmap)]} ) false)
              (db/update-recommend  {:_id recommendid} {:isreadbyfactory true} )
              )
            (resp/json {:success true})
            )

          )

        )
      (catch Exception ex
        (println (.getMessage ex))
        (resp/json {:success false :message (.getMessage ex)})
        ))
  )


;; send message to my customer
(defn sendmsgtocustomer [channel-hub-key factoryid customerid message]
  (let [
         channel (get @channel-hub-key customerid)
         message {:content message :fromid factoryid :fromtype 1 :toid customerid :msgtime (l/local-now) :isread false}
         newmessage (db/create-message message)
         messagid (:_id newmessage)
         user (db/get-factory-byid  (ObjectId. factoryid))
         ]

    (try
      (do
          (when-not (nil? channel)
            (send! channel (json/write-str {:type "factorychat" :data [(conj message {:userinfo (:userinfo user)})]} ) false)
            (db/update-message  {:_id messagid} {:isread true} )
            )
        {:success true}
        )
      (catch Exception ex
        (println (.getMessage ex))
        {:success false :message (.getMessage ex)}
        ))

    )

  )

;; add black list
(defn addblacklist [customerid factoryid]
  (try
    (do
      (db/createblacklist {:factoryid factoryid :customerid customerid} {:factoryid factoryid :customerid customerid})

      (resp/json {:success true})
      )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      ))

  )

;; chat process func begin here
(defn chatprocess [data  channel-hub-key]
  (println "111111")
;;{type chatfactory, from 551b4cb83b83719a9aba9c01, to 551b4e1d31ad8b836c655377, content 1212}
    (let [ ctype (get data "ctype")
           ctype (if (nil? ctype )(get data (keyword "ctype"))ctype)

           from (get data "from")
           from (if (nil? from )(get data (keyword "from"))from)

           to   (get data "to")
           to   (if (nil? to )(get data (keyword "to"))to)

           content (get data "content")
           content  (if (nil? content )(get data (keyword "content"))content)

           fromtype (get data "fromtype")
           fromtype (if (nil? fromtype )(get data (keyword "fromtype"))fromtype)

           imgid (get data "imgid")
           imgid (if (nil? imgid )(get data (keyword "imgid"))imgid)

           message {:content content :fromid from :toid to
                    :msgtime (l/local-now) :isread false
                    :fromtype fromtype :type ctype
                    }
        ]
      (println "data" message)
     (try
          (do

             (let [
                 newmessage (db/create-message message)
                 messagid (:_id newmessage)
                 user (if (= fromtype 1) (:userinfo (db/get-factory-byid  (ObjectId. from))) (db/get-customer-byid  (ObjectId. from)))
                 channel (get @channel-hub-key to)
                 channelfrom (get @channel-hub-key from)
             ]
               (println "channel" "channelfrom" channel  channelfrom)
               (when-not (nil? channel)
                (send! channel (json/write-str {:type "factorychat" :data [(conj newmessage {:userinfo  user})]} ) false)

                 (db/update-message  {:_id messagid} {:isread true} )
               )

               (when-not (nil? channelfrom)
                 (send! channelfrom (json/write-str {:type "chatsuc" :data {:imgid imgid :toid to}} ) false)
                 ;(db/update-message  {:_id messagid} {:isread true} )
                 )


             )
            (resp/json {:success true})
            )
          (catch Exception ex
          (println (.getMessage ex))
            (resp/json {:success false :message (.getMessage ex)})
            ))

    ;;(json/write-str a :value-fn common/write-ObjectId)
    ;(resp/json [{:foo "bar"}])

    )

  )

(defn newfactory [factoryname factoryaddress factoryinfo  username realname password]

  (try
    (let [

           factory (db/get-factory-byusername username)
           factoryinfo (db/get-factory-byfactoryname factoryname)


           ]

      (if (and (nil? factoryinfo)(nil? factory)) (let [
                                                     newfactory (db/make-new-factory
                                                                  {:factoryname factoryname
                                                                   :factoryaddress factoryaddress
                                                                   :factoryinfo factoryinfo
                                                                   })
                                                     ]
                                                (resp/json {:success true :message (db/make-new-factory-user
                                                                                     {:username username
                                                                                      :factoryid (str (:_id newfactory))
                                                                                      :realname realname
                                                                                      :password password
                                                                                      :usertype 0
                                                                                      })})
                                                ) (resp/json {:success true :message "用户已存在"}))


      )

    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )

    )

  )

(defn getgoodsbyfid [factoryid]
  (resp/json (db/get-goods-by-cond {:factoryid factoryid}))
  )

(defn getgoodsbykeyword  [keyword page limit]

  (resp/json (db/get-goods-by-keyword keyword (read-string page) (read-string limit)))

  )
(defn addgoodsbyfid  [factoryid goodsname price unit colors imgs]

  (try
    (do (db/make-new-goods {:factoryid factoryid :goodsname goodsname
                        :price price :unit unit :colors colors :imgs imgs})
      (resp/json {:success true})
      )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )

    )

  )
(defn altergoodsbyfid  [gid goodsname price unit colors imgs]

  (try
    (do (db/alter-goods { :goodsname goodsname
                        :price price :unit unit :colors colors :imgs imgs} {:_id (ObjectId. gid)})
      (resp/json {:success true})
      )
    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )

    )

  )

(defn addfactorybyid [fromid toid channel-hub-key]
  (try
    (let [
           rels (db/get-relation-factory {$or [{:factoryid fromid :rid  toid} {:factoryid toid :rid fromid}]})


           channel (get @channel-hub-key toid)


           ]

      (if (> (count rels) 0) (resp/json {:success false :message  "关系已经存在"} ) (

                  do

                    (db/makefactorysvsfactorys {:factoryid fromid :rid toid :rtime (l/local-now)} )

                    (future (send! channel (json/write-str {:type "scanadd"  :data (conj {:fromtype 1} (db/get-factory-byid (ObjectId. fromid)))} ) false))

                  (future (do (chatprocess {:type "factorychat" :fromtype 1
                                :from fromid :to toid
                                :content "已添加您为医生好友!" :imgid -1} channel-hub-key)
                            (chatprocess {:type "factorychat" :fromtype 1
                                          :from toid :to fromid
                                          :content "已添加您为医生好友!" :imgid -1} channel-hub-key)

                            ))



                    (resp/json {:success true})



                                                                                ))


      )

    (catch Exception ex
      (println (.getMessage ex))
      (resp/json {:success false :message (.getMessage ex)})
      )

    )

  )


