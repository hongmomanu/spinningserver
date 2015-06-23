(ns spinningserver.handler
  (:require [compojure.core :refer [defroutes routes wrap-routes]]
            [spinningserver.routes.home :refer [home-routes]]

            [spinningserver.routes.user :refer [user-routes]]
            [spinningserver.routes.factory :refer [factory-routes]]
            [spinningserver.routes.customer :refer [customer-routes]]
            [spinningserver.public.websocket :as websocket]
            
            [spinningserver.middleware :as middleware]
            [spinningserver.session :as session]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]
            [clojure.tools.nrepl.server :as nrepl]))

(defonce nrepl-server (atom nil))

(defroutes base-routes
           (route/resources "/")
           (route/not-found "Not Found"))

(defn start-nrepl
  "Start a network repl for debugging when the :repl-port is set in the environment."
  []
  (when-let [port (env :repl-port)]
    (try
      (reset! nrepl-server (nrepl/start-server :port port))
      (timbre/info "nREPL server started on port" port)
      (catch Throwable t
        (timbre/error "failed to start nREPL" t)))))

(defn stop-nrepl []
  (when-let [server @nrepl-server]
    (nrepl/stop-server server)))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []

  (timbre/set-config!
    [:appenders :rotor]
    {:min-level             (if (env :dev) :trace :info)
     :enabled?              true
     :async?                false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn                    rotor/appender-fn})

  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "spinningserver.log" :max-size (* 512 1024) :backlog 10})

  (if (env :dev) (parser/cache-off!))
  (start-nrepl)
  ;;start the expired session cleanup job
  (session/start-cleanup-job!)
  (timbre/info "\n-=[ spinningserver started successfully"
               (when (env :dev) "using the development profile") "]=-")

  (websocket/start-server 3003)
  )

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "spinningserver is shutting down...")
  (stop-nrepl)
  (timbre/info "shutdown complete!"))

(def app
  (-> (routes
        (wrap-routes [home-routes user-routes
          factory-routes customer-routes] middleware/wrap-csrf)
        base-routes)
      middleware/wrap-base))