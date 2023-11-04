#!/usr/bin/env bb

;;;; xtbb - a simple XTDB querying CLI

(require '[babashka.cli :as cli]
         '[babashka.http-client :as http]
         '[babashka.process :refer [shell]]
         '[clojure.edn :as edn]
         '[clojure.string :as s]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

;;; Options

;; `--url`:           XTDB REST API URL. Defaults to the xtdb-in-a-box default URL.
;; `--editor`:        Editor to use for writing the query. Defaults to $EDITOR or "vim".
;; `--dir`:           Dir to save app data to like the working query and query history. Defaults to "/tmp/xtbb".
;; `--format`:        Result print formatting. Options: "tabular", "maps", "raw". Defaults to "tabular".
(def default-opts {:url "http://localhost:9999/_xtdb/query"
                   :dir "/tmp/xtbb"
                   :editor (or (System/getenv "EDITOR")
                               "vim")
                   :format "tabular"})

(def opts (merge default-opts (cli/parse-opts *command-line-args*)))

(def query-file (format "%s/query.edn" (:dir opts)))
(def query-history-dir (format "%s/history" (:dir opts)))

;;; Functions

(defn get-input []
  (println "\n[Enter] to query, [Ctrl-C] to quit")
  (read-line))

(defn unix-time []
  (quot (System/currentTimeMillis) 1000))

(defn pp-str [x]
  (with-out-str (pp/pprint x)))

(defn print-err [resp]
  (println)
  (println "Status:" (:status resp))
  (println "Body:")
  (try (pp/pprint (edn/read-string (:body resp)))
       (catch Exception _ (println (:body resp)))))

;; NOTE: pp/print-table doesn't print great when contents have newlines. for objects, just use other formats
;; (defn print-tabular [maps]
;;   (let [formatted (map #(into {} (for [[k v] %] [k (try (pp-str v)
;;                                                         (catch Exception e (println e) (println v) v))]))
;;                        maps)]
;;     (pp/print-table formatted)))

(def default-query-file (s/trim "
;; xtbb - A simple XTDB querying CLI
;; Save a query edn map in this file to run it
;; -----------------------------------------------------------------------------
;;
;; ;; Provide just the query map...
;; {:find [?e]
;;  :where [[?e :xt/id]]
;;  :limit 10}
;;
;; ;; ... Or provide a map with :query, :in-args, :valid-time, :tx-time, :tx-id
;; ;; This format emulates the HTTP API
;; {:query {:find [?e]
;;          :in [?n]
;;          :where [[?e :xt/id] [?e :name ?n]]
;;          :limit 5}
;;  :in-args [\"Amelia\"]
;;  :valid-time \"2023-11-01T21:23:00Z\"}

{:find [?e]
 :where [[?e :xt/id]]
 :limit 10}
"))

(defn query-loop []
  (io/make-parents query-file)
  (loop []
    ;; start next query
    (get-input)
    ;; if query file DNE, create it with some default instructions
    (when (not (.exists (io/file query-file)))
      (spit query-file default-query-file))
    ;; edit query in a working file
    (let [in-str (let [f (io/file query-file)]
                      (shell (:editor opts) (.getPath f))
                      (slurp f))
          in (try (edn/read-string in-str)
                     (catch Exception _ nil))]
      (pp/pprint in)
      (if (not (map? in))
        (do
          (println "Error: Input must be a valid edn map")
          (recur))
        ;; query XTDB
        (let [args (if (:query in) in {:query in})
              resp (time (http/post (:url opts)
                                    {:headers {:content-type "application/edn"}
                                     :query-params (select-keys args [:valid-time :tx-time :tx-id])
                                     :body (str (select-keys args [:query :in-args]))
                                     :throw false}))]
          (if (not= 200 (:status resp))
            (do
              (print-err resp)
              (recur))
            (let [resp-body (edn/read-string (:body resp))
                  resp-maps (map (fn [m] (zipmap (get-in args [:query :find]) m)) resp-body)
                  history-file (format "%s/%s.edn" query-history-dir (unix-time))]

              ;; save query history
              (io/make-parents history-file)
              (spit history-file (pp-str in))

              ;; print results
              (case (:format opts)
                "tabular" (pp/print-table resp-maps)
                "maps" (pp/pprint resp-maps)
                "raw" (pp/pprint resp-body))
              (println (format "\n%d results" (count resp-maps)))

              (recur))))))))

;;; Main

(println "xtbb -" opts)
(query-loop)
