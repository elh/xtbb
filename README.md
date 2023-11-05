# `xtbb>`

A simple [XTDB](https://github.com/xtdb/xtdb) querying CLI.

* Queries can be written at the command line or in your preferred editor.
* Results can be printed in a few formats: tabular, as zipped maps, raw.
* Error contents in EDN are pretty printed.

<br>

### Usage

Run `xtbb`! Interactively query XTDB like a repl.
```plaintext
$ xtbb
xtbb - {:url http://localhost:9999/_xtdb/query, :query nil, :editor nil, :dir /tmp/xtbb, :format tabular}

xtbb> {:find [?e ?n] :where [[?e :xt/id][?e :name ?n][?e :active true]] :order-by [[?n :asc]] :limit 5}
"Elapsed time: 21.521875 msecs"

|                          ?e |      ?n |
|-----------------------------+---------|
| 2XbejIwxoYKLlnKquM9RZnZvV9w |   Aaron |
| 2XbejCWZcq2QLlK1F32DHaaTp2W |    Alex |
| 2XbejDmUFWGgtQ1XAlrCfbWiX05 |  Amelia |
| 2XbejD58l9qjVrTPVyo4r1WB9Kj | Antoine |
| 2XbejI1Phei9D3tGSCW3HRaiCku |  Aurora |

5 results

xtbb>
```

#### Editor mode

Write queries with your preferred editor. This is my favorite way to use it.
```plaintext
$ xtbb --editor vim
xtbb - {:url http://localhost:9999/_xtdb/query, :query nil, :editor vim, :dir /tmp/xtbb, :format tabular}

[Enter] to write query in editor, [Ctrl-C] to quit

{:find [?e ?n]
 :where [[?e :xt/id][?e :name ?n][?e :active true]]
 :order-by [[?n :asc]]
 :limit 5}
"Elapsed time: 17.101709 msecs"

|                          ?e |      ?n |
|-----------------------------+---------|
| 2XbejIwxoYKLlnKquM9RZnZvV9w |   Aaron |
| 2XbejCWZcq2QLlK1F32DHaaTp2W |    Alex |
| 2XbejDmUFWGgtQ1XAlrCfbWiX05 |  Amelia |
| 2XbejD58l9qjVrTPVyo4r1WB9Kj | Antoine |
| 2XbejI1Phei9D3tGSCW3HRaiCku |  Aurora |

5 results

[Enter] to write query in editor, [Ctrl-C] to quit
```

Example query file:
```clojure
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
;;  :in-args ["Amelia"]
;;  :valid-time "2023-11-01T21:23:00Z"}

{:find [?e ?n],
 :where
 [[?e :xt/id]
  [?e :name ?n]
  [?e :active true]],
 :order-by [[?n :asc]],
 :limit 5}
```

#### Non-interactive mode

Just execute a single query.
```plaintext
$ xtbb --query "{:find [?e], :where [[?e :xt/id]], :limit 10}"
```

#### Flags
* `--url`:           XTDB REST API URL. Defaults to the xtdb-in-a-box default URL.
* `--query`:         If a query is provided, just execute that query and exit. This disables default interactive mode.
* `--editor`:        If editor is provided (e.g. "vim"), it will be opened to edit the query as a file.
* `--history`:       If enabled, save a history of queries to a directory. Defaults to "<--dir>/history".
* `--dir`:           Dir to save app data to like the working query and query history. Defaults to "/tmp/xtbb".
* `--format`:        Result print formatting. Options: "tabular", "maps", "raw". Defaults to "tabular".

<br>

### Installation

The only dependency is [Babashka](https://github.com/babashka/babashka).

1. Install with bbin
```bash
bbin install https://raw.githubusercontent.com/elh/xtbb/main/xtbb.clj

xtbb
```

2. Install manually
```bash
# download file or clone repo

./xtbb.clj
```

<br>

### Motivation. Why not use the query GUI?
1. CLIs are fun. Why not both?
2. The GUI seems crash on some valid expressions used in the `:find`.
3. The GUI can be hard to read for verbose errors.

Querying from the clojure REPL is nice too, but I like having an opinionated tool that I can distribute as a CLI.

<br>

---
Contributions welcome!<br>
Copyright © 2023 Eugene Huang
