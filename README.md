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
./run.sh --fast              # like --all, but skips spark, scala-js, and scala-native
./run.sh ecosystem/testing/munit.test.scala   # *.test.scala files run with `scala-cli test`
```

A few lessons need more than one file or a specific tool; their headers say how to run them:
`lessons/10-metaprogramming/macros/` (a directory, run with `--server=false`), the sbt and Mill
sample projects in `ecosystem/build-tools/`, Scala.js (Node.js) and Scala Native (clang).

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

Target: **Scala 3.9.0** on JDK 17+ (a few files ask scala-cli for JDK 21). Scala 2 has its own
section (`lessons/12-scala2`), because much existing code and some libraries (Spark!) are still
Scala 2. Every file has been compiled and run with scala-cli 1.18; dependency versions are the
current releases as of November 2026.

---

## First ten files

If you are new to Scala 3 or want the quickest on-ramp, start with these ten lessons in order. The curriculum table below is the full reference.

1. `lessons/01-basics/01_hello.scala`
2. `lessons/01-basics/02_values_variables.scala`
3. `lessons/01-basics/03_types_literals.scala`
4. `lessons/01-basics/05_control_flow.scala`
5. `lessons/02-functions/01_methods.scala`
6. `lessons/03-oop/04_case_classes.scala`
7. `lessons/04-pattern-matching/01_patterns.scala`
8. `lessons/06-types/02_generics_variance.scala`
9. `lessons/07-contextual/01_given_using.scala`
10. `lessons/08-errors/02_option_either_try.scala`

---

## Curriculum

### Part I — The language (`lessons/`)

| # | Topic | Files |
|---|-------|-------|
| 01 | **Basics** | hello & entry points · values/vars/lazy · numeric types & literals · strings & interpolation · control flow · operators & precedence · syntax styles (braces vs indentation) |
| 02 | **Functions** | methods & parameters · function values · higher-order functions & currying · by-name & laziness · recursion & tailrec · partial functions · `lessons/02-functions/07_dependent_functions.scala` |
| 03 | **OOP** | classes · objects & companions · traits & linearization · case classes · inheritance & init order · enums & ADTs · access modifiers · extension methods · export clauses · universal apply / creator applications |
| 04 | **Pattern matching** | every pattern form · extractors · exhaustiveness & sealed · type tests & erasure · `lessons/04-pattern-matching/04_evaluation_order.scala` |
| 05 | **Collections** | hierarchy · sequences · sets & maps · the operations catalogue · views, iterators, LazyList · mutable collections · Arrays (the weird one) · performance & pitfalls · `lessons/05-collections/08_factories.scala` |
| 06 | **Types** | type hierarchy · generics & variance · bounds · type members & path-dependent types · union & intersection · higher-kinded types · type lambdas & match types · structural types · singleton & literal types · opaque types (polymorphic function types are in 02, dependent function types in `lessons/02-functions/07_dependent_functions.scala`) |
| 07 | **Contextual abstractions** | given/using · type classes · derivation & Mirrors · implicit conversions · context functions · given imports & resolution rules · `lessons/07-contextual/07_notgiven.scala` · `lessons/07-contextual/08_given_priority.scala` |
| 08 | **Errors** | exceptions · Option/Either/Try · boundary/break · accumulating validation |
| 09 | **Concurrency** | threads, JMM, synchronization · Futures & Promises · virtual threads (structured concurrency: see Ox in Part II) · `lessons/09-concurrency/04_init_races.scala` |
| 10 | **Metaprogramming** | inline · compiletime ops · quoted macros · TASTy reflection · annotations · `lessons/10-metaprogramming/04_staging.scala` |
| 11 | **Advanced** | for-comprehension desugaring · multiversal equality · named tuples · Java interop · tuples as HLists · explicit nulls · capture checking & other experimental features · puzzlers · `lessons/11-advanced/09_dynamic.scala` · `lessons/11-advanced/10_processes_and_io.scala` · `lessons/11-advanced/11_overloading_inference.scala` · `lessons/11-advanced/12_safe_init.scala` · `lessons/11-advanced/13_lints.scala` · `lessons/11-advanced/14_bytecode.scala` · `lessons/11-advanced/15_equality_contracts.scala` · `lessons/11-advanced/16_numeric_tower.scala` |
| 12 | **Scala 2** | implicits & 2.x idioms · migration to Scala 3 |

### Part II — The ecosystem (`ecosystem/`)

| Folder | Contents |
|------|-------|
| `build-tools/` | scala-cli directives · sbt 1.13 multi-module project · Mill 1.3 project |
| `testing/` | MUnit · ScalaTest · ScalaCheck (property testing) |
| `typelevel/` | cats type classes · cats data types · cats-effect (IO, fibers, Resource) · fs2 streams |
| `zio/` | ZIO: typed errors, layers, fibers, STM, schedules, ZStream · ZIO HTTP |
| `direct-style/` | Ox: structured concurrency on virtual threads |
| `json/` | circe · upickle · jsoniter-scala |
| `http/` | http4s server + client · tapir endpoints → server, sttp client, OpenAPI |
| `lihaoyi/` | os-lib · requests · fastparse · Cask |
| `data/` | doobie (in-memory H2) · Spark (Scala 2.13, local mode) · CSV · Slick · Skunk (embedded Postgres) |
| `actors/` | Apache Pekko typed actors · Pekko Streams & HTTP |
| `refined/` | Iron refined types |
| `config/` | Ciris functional configuration |
| `logging/` | log4cats & Scribe |
| `mapping/` | Chimney |
| `optics/` | Monocle |
| `parsing/` | cats-parse |
| `graphql/` | Caliban in-process GraphQL |
| `benchmarks/` | JMH microbenchmarks |
| `streaming/` | Kafka Streams with TopologyTestDriver |
| `api/` | Smithy4s dynamic schemas |
| `derivation/` | Magnolia generic derivation |
| `platforms/` | Scala.js · Scala Native · Laminar on jsdom |

See [`ecosystem/README.md`](ecosystem/README.md) for notes on each.

---

## Exercises

Hands-on practice exercises with stubs and working solutions: see [`exercises/README.md`](exercises/README.md).

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

## Agents

Phases 1 through 19 are done. Phases 20 through 49 are defined in `Docs/antigravity_long_haul_program.md` and are done. Phases 50–87 are defined in `Docs/long_haul_program_2.md` and are done.

## Glossary

See [`GLOSSARY.md`](GLOSSARY.md) for concise definitions of core Scala 3 concepts and pointers to their demonstration files.

