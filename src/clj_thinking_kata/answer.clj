(ns clj-thinking-kata.answer
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [datomic.client.api :as d]))

; region Batman

(def batman
  (conj {}
    [:person/id #uuid"1698c71a-c04a-417a-9515-dd53fc4eb006"]
    [:person/name "Bruce"]
    [:person/surname "Wayne"]
    [:person/age 33]
    [:person.height/meters 1.86]
    [:person/social-security-number "123-45-6789"]
    [:person/enemies [{:person/id      #uuid"bc350473-b401-47da-88df-205255595a56"
                       :person/name    "Sue"
                       :person/surname "Smith"}]]
    [:shield/id #uuid"68a2e448-0857-405e-85a6-856fc265a887"]
    [:super-hero/name "Batman"]
    [:super-hero/enemies [{:villian/name         "The Joker"
                           :person.height/meters 1.92
                           :shield/id            #uuid"7699388b-b84d-4824-96d0-303a419d018d"}]]))

; endregion

; region Specs

(s/def :person/id uuid?)
(s/def :person/name string?)
(s/def :person/surname string?)
(s/def :person/age (s/int-in 20 60))
(s/def :person.height/meters (s/double-in :min 1.2 :max 2.1 :NaN? false :inifinite? false))
(s/def :person/social-security-number string?)

(s/def ::person (s/keys
                  :req [:person/id :person/name :person/surname]
                  :opt [:person/age :person.height/meters :person/social-security-number
                        :person/enemies]))

(s/def ::super-hero (s/keys
                      :req [:shield/id :super-hero/name :super-hero/enemies]))
(s/def ::villian (s/keys
                   :req [:shield/id
                         :villian/name]))

(s/def :person/enemies (s/coll-of ::person :kind vector?))
(s/def :super-hero/enemies (s/coll-of ::villian :kind vector?))

(s/def :shield/id uuid?)
(s/def :super-hero/name string?)

(gen/generate (s/gen ::person))

; endregion

; region Database

(def client (d/client
              {:server-type :dev-local
               :storage-dir :mem
               :system      "thinking"}))

(def db-name (gen/generate (s/gen string?)))
(d/create-database client {:db-name db-name})

(def conn (d/connect client {:db-name db-name}))

(def db (d/db conn))

; endregion

; region Schema

(def schema
  [{:db/ident       :person/name
    :db/doc         "A person's name"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}

   {:db/ident       :person/surname
    :db/doc         "A person's surname"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}

   {:db/ident       :person/age
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/long}

   {:db/ident       :person.height/meters
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/double}

   {:db/ident       :super-hero/name
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity}

   {:db/ident       :villian/name
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity}

   {:db/ident       :shield/id
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/uuid
    :db/unique      :db.unique/identity}

   {:db/ident       :person/id
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/uuid
    :db/unique      :db.unique/identity}

   {:db/ident       :person/social-security-number
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity}

   {:db/ident       :person/enemies
    :db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}

   {:db/ident       :super-hero/enemies
    :db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}])

; endregion

; region Transact and query

(d/transact conn {:tx-data schema})

(d/transact conn {:tx-data [{:person/id                     #uuid"1698c71a-c04a-417a-9515-dd53fc4eb006"
                             :person/name                   "Bruce"
                             :person/surname                "Wayne"
                             :person/age                    33
                             :person.height/meters          1.86
                             :person/social-security-number "123-45-6789"
                             :person/enemies                {:person/id      #uuid"bc350473-b401-47da-88df-205255595a56"
                                                             :person/name    "Sue"
                                                             :person/surname "Smith"}
                             :shield/id                     #uuid"68a2e448-0857-405e-85a6-856fc265a887"
                             :super-hero/name               "Batman"
                             :super-hero/enemies            {:villian/name         "The Joker"
                                                             :person.height/meters 1.92
                                                             :shield/id            #uuid"7699388b-b84d-4824-96d0-303a419d018d"}}]})

(d/transact conn {:tx-data [(gen/generate (s/gen ::person))]})

(d/pull
  (d/db conn)
  '[*
    {:person/enemies [*]}
    {:super-hero/enemies [*]}]
  (find batman :person/id))

(d/index-pull
  (d/db conn)
  {:index    :avet
   :selector '[*
               {:person/enemies [*]}
               {:super-hero/enemies [*]}]
   :start    [:person/id]})

(tap> (d/index-pull
        (d/db conn)
        {:index    :avet
         :selector '[*]
         :start    [:person/id]}))
(d/pull
  (d/db conn)
  '[*
    {:person/_enemies [*]}]
  [:person/id #uuid"bc350473-b401-47da-88df-205255595a56"])

; endregion