(ns jhimanka.sjarvireitti
  (:require
   [org.httpkit.client :as hk-client]
   [jsonista.core :as j]
   [java-time :as jt]
   [nextjournal.clerk :as clerk])
  (:gen-class))

(def datapoints_58 {4017  "Tampere Atala"
                    4003  "Orivesi Kutema"
                    9012  "Keuruu Tiusala"
                    9029  "Multia Väätäiskylä"
                    9009  "Saarijärvi Kalmari"})
(def datapoints_9 {4017  "Tampere Atala"
                   4003  "Orivesi Kutema"
                   4031 "Orivesi Leppähammas"
                   9016 "Jämsä Ratalammi"
                   9017 "Jämsä Partalanmäki"
                   9002 "Jyväskylä Korpilahti"
                   9021 "Muurame Keljonkangas"
                   9015 "Äänekoski Honkola"
                   9009  "Saarijärvi Kalmari"})

(def datapoints_65 {4085 "Ylöjärvi Kuru"
                    4016 "Virrat Puttoskylä"})

(def sensors #{"ILMA" "ILMA_DERIVAATTA" "SADE" "SADE_INTENSITEETTI" "LUMEN_SYVYYS"
               "TIE_1" "TIE1_DERIVAATTA" "KELI_1" "SUOLAN_MÄÄRÄ_1" "KITKA1"
               "SATEEN_OLOMUOTO_PWDXX" "TURVALLISUUSLÄMPÖ_1" "SADEMÄÄRÄ_1H_ENNUSTE"})

(defn fetch-data [target]
  (-> @(hk-client/request 
         {:url (str "https://tie.digitraffic.fi/api/weather/v1/stations/" target "/data")
          :method :get
          :headers {"Accept-Encoding" ["gzip" "deflate" "br" "zstd"]
                    "Digitraffic-User" "JH/Testing 0.1"}
          :keepalive 5000})
    (:body)
    (j/read-value j/keyword-keys-object-mapper)))

(defn interesting-sensor? [sensor]
  (contains? sensors (:name sensor)))

(defn keep-relevant [item]
  [(:name item) (or (:sensorValueDescriptionFi item)
                    (str (:value item) " " (:unit item)))])

(def xform (comp (filter interesting-sensor?)
                 (map keep-relevant)
                 (remove nil?)))

(defn output-station "Output data from a single station"
  [target]
  (let [{:keys [dataUpdatedTime sensorValues]} (fetch-data target)
        payload (eduction xform sensorValues)]
    {:measuredtime dataUpdatedTime :payload payload}))

(defn station-table [dataset]
  (for [sensor dataset
        :let [dada (output-station (key sensor))]]
    {(str (val sensor) " " (jt/local-date-time (jt/zoned-date-time
                                                (:measuredtime dada))
                                               (str "GMT" (jt/zone-offset))))
     (clerk/table (:payload dada))}))

(station-table datapoints_58)
(station-table datapoints_9)
(station-table datapoints_65)


