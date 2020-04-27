
Each `Push` will record fingerprints for the `after` Commit.  
Immediately after this skill has run, the query:

```
{
  Commit(sha: "$sha") {
    analysis {
      name
      data
      type
    }
  }
}
```

will return:

```
    "Commit": [
      {
        "analysis": [
          {
            "data": "[\n  \"com.atomist/api-cljs\",\n  \"0.3.30\"\n]",
            "name": "com.atomist::api-cljs"
            "type": "clojure-tools-deps"
          },
          {
            "data": "[\n  \"org.clojure/clojurescript\",\n  \"1.10.520\"\n]",
            "name": "org.clojure::clojurescript"
            "type": "clojure-tools-deps" 
          }
        ]
      }
    ]
```

### latest semver

These queries are not working.

```
{
  fingerprintAggregates(type: "clojure-tools-deps", name: "com.atomist::api-cljs") {
    totalRepos
    totalVariants
    latestSemVerUsed {
      fingerprint {
        data
      }
    }
    mostFrequentlyUsed {
      fingerprint {
        data
      }
    }
    latestSemVerAvailable {
      data
    }
    mostRecentlyUsed {
      fingerprint {
        data
      }
    }
  }
}
```

## Fingerprint Data

### Maven

```
type = maven-direct-dep
{
  "group": "atomist",
  "artifact": "common",
  "version": "1.35"
}
```

### Npm

```
type = npm-project-dep
["lib" "version"]
```

## Policy



### Maven

```
manual:
["group:artifact:version",...]
latest:
["group:artifact",...]
```

### Npm

```
manual:
{"express": "4.17.1"}
latest:
["express",...]
```

### Leiningen
```
manual:
[["library" "version"] ...]
latest:
["library" ...]
```

### Tools deps.edn

```
manual:
[["library" "version"] ...]
latest:
["library" ...]
```

