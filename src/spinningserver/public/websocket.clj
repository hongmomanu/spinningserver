(ns spinningserver.public.websocket
      (:use org.httpkit.server)
  (:require
            [clojure.data.json :as json]
            [spinningserver.controller.factory :as factory]
            [spinningserver.controller.customer :as customer]
            )
)

(def channel-hub (atom {}))
(def channel-hub-key (atom {}))

(defn handler [request]
  (with-channel request channel
    ;; Store the channel somewhere, and use it to sent response to client when interesting event happened
    ;;(swap! channel-hub assoc channel nil)
    (on-receive channel (fn [data]


                            (let [cdata  (json/read-str data)
                                  type    (get cdata "type")
                                  content (get cdata "content")
                            ]
                            (cond (= "factoryconnect" type) (do
                                                        (swap! channel-hub assoc channel content )
                                                        (swap! channel-hub-key assoc content channel )
                                                        (factory/getnoread content 1 channel-hub-key)
                                                        (factory/getquickapplying content channel-hub-key)
                                                        )
                                (= "customerconnect" type)(
                                                           do
                                                           (swap! channel-hub assoc channel content )
                                                           (swap! channel-hub-key assoc content channel )
                                                           (factory/getnoread content 0 channel-hub-key)
                                                           (customer/getquickapplying content channel-hub-key)
                                                           (customer/getquickaccept content channel-hub-key)

                                                           )

                              (= "factorychat" type)(do
                                                     (factory/chatprocess cdata channel-hub-key)
                                                     )

                                 :else (factory/chatprocess cdata channel-hub-key))
                               (println "mumumu" channel)


                            )

                               ;(println request)
                              ;(send! channel data)
                              ))
    (on-close channel (fn [status]
                        ;; remove from hub when channel get closed
                        (let [chanel-key (get @channel-hub channel)]


                        (println channel " disconnected. status: " status " channel-key" chanel-key)
                        (swap! channel-hub dissoc channel)
                        (swap! channel-hub-key dissoc chanel-key)
                        )


                        ))))




(defn start-server [port]
  (run-server handler {:port port :max-body 16388608 :max-line 16388608})
  )








