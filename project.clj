(defproject kremers/monger-session "1.0.0"
  :description "mongodb as http session storage (using monger instead of congomongo)"
  :dependencies [[org.clojure/clojure "1.4.0"]
		 [ring/ring-core "1.1.0-RC1"]
                 [com.novemberain/monger "1.0.0-SNAPSHOT"]])
