# scalagrammar

A hands-on, exhaustive tour of the Scala language — and the ecosystem around it —
where **every lesson is a single file you can run**.

```bash
# install scala-cli once: https://scala-cli.virtuslab.org/install
scala-cli run lessons/01-basics/01_hello.scala

# or use the helper (accepts a path or a fuzzy fragment)
./run.sh lessons/04-pattern-matching/01_patterns.scala
./run.sh variance            # runs the first lesson whose path contains "variance"
./run.sh --list              # list every lesson
./run.sh --all lessons/03-oop  # run every lesson under a directory (regression check)
```

Each file:

* starts with `//> using` directives (Scala version, dependencies, compiler options), so it needs
  no build file;
* lives in its own `package`, so files never clash;
* prints labelled sections — read the source and the output side by side;
* demonstrates **code that does not compile** by calling
  `scala.compiletime.testing.typeCheckErrors("...")` and printing the compiler's message.
  You see the compile error *at runtime* without the file itself failing.
  `typeCheckErrors` only sees errors found while parsing and type checking; errors from later
  compiler phases (e.g. `@tailrec` checks, some inheritance rules) are quoted in comments instead.

Target: **Scala 3.9.0** on JDK 17+. Scala 2 has its own section (`lessons/12-scala2`), because
much existing code and many libraries (Spark!) are still written in Scala 2.

---

## Curriculum

### Part I — The language (`lessons/`)

| # | Topic | Files |
|---|-------|-------|
| 01 | **Basics** | hello & entry points · values/vars/lazy · numeric types & literals · strings & interpolation · control flow · operators & precedence · syntax styles (braces vs indentation) |
| 02 | **Functions** | methods & parameters · function values · higher-order functions & currying · by-name & laziness · recursion & tailrec · partial functions |
| 03 | **OOP** | classes · objects & companions · traits & linearization · case classes · inheritance & init order · enums & ADTs · access modifiers · extension methods · export clauses · universal apply / creator applications |
| 04 | **Pattern matching** | every pattern form · extractors · exhaustiveness & sealed · type tests & erasure |
| 05 | **Collections** | hierarchy · sequences · sets & maps · the operations catalogue · views, iterators, LazyList · mutable collections · Arrays (the weird one) · performance & pitfalls |
| 06 | **Types** | type hierarchy · generics & variance · bounds · type members & path-dependent types · union & intersection · higher-kinded types · type lambdas & match types · structural types · singleton & literal types · dependent & polymorphic functions · opaque types |
| 07 | **Contextual abstractions** | given/using · type classes · derivation & Mirrors · implicit conversions · context functions · given imports & resolution rules |
| 08 | **Errors** | exceptions · Option/Either/Try · boundary/break · accumulating validation |
| 09 | **Concurrency** | threads, JMM, synchronization · Futures & Promises · virtual threads & structured concurrency |
| 10 | **Metaprogramming** | inline · compiletime ops · quoted macros · TASTy reflection · annotations |
| 11 | **Advanced** | for-comprehension desugaring · multiversal equality · named tuples · tuples as HLists · Java interop · capture checking (experimental) · experimental features · puzzlers |
| 12 | **Scala 2** | implicits & 2.x idioms · migration to Scala 3 |

### Part II — The ecosystem (`ecosystem/`)

| Area | Files |
|------|-------|
| Build tools | scala-cli directives · sbt sample project · Mill sample project |
| Testing | MUnit · ScalaTest · ScalaCheck (property testing) |
| Typelevel | cats (type classes, data types) · cats-effect (IO, fibers, resources) · fs2 (streams) |
| ZIO | ZIO effects, layers, streams |
| Direct style | Ox (structured concurrency on virtual threads) |
| JSON | circe · upickle |
| HTTP | sttp client · http4s server · tapir endpoints |
| Li Haoyi stack | os-lib · requests · upickle · fastparse |
| Data / DB | doobie (with in-memory H2) · Spark (Scala 2.13) |
| Actors | Apache Pekko (Akka fork) typed actors |
| Refined types | Iron |
| Other platforms | Scala.js · Scala Native |

See [`ecosystem/README.md`](ecosystem/README.md) for notes on each.

---

## Conventions used in comments

```scala
println(1 / 2)        // 0          ← what the line prints
// ⚠ corner case: ...                ← something that surprises people
// ✗ does not compile: ...           ← shown via typeCheckErrors
```

## IDE

Metals (VS Code) and IntelliJ both understand scala-cli. Because every file is an independent
program with its own dependencies, open a single lesson directory, or run
`scala-cli setup-ide <file>` for the file you are studying.
