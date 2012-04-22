(ns kremers.monger-session
  (:require [monger.collection :as mng])
  (:use [ring.middleware.session.store :as ringstore])
  (:import [java.util UUID Date]))

(defmethod clojure.core/print-dup java.util.Date [o w]
          (.write w (str "#=(java.util.Date. " (.getTime o) ")")))

;(defn ser [data] (str (binding [*print-dup* true] (prn-str data))))
(defn ser [data] (prn-str data))

(deftype MongodbStore [collection-name auto-key-change?]
  ringstore/SessionStore
  (read-session [_ key] (if (nil? key) {} 
                (if-let [entity (mng/find-one-as-map collection-name {:_id key})] 
                   (read-string (:content entity)) {})))
  (write-session [_ key data]
                 (do  
                   (let  [;data (zipmap (map #(if (and (keyword? %) (namespace %))
                          ;   (-> % str (.substring 1)) %)
                          ;   (keys data))
                          ;   (vals data))
                       entity (if (nil? key) nil (mng/find-one-as-map collection-name {:_id key}))
                       key-change? (or (= nil entity) auto-key-change?)
                       newkey (if key-change? (str (UUID/randomUUID)) key)]
                   (if entity
                     (do (if key-change?
                           (do (mng/remove collection-name {:_id key})
                               (mng/insert collection-name {:_id newkey :content (ser (assoc data :_id newkey :_date (:_date entity)))}))
                           (mng/update collection-name {:_id newkey} {:_id newkey :content (ser (assoc data :_id newkey :_date (:_date entity)))}))
                         newkey)
                     (do (mng/insert collection-name {:_id newkey :content (ser (assoc data :_id newkey :_date (Date.)))})
                         newkey)))))
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

