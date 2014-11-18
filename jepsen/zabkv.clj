(ns jepsen.zabkv
  (:require [clojure.string :as string])
  (:require [clj-http.client :as client])
  (:require [clojure.data.json :as json])
  (:use [clojure.set :only [union difference]]
        [jepsen.control.net :only [hosts-map]]
        jepsen.set-app
        jepsen.load)
  )

(defn convert-to-list
 [dict]
 conj []
  (for [[k v] dict] (Integer/parseInt k)))

(defn zabkv-app
  [opts]
  (let [author "yisheng"
        db "zabkv"])

    (reify SetApp
      (setup [app]
             (println "Hello world" (:n1 jepsen.control.net/hosts-map)))

      (add [app element]
        (let [dest (String/format "http://n%d:8080" (into-array [(+ 1 (mod element 5))]))]
          (try
             (client/put dest {:body (String/format "{%d : 1}" (into-array [element]))})
             (catch clojure.lang.ExceptionInfo e error))))

      (results [app]
               (let [response ((client/get "http://n5:8080") :body)]
                (convert-to-list (json/read-str response))))

      (teardown [app]
                (println "Tear down"))))
