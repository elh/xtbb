#!/usr/bin/env bb

;;;; xtbb - a simple XTDB querying CLI

;; TODO: support in-args, valid-time, tx-time, tx-id

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

(def default-query-file (s/trim "
;; xtbb - A simple XTDB querying CLI
;; Save a query in this file to run it

{:find [?e]
 :where [[?e :xt/id]]
 :limit 10}
"))

;; NOTE: pp/print-table doesn't print great when contents have newlines. for objects, just use other formats
;; (defn print-tabular [maps]
;;   (let [formatted (map #(into {} (for [[k v] %] [k (try (pp-str v)
;;                                                         (catch Exception e (println e) (println v) v))]))
;;                        maps)]
;;     (pp/print-table formatted)))

(defn query-loop []
  (io/make-parents query-file)
  (loop []
    ;; start next query
    (get-input)
    ;; if query file DNE, create it with some default instructions
    (when (not (.exists (io/file query-file)))
      (spit query-file default-query-file))
    ;; edit query in a working file
    (let [query-str (let [f (io/file query-file)]
                      (shell (:editor opts) (.getPath f))
                      (slurp f))
          query (try (edn/read-string query-str)
                     (catch Exception _ nil))]
      (if (not (map? query))
        (do
          (println "Error: Query must be a valid edn map")
          (recur))
        ;; query XTDB
        (let [resp (time (http/post (:url opts)
                                    {:headers {:content-type "application/edn"}
                                     :body (format "{:query %s}" query)
                                     :throw false}))]
          (if (not= 200 (:status resp))
            (do
              (print-err resp)
              (recur))
            (let [resp-body (edn/read-string (:body resp))
                  resp-maps (map (fn [m] (zipmap (:find query) m)) resp-body)
                  history-file (format "%s/%s.edn" query-history-dir (unix-time))]

              ;; save query history
              (io/make-parents history-file)
              (spit history-file (pp-str query))

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
