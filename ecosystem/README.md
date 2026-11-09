# The Scala ecosystem

Each area has runnable files: `scala-cli run <file>` for programs and `scala-cli test <file>` for
`*.test.scala` files. The sample projects under `build-tools/` use their own build tool.

## Build tools — `build-tools/`

| File | What it shows |
|------|---------------|
| `01_scala_cli.scala` | `//> using` directives (deps, options, JVM opts, resources) and the scala-cli command set |
| `sbt-project/` | multi-module sbt 1.13 build: settings vs tasks, `%%`, `dependsOn`/`aggregate`, a custom task, scalafmt plugin, MUnit tests |
| `mill-project/` | the same project in Mill 1.3 (programmable `build.mill`; YAML alternative noted); `./mill` bootstraps Mill |
| `cross-project/` | cross-building across Scala 2.13 and Scala 3: `crossScalaVersions`, version-specific sources (`scala-2/`, `scala-3/`), and `sbt +test` |

```bash
cd ecosystem/build-tools/sbt-project && sbt test app/run app/hello
cd ecosystem/build-tools/mill-project && ./mill core.test app.run app.hello
cd ecosystem/build-tools/cross-project && sbt +test
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
| `discipline.test.scala` | type class law testing with Discipline and MUnit: Functor laws and expected failures |
| `munit_cats_effect.test.scala` | Cats Effect integration with MUnit: IO tests, Resource fixtures, and interceptIO |

## Typelevel stack — `typelevel/`

| File | What it shows |
|------|---------------|
| `01_cats_typeclasses.scala` | Eq, Show, Order, Semigroup/Monoid, Functor, Applicative, Monad, Traverse, Foldable, Either syntax; deriving with kittens |
| `02_cats_data_types.scala` | NonEmptyList, Validated(Nec), Chain, Ior, Kleisli, Reader, State, Writer, OptionT/EitherT, Eval |
| `03_cats_effect.scala` | IO laziness, errors, Resource, fibers (`parTupled`, `race`, `start`/`join`, `timeout`), cancellation, Ref/Deferred/Queue/Semaphore, retry with backoff |
| `04_fs2_streams.scala` | pure & effectful streams, resource safety, `parEvalMap`, `merge`, `groupWithin`, interruption, file I/O, pipes |
| `05_cats_effect_runtime.scala` | Cats Effect runtime utilities: IOLocal fiber isolation, Supervisor scope cancellation, Dispatcher callback bridge |
| `06_fs2_pulls.scala` | fs2 stream construction with Pull, Chunk boundaries, and cancellation with interruptScope |

## ZIO — `zio/`

| File | What it shows |
|------|---------------|
| `01_zio.scala` | `ZIO[R, E, A]`, typed errors vs defects, `validate`, ZLayer dependency injection, fibers (`<&>`, `foreachPar`, `race`, `timeout`, interruption), Ref, STM, Schedules, Scope, ZStream |
| `02_zio_test.test.scala` | ZIO Test suite (ZIOSpecDefault): assertTrue, layer provisioning, typed error assertions |

## Direct style — `direct-style/`

| File | What it shows |
|------|---------------|
| `ox.scala` | structured concurrency on virtual threads (JDK 21): `supervised`/`fork`/`forkUser`, `par`, `raceSuccess`, `timeoutOption`, `mapPar`, retries, channels, Flows |
| `02_ox_select.scala` | channel multiplexing with `select` across multiple Ox channels |

## JSON — `json/`

| File | What it shows |
|------|---------------|
| `01_circe.scala` | building JSON, derived & hand-written codecs, decoding errors (fail-fast and accumulating), cursors, transformations, null handling |
| `02_upickle_jsoniter.scala` | upickle `ReadWriter` (defaults, error paths), the mutable `ujson` AST, MessagePack; jsoniter-scala macro codecs; how to choose |
| `03_iron_circe.scala` | decoding refined types with Iron and circe: constraint validation and error reporting |

## HTTP — `http/` (everything runs locally on a random port)

| File | What it shows |
|------|---------------|
| `01_http4s.scala` | http4s routes DSL, path/query extractors, JSON bodies, 400 on bad input, Logger middleware, Ember server + client wired with Resource |
| `02_tapir_sttp.scala` | tapir endpoints as values → Netty direct-style server, typed sttp client and OpenAPI YAML, all from the same definitions |
| `03_http4s_middleware.scala` | http4s authentication middleware: checking headers and responding with 401 Unauthorized |

## Li Haoyi stack — `lihaoyi/`

| File | What it shows |
|------|---------------|
| `01_os_lib_requests.scala` | os-lib paths, files, walking, subprocesses with stdin/stdout; `requests` HTTP client against a local server |
| `02_fastparse.scala` | parser combinators: an arithmetic evaluator, cuts and error messages, whitespace-aware config parser |
| `03_mainargs.scala` | CLI parsing with mainargs: subcommands, flags, in-process parsing, and formatted usage docs |

## Data — `data/`

| File | What it shows |
|------|---------------|
| `01_doobie.scala` | doobie + in-memory H2: schema, inserts, batch updates, typed queries, composable fragments, joins, transaction rollback, why interpolation prevents SQL injection |
| `02_spark.scala` | **Scala 2.13** Spark 4 in local mode: typed Datasets, DataFrames, SQL, window functions, query plans, RDD word count (first run downloads ~300 MB) |
| `03_csv.scala` | streaming CSV encoding and lenient decoding with fs2-data-csv |
| `04_slick.scala` | Slick functional-relational mapping on in-memory H2: schema, joins, query AST to SQL, and rollback transactions |
| `05_skunk.scala` | pure functional Postgres with Skunk and embedded-postgres: codecs, prepared statements, and transactional rollback |

## Actors — `actors/`

| File | What it shows |
|------|---------------|
| `01_pekko_typed.scala` | Apache Pekko typed actors: behaviors as state, tell & ask, child actors, supervision with restart, serial message processing, timers |
| `02_pekko_streams_http.scala` | Pekko Streams (throttle, mapAsync, grouped) and Pekko HTTP server/client on port 0 with JSON route |

## Refined types — `refined/`

| File | What it shows |
|------|---------------|
| `01_iron.scala` | Iron: compile-time checked constants, runtime refinement with Either/Option, `RefinedType` newtypes, zero runtime overhead |

## Configuration — `config/`

| File | What it shows |
|------|---------------|
| `01_ciris.scala` | functional configuration with Ciris: error accumulation, file and property loading |

## Logging — `logging/`

| File | What it shows |
|------|---------------|
| `01_log4cats_scribe.scala` | structured logging with log4cats (SLF4J MDC) and direct-style logging with Scribe |

## Object mapping — `mapping/`

| File | What it shows |
|------|---------------|
| `01_chimney.scala` | boilerplate-free data transformation with Chimney: field mapping, renaming, and partial transformers |

## Optics — `optics/`

| File | What it shows |
|------|---------------|
| `01_monocle.scala` | functional optics with Monocle: deeply nested Lens updates and Optional / Prism |

## Parsing — `parsing/`

| File | What it shows |
|------|---------------|
| `01_cats_parse.scala` | functional parser combinators with cats-parse: typed AST results and offset-based error reporting |

## GraphQL — `graphql/`

| File | What it shows |
|------|---------------|
| `01_caliban.scala` | type-safe GraphQL with Caliban: schema derivation from case classes, SDL rendering, queries with arguments, and validation error reporting |

## Benchmarks — `benchmarks/`

| File | What it shows |
|------|---------------|
| `01_collections.bench.scala` | JMH microbenchmarks with `scala-cli --power run --jmh`: List vs Vector append, Array.sum vs while loop |

## Other platforms — `platforms/`

| File | What it shows |
|------|---------------|
| `01_scala_js.scala` | Scala.js on Node.js: facades for JS globals and Node modules, `js.Dynamic`, exports, the `js.Date()` vs `new js.Date()` trap, number semantics that differ from the JVM |
| `02_scala_native.scala` | Scala Native via LLVM: binding C functions, C strings and `Zone`s, pointers, `malloc`/`free`, stack allocation |

Requirements: Node.js for Scala.js, clang/LLVM for Scala Native.
