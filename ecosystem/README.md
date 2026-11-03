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
