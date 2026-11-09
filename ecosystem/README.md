# The Scala ecosystem

Each area has runnable files: `scala-cli run <file>` for programs and `scala-cli test <file>` for
`*.test.scala` files. The two sample projects under `build-tools/` use their own build tool.

## Build tools — `build-tools/`

| File | What it shows |
|------|---------------|
| `01_scala_cli.scala` | `//> using` directives (deps, options, JVM opts, resources) and the scala-cli command set |
| `sbt-project/` | multi-module sbt 1.13 build: settings vs tasks, `%%`, `dependsOn`/`aggregate`, a custom task, scalafmt plugin, MUnit tests |
| `mill-project/` | the same project in Mill 1.3 (programmable `build.mill`; YAML alternative noted); `./mill` bootstraps Mill |

```bash
cd ecosystem/build-tools/sbt-project && sbt test app/run app/hello
cd ecosystem/build-tools/mill-project && ./mill core.test app.run app.hello
```

No sbt installed? scala-cli can run the sbt launcher directly:

```bash
cd ecosystem/build-tools/sbt-project
scala-cli run --server=false --dep org.scala-sbt:sbt-launch:1.13.1 --main-class xsbt.boot.Boot -- test
```

## Testing — `testing/`

| File | What it shows |
|------|---------------|
| `munit.test.scala` | assertions, expected failures, `compileErrors`, async tests, fixtures, generated tests |
| `scalatest.test.scala` | FunSuite / FlatSpec / WordSpec styles, matchers, futures, pending & ignored |
| `scalacheck.test.scala` | property-based testing, custom generators, shrinking |

## Typelevel stack — `typelevel/`

| File | What it shows |
|------|---------------|
| `01_cats_typeclasses.scala` | Eq, Show, Order, Semigroup/Monoid, Functor, Applicative, Monad, Traverse, Foldable, Either syntax; deriving with kittens |
| `02_cats_data_types.scala` | NonEmptyList, Validated(Nec), Chain, Ior, Kleisli, Reader, State, Writer, OptionT/EitherT, Eval |
| `03_cats_effect.scala` | IO laziness, errors, Resource, fibers (`parTupled`, `race`, `start`/`join`, `timeout`), cancellation, Ref/Deferred/Queue/Semaphore, retry with backoff |
| `04_fs2_streams.scala` | pure & effectful streams, resource safety, `parEvalMap`, `merge`, `groupWithin`, interruption, file I/O, pipes |
| `05_cats_effect_runtime.scala` | Cats Effect runtime utilities: IOLocal fiber isolation, Supervisor scope cancellation, Dispatcher callback bridge |

## ZIO — `zio/`

| File | What it shows |
|------|---------------|
| `01_zio.scala` | `ZIO[R, E, A]`, typed errors vs defects, `validate`, ZLayer dependency injection, fibers (`<&>`, `foreachPar`, `race`, `timeout`, interruption), Ref, STM, Schedules, Scope, ZStream |

## Direct style — `direct-style/`

| File | What it shows |
|------|---------------|
| `ox.scala` | structured concurrency on virtual threads (JDK 21): `supervised`/`fork`/`forkUser`, `par`, `raceSuccess`, `timeoutOption`, `mapPar`, retries, channels, Flows |

## JSON — `json/`

| File | What it shows |
|------|---------------|
| `01_circe.scala` | building JSON, derived & hand-written codecs, decoding errors (fail-fast and accumulating), cursors, transformations, null handling |
| `02_upickle_jsoniter.scala` | upickle `ReadWriter` (defaults, error paths), the mutable `ujson` AST, MessagePack; jsoniter-scala macro codecs; how to choose |

## HTTP — `http/` (everything runs locally on a random port)

| File | What it shows |
|------|---------------|
| `01_http4s.scala` | http4s routes DSL, path/query extractors, JSON bodies, 400 on bad input, Logger middleware, Ember server + client wired with Resource |
| `02_tapir_sttp.scala` | tapir endpoints as values → Netty direct-style server, typed sttp client and OpenAPI YAML, all from the same definitions |

## Li Haoyi stack — `lihaoyi/`

| File | What it shows |
|------|---------------|
| `01_os_lib_requests.scala` | os-lib paths, files, walking, subprocesses with stdin/stdout; `requests` HTTP client against a local server |
| `02_fastparse.scala` | parser combinators: an arithmetic evaluator, cuts and error messages, whitespace-aware config parser |

## Data — `data/`

| File | What it shows |
|------|---------------|
| `01_doobie.scala` | doobie + in-memory H2: schema, inserts, batch updates, typed queries, composable fragments, joins, transaction rollback, why interpolation prevents SQL injection |
| `02_spark.scala` | **Scala 2.13** Spark 4 in local mode: typed Datasets, DataFrames, SQL, window functions, query plans, RDD word count (first run downloads ~300 MB) |

## Actors — `actors/`

| File | What it shows |
|------|---------------|
| `01_pekko_typed.scala` | Apache Pekko typed actors: behaviors as state, tell & ask, child actors, supervision with restart, serial message processing, timers |

## Refined types — `refined/`

| File | What it shows |
|------|---------------|
| `01_iron.scala` | Iron: compile-time checked constants, runtime refinement with Either/Option, `RefinedType` newtypes, zero runtime overhead |

## Other platforms — `platforms/`

| File | What it shows |
|------|---------------|
| `01_scala_js.scala` | Scala.js on Node.js: facades for JS globals and Node modules, `js.Dynamic`, exports, the `js.Date()` vs `new js.Date()` trap, number semantics that differ from the JVM |
| `02_scala_native.scala` | Scala Native via LLVM: binding C functions, C strings and `Zone`s, pointers, `malloc`/`free`, stack allocation |

Requirements: Node.js for Scala.js, clang/LLVM for Scala Native.
