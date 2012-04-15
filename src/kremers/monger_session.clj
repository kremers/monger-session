(ns kremers.monger-session
  (:require [monger.collection :as mng])
  (:use [ring.middleware.session.store :as ringstore])
  (:import [java.util UUID Date]))

(deftype MongodbStore [collection-name auto-key-change?]
  ringstore/SessionStore
  (read-session [_ key] (if (nil? key) {} 
                (if-let [entity (mng/find-one-as-map collection-name {:_id key})]
                  entity {})))
  (write-session [_ key data]
                 (let  [entity (if (nil? key) nil (mng/find-one-as-map collection-name {:_id key}))
                       key-change? (or (= nil entity) auto-key-change?)
                       newkey (if key-change? (str (UUID/randomUUID)) key)]
                   (if entity
                     (do (if key-change?
                           (do (mng/remove collection-name {:_id key})
                               (mng/insert collection-name (assoc data :_id newkey :_date (:_date entity))))
                           (mng/update collection-name {:_id newkey} (assoc data :_date (:_date entity))))
                         newkey)
                     (do (mng/insert collection-name (merge data { :_id newkey :_date (Date.) }))
                         newkey))))
  (delete-session [_ key]
                  (mng/remove collection-name {:_id key})
                  nil)
)

(defn mongodb-store
  ([] (mongodb-store {}))
  ([opt]
     (let [collection-name (opt :collection-name "ring_sessions")
           auto-key-change? (opt :auto-key-change? false)]
       (MongodbStore. collection-name auto-key-change?))))

