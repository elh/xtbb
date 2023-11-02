# xtbb

A simple XTDB querying CLI using Babashka.

* Queries are written in your preferred editor.
* Results can be printed in a few formats: tabular, as zipped maps, raw.
* Error contents in EDN are pretty printed.

<br>

### Usage

Run xtbb! Interactively query like a repl.
```plaintext
$ xtbb
xtbb - {:url http://localhost:9999/_xtdb/query, :dir /tmp/xtbb, :editor vim, :format tabular}

[Enter] to query, [Ctrl-C] to quit

"Elapsed time: 17.101709 msecs"

|                          ?e |      ?n |
|-----------------------------+---------|
| 2XbejIwxoYKLlnKquM9RZnZvV9w |   Aaron |
| 2XbejCWZcq2QLlK1F32DHaaTp2W |    Alex |
| 2XbejDmUFWGgtQ1XAlrCfbWiX05 |  Amelia |
| 2XbejD58l9qjVrTPVyo4r1WB9Kj | Antoine |
| 2XbejI1Phei9D3tGSCW3HRaiCku |  Aurora |

5 results

[Enter] to query, [Ctrl-C] to quit
```

Write your queries in an editor.
```clojure
;; xtbb - A simple XTDB querying CLI
;; Save a query in this file to run it

{:find [?e ?n],
 :where
 [[?e :xt/id]
  [?e :name ?n]
  [?e :active true]],
 :order-by [[?n :asc]],
 :limit 5}
```

Flags:
* `--url`:           XTDB REST API URL. Defaults to the xtdb-in-a-box default URL.
* `--editor`:        Editor to use for writing the query. Defaults to $EDITOR or "vim".
* `--dir`:           Dir to save app data to like the working query and query history. Defaults to "/tmp/xtbb".
* `--format`:        Result print formatting. Options: "tabular", "maps", "raw". Defaults to "tabular".

<br>

### Installation

The only dependency is Babashka.

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
