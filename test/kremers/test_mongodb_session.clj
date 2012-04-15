(ns kremers.test-mongodb-session
  (:use [clojure.test]
	[ring.middleware.session.store]
	[kremers.monger-session]
        [monger.core :only [command connect! connect set-db! get-db]]
))

(def dbname "test-mongodb-sessions")
(defn connect-to-db! [] (connect!) (monger.core/set-db! (monger.core/get-db dbname)))

(defn server-fixture [f] 
  (do (connect!) (monger.core/set-db! (monger.core/get-db dbname)))
  (f)
  (command { :dropDatabase 1 })
)

(use-fixtures :each server-fixture)

(deftest read-not-exist
  (let [store (mongodb-store)]
    (is (read-session store "non-existent")
	{})))

(deftest session-create
  (let [store    (mongodb-store)
	sess-key (write-session store nil {:foo "bar"})
	entity   (read-session store sess-key)]
    (is (not (nil? sess-key)))
    (is (and (:_id entity) (:_date entity)))
    (is (= (dissoc entity :_id :_date)
	   {:foo "bar"}))))

(deftest session-update
  (let [store     (mongodb-store)
	sess-key  (write-session store nil {:foo "bar"})
	sess-key* (write-session store sess-key {:bar "baz"})
	entity    (read-session store sess-key*)]
    (is (= sess-key sess-key*))
    (is (:_id entity))
    (is (= (dissoc entity :_id :_date)
	   {:bar "baz"}))))

(deftest session-auto-key-change
  (let [store     (mongodb-store {:auto-key-change? true})
	sess-key  (write-session store nil {:foo "bar"})
	sess-key* (write-session store sess-key {:bar "baz"})
	entity    (read-session store sess-key*)]
    (is (not= sess-key sess-key*))
    (is (:_id entity))
    ;(:_date entity)
    (is (= (dissoc entity :_id :_date)
	   {:bar "baz"}))))

(deftest session-delete
  (let [store    (mongodb-store)
	sess-key (write-session store nil {:foo "bar"})]
    (is (nil? (delete-session store sess-key)))
    (is (= (read-session store sess-key)
	   {}))))
