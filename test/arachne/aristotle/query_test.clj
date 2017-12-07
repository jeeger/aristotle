(ns arachne.aristotle.query-test
  (:require [clojure.test :refer :all]
            [arachne.aristotle.registry :as reg]
            [arachne.aristotle.graph :as graph]
            [arachne.aristotle.query :as q]
            [arachne.aristotle :as ar]
            [clojure.java.io :as io]))

(reg/prefix :foaf "http://xmlns.com/foaf/0.1/")
(reg/prefix :arachne "http://example.com/person/")
(reg/prefix :socrata "http://www.socrata.com/rdf/terms#")
(reg/prefix :dcat "http://www.w3.org/ns/dcat#")
(reg/prefix :ods "http://open-data-standards.github.com/2012/01/open-data-standards#")
(reg/prefix :dcterm "http://purl.org/dc/terms/")
(reg/prefix :geo "http://www.w3.org/2003/01/geo/wgs84_pos#")
(reg/prefix :skos "http://www.w3.org/2004/02/skos/core#")
(reg/prefix :dsbase "http://data.lacity.org/resource/")
(reg/prefix :ds "https://data.lacity.org/resource/zzzz-zzzz/")

(def test-graph (graph/load (io/resource "la_census.rdf")))

(deftest basic-query
  (is (= [["57110"]]
        (q/query '[:project [?pop]
                   [:bgp {:rdf/about ?e
                          :ds/zip_code "90001"
                          :ds/total_population ?pop}]]
          test-graph)))
  (is (= [["57110"]]
        (q/query '[:project [?pop]
                   [:bgp
                    [?e :ds/zip_code "90001"]
                    [?e :ds/total_population ?pop]]]
          test-graph)))
  (let [results (q/query '[:bgp
                           [?e :ds/zip_code "90001"]
                           [?e :ds/total_population ?pop]]
                  test-graph)]
    (is (= "57110" (get (first results) '?pop)))))

(deftest functions+filters
  (is (= [["90650"]]
       (q/query '[:project [?zip]
                  [:filter (< 105000 (:xsd/integer ?pop))
                   [:bgp
               [?e :ds/zip_code ?zip]
               [?e :ds/total_population ?pop]]]]
         test-graph))))

(deftest aggregates
  (is (= [[319 0 105549 33241]]
        (q/query '[:project [?count ?min ?max ?avg]
                   [:extend [?avg (round ?avgn)]
                    [:group [] [?count (count)
                                ?min (min (:xsd/integer ?pop))
                                ?max (max (:xsd/integer ?pop))
                                ?avgn (avg (:xsd/integer ?pop))]
                     [:bgp
                      [_ :ds/total_population ?pop]]]]]
          test-graph))))

(deftest minus
  (is (= 5 (count (q/query '[:diff
                         [:bgp [?zip :ds/total_population "0"]]
                         [:bgp [?zip :ds/zip_code "90831"]]]
                test-graph)))))

(deftest unions
  (is (= 2 (count (q/query '[:union
                             [:bgp [?zip :ds/zip_code "92821"]]
                             [:bgp [?zip :ds/zip_code "90831"]]]
                    test-graph)))))
