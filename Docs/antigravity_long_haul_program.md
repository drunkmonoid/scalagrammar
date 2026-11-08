# Antigravity long-haul program

Leon asked for this on 8 Nov 2026. Cursor wrote this plan. Antigravity executes it. Execute these phases in order. One phase is one commit on `main`, pushed to `origin/main`. Then start the next phase. Cursor is not doing this run.

Phases 1 through 19 are already on `main`. The last of them is `7ece84e`, "Phase 19: final verification and README". Do not redo them. Number these phases 20 through 49.

The repository is `drunkmonoid/scalagrammar`. `origin` is `git@github.com:drunkmonoid/scalagrammar.git`. If this file disagrees with a lesson that already runs, the lesson wins. Do not edit a finished lesson to match a sentence here.

## Goal

The language tour and the first ecosystem pass are done. This program fills the holes that tour left, and puts a check in front of the tree so later files cannot rot it.

Each new lesson is still one file a person can run. It prints labelled sections. It shows code that must not compile by printing the compiler's message at runtime.

## How to run

- Start from current `main`. Phase 20 commits this file if it is still untracked. Later phases do not restyle it and do not delete a hard stop.
- Read this file's phase, then the files that phase names, before editing.
- One phase, one commit, one push. Commit message shape: `Phase N: <object>`.
- Do not amend. Do not force-push. Do not skip hooks. Do not change git config. Do not commit secrets.
- If the push fails, stop the run.
- Run only the command the phase names. Do not run Spark, Scala.js, or Scala Native unless the phase says so. Do not run `./run.sh --all` as a habit. Phase 24's CI job is the full lessons run.
- A new file that does not run is a failed phase. Fix it in that phase. Do not delete the demonstration to go green. Do not pick a `typeCheckErrors` snippet that compiles.
- No emojis in source, in comments, or in commit messages.

## File contract

Every new Scala file, unless the phase says it is a test or a build definition:

- First lines are `//> using` directives. Scala 3 files use `//> using scala 3.9.0`. Do not change that version.
- Its own `package`, not one already used by another file.
- An `@main` that prints sections through a `def section` defined in that same file. There is no shared helper. Do not add one.
- A comment of the form `// See also: <path>` after the using directives, pointing at the existing file the phase names.
- Expected output written as a comment on the printing line, in the style `// 0`.
- A corner case marked `// ⚠` when the phase names one.
- Code that must not compile goes through `scala.compiletime.testing.typeCheckErrors`. Print the compiler message. `typeCheckErrors` sees parse and type errors only. A later-phase error (tailrec, some inheritance checks, capture checking) is quoted in a comment with the real compiler output, not faked.
- Dependencies are `//> using dep`. Resolve a stable version scala-cli can fetch. Write that version in the directive. Do not use a snapshot. Do not invent a version that does not resolve.

A file named `*.test.scala` is a test. `./run.sh` already calls `scala-cli test` for that suffix. It has no `@main`.

When a phase adds a file under `lessons/`, put its repo-relative path in backticks in the matching curriculum row of `README.md`. When it adds a file under `ecosystem/`, add a row to `ecosystem/README.md` with the filename and one line on what it runs. Phase 21's script checks backtick paths in `README.md` that contain `lessons/` or `ecosystem/` and end in `.scala`. Those paths must exist.

## Already finished

Do not re-teach these. A new file may point at them. It may not replace them.

- Lessons `01-basics` through `12-scala2`, including the macros directory. Polymorphic function values are a short section of `lessons/02-functions/02_function_values.scala`. Dependent function types are not a file yet. That is phase 25.
- `ClassTag`, `TypeTest`, `IArray`, `Factory` as a call (`.to(Factory)`), `boundary`/`break`, `scala.util.Using`, `NonFatal`, `Conversion`, `infix`, `open`, `@targetName`, `transparent inline`, `Selectable`, quotes, splices, and TASTy reflection.
- `summonFrom` is in `lessons/10-metaprogramming/02_compiletime.scala`.
- Ecosystem files listed in `ecosystem/README.md`, one per row. Ox already runs a failing supervised fork and a single channel. http4s already uses `Logger` middleware. Do not add a second Logger demo.
- The sbt sample and the Mill sample under `ecosystem/build-tools/`. Leave both trees alone. The cross-build is a new directory in phase 47.
- Scala 2.13 is not on one version. The idioms lesson says `2.13.16`. Spark says `2.13.17`. Phase 23 is the one phase that edits those pins.

## Hard stops

- Do not rewrite a lesson from phases 1–19, except the edits phase 23 and phase 30 name.
- Do not add Play, Akka, Slick, gRPC, Smithy, protobuf, Kafka, Skunk, Testcontainers, Graal native-image, a workbook of exercises, or a second Spark lesson.
- Do not add a library that needs Docker.
- Do not put `-experimental` on a file other than the capture-checking lesson. Phase 30 is the one phase that edits that lesson.
- Do not change `//> using scala 3.9.0` on an existing Scala 3 file.
- Do not format the repository. Do not add scalafmt or Scalafix to the existing sbt sample.
- Do not create `Docs/agent_channel.md`. The commit message is the record.
- Do not import one lesson's package from another lesson.

## Phase 20 — Charter

### Files

- `Docs/antigravity_long_haul_program.md` (this file)
- `README.md`, one short section only

### Object

Commit this file if it is untracked.

At the end of `README.md`, add a section `Agents` of two sentences: phases 1–19 are done, and phases 20–49 are this file. Do not edit the curriculum table in this phase.

### Exit

Push. `git status` is clean. No Scala file changed.

## Phase 21 — Inventory script

### Files

- `scripts/check-inventory.sh`
- `.gitignore` only if the script would otherwise force a build directory into git. Prefer not to touch `.gitignore`.

### Object

A bash script, executable, run from anywhere, that `cd`s to the repo root and exits 1 on the first problem.

Checks, all of them:

- Every `*.scala` under `lessons/` and `ecosystem/`, except paths containing `/sbt-project/`, `/mill-project/`, `/macros/`, or `/.scala-build/`, contains `//> using scala` in its first 15 lines and contains a line that starts with `package `.
- Every backtick span in `README.md` that contains `lessons/` or `ecosystem/` and ends with `.scala` is an existing file, relative to the repo root.

The script passes on the tree as phase 19 left it, before any later phase adds a file. Do not weaken a check to skip a file that fails. Fix the file only if it is a genuine miss of the contract above. The macros directory and the sbt and Mill sources are excluded on purpose.

Document the two checks in a comment at the top of the script.

### Exit

`./scripts/check-inventory.sh` exits 0. Commit. Push.

## Phase 22 — Fast local suite

### Files

- `run.sh`
- `README.md`, the run instructions only

### Object

`./run.sh --fast [dir]` behaves like `--all`, and it skips any path containing `02_spark.scala`, `01_scala_js.scala`, or `02_scala_native.scala`. The default directory is `lessons`, matching `--all`.

`--all` stays as it is and still runs those three files when they are under the directory. `--list` and `--help` mention `--fast`. The README's run block mentions it in one line.

Do not change how `*.test.scala` or the macros directory are launched.

### Exit

`./run.sh --fast lessons/01-basics` exits 0. `./scripts/check-inventory.sh` exits 0. Commit. Push.

## Phase 23 — One Scala 2.13 version

### Files

- `lessons/12-scala2/01_scala2_idioms.scala`
- `lessons/12-scala2/02_migration.scala`
- Any other tracked file that still says `2.13.16`

### Object

Spark is `//> using scala 2.13.17`. Move the idioms lesson to `2.13.17`. In the migration lesson, the cross-build example that says `2.13.16` says `2.13.17`. Search the tree for `2.13.16` and update remaining mentions. Do not change Spark's Scala version or its dependency versions. Do not run Spark.

### Exit

`./run.sh lessons/12-scala2/01_scala2_idioms.scala` exits 0. `./run.sh lessons/12-scala2/02_migration.scala` exits 0. A search for `2.13.16` in tracked files finds nothing. Commit. Push.

## Phase 24 — CI

### Files

- `.github/workflows/ci.yml`

### Object

GitHub Actions on `ubuntu-latest`, JDK 21 (Temurin), scala-cli 1.18 or a newer 1.x. Cache the Coursier directory.

Jobs:

- `inventory`: `./scripts/check-inventory.sh`
- `lessons`: `./run.sh --all lessons`, `timeout-minutes` at least 180
- `ecosystem`: `./run.sh --fast ecosystem`, `timeout-minutes` at least 120. This job must not launch Spark, Scala.js, or Scala Native, because `--fast` skips them.
- `platforms`: install Node.js and clang, then `./run.sh ecosystem/platforms/01_scala_js.scala` and `./run.sh ecosystem/platforms/02_scala_native.scala`
- `build-tools`: the sbt sample's `test` and `app/run`, and the Mill sample's `core.test` and `app.run`. Use the sbt launcher command already written in `ecosystem/README.md` if `sbt` is not installed. `./mill` bootstraps itself.
- `spark`: the same spark file, only on `workflow_dispatch`. Not on push.

If a job fails after the push, fix it before phase 25. That fix may be a second commit. Do not squash. Do not weaken `--fast` or the inventory script to make a job green.

### Exit

The workflow file is on `origin/main`. The `inventory` job and `lessons` job have gone green, or the follow-up commit that made them green is pushed. Commit. Push.

## Phase 25 — Dependent function types

### Files

- `lessons/02-functions/07_dependent_functions.scala`
- `README.md`, the functions row

### Object

See also `lessons/02-functions/02_function_values.scala`, which already has a polymorphic function value (`[A] => List[A] => Option[A]`). Do not repeat that example. This file is the dependent one.

Show a path-dependent method and the function type `(e: Entry) => e.Value`. Apply it to an entry whose `Value` is `Int`, and print the integer. Show that a dependent function is not a plain `Entry => Int` when two entries have different `Value` types. One `typeCheckErrors` snippet must fail because the result is used as the wrong type.

Update the lesson 02 sentence in the README that says dependent and polymorphic function types are in 02, so it names this file for the dependent half.

### Exit

`./run.sh lessons/02-functions/07_dependent_functions.scala` exits 0 and prints a section header and the integer. `./scripts/check-inventory.sh` exits 0. Commit. Push.

## Phase 26 — Factories and BuildFrom

### Files

- `lessons/05-collections/08_factories.scala`
- `README.md`, the collections row

### Object

See also `lessons/05-collections/01_hierarchy.scala`, which calls `.to(Factory)` and does not say what a factory is.

Show that `Array(1, 2, 3).map(_ + 1)` has class `Array`. Show `List(1, 2, 3).to(Vector)` and name `Factory` in a comment on that line. Obtain a `Builder` from `Vector`'s factory and add two elements. Show one call that copies because the source and the target are unrelated, and say so in a `// ⚠` comment.

A custom collection type is in scope only if the file stays a single readable lesson. A wrapper built with `Factory` or `BuildFrom` is enough. Do not reimplement `Vector`.

### Exit

`./run.sh lessons/05-collections/08_factories.scala` exits 0. The output shows an `Array` result and a `Vector` result. Inventory passes. Commit. Push.

## Phase 27 — NotGiven

### Files

- `lessons/07-contextual/07_notgiven.scala`
- `README.md`, the contextual row

### Object

See also `lessons/07-contextual/06_resolution_rules.scala` and `lessons/10-metaprogramming/02_compiletime.scala`. The compiletime lesson already runs `summonFrom`. Do not repeat it. Point at it.

Use `scala.util.NotGiven` so a method accepts an `A` only when no given `Show[A]` is visible, and print a call that succeeds for a type with no `Show`. A second call, for a type that has a `Show`, is a `typeCheckErrors` snippet. A third snippet is an ambiguous given: two instances of the same type at the same priority, and the compiler message is printed. A fourth snippet diverges: a given whose own constraint requires that given. Print that message too.

### Exit

`./run.sh lessons/07-contextual/07_notgiven.scala` exits 0. Three snippets report errors. One call prints a success value. Inventory passes. Commit. Push.

## Phase 28 — scala.Dynamic

### Files

- `lessons/11-advanced/09_dynamic.scala`
- `README.md`, the advanced row

### Object

See also `lessons/06-types/08_structural_types.scala`.

A class extending `Dynamic`, with `scala.language.dynamics` imported. `selectDynamic` returns the selected name. `applyDynamic` returns the name and the arguments. `updateDynamic` records the last assignment. The `@main` prints a selection, a call, and the recorded assignment.

A section of comments, not a second implementation, says when `Selectable` from the structural-types lesson is the better tool: the member set is known, or the result type should not be `Any`.

### Exit

`./run.sh lessons/11-advanced/09_dynamic.scala` exits 0 and prints the selected name. Inventory passes. Commit. Push.

## Phase 29 — Processes and sources

### Files

- `lessons/11-advanced/10_processes_and_io.scala`
- `README.md`, the advanced row

### Object

See also `lessons/08-errors/01_exceptions_resources.scala`, which already teaches `scala.util.Using`. Use `Using` here. Do not re-teach the exception hierarchy.

`scala.sys.process` runs `echo scalagrammar` and the program prints the trimmed output. `scala.io.Source` reads a temp file this program wrote, as UTF-8, through `Using`, then the program deletes the temp file. A `// ⚠` comment says `Source` is not closed by the garbage collector on any schedule you can rely on.

Do not run a shell pipeline that reads the repository. Do not call `scala.sys.process` with a string built from user input. The command is the literal `echo`.

### Exit

`./run.sh lessons/11-advanced/10_processes_and_io.scala` exits 0 and prints `scalagrammar`. The temp file is gone after the run. Inventory passes. Commit. Push.

## Phase 30 — erased and into

### Files

- `lessons/11-advanced/07_experimental_capture_checking.scala`

### Object

The section "Other experimental / preview features" lists `erased` and `into` as comments. This is the one phase allowed to edit that lesson.

For each of those two, replace the comment-only bullet with a real demonstration:

- If Scala 3.9.0 accepts it under a language import or a flag that can sit next to the file's existing `-experimental`, add a short snippet and print a value from it.
- If Scala 3.9.0 rejects it, delete the bullet and add a `typeCheckErrors` call, or a comment that quotes the compiler's actual message. Do not leave "preview in recent versions" as the only text.

Do not remove the capture-checking demonstration. The file still uses `-experimental`. Do not add named type arguments, modularity, or macro annotations in this phase. Those bullets may stay comments.

### Exit

`./run.sh lessons/11-advanced/07_experimental_capture_checking.scala` exits 0. The capture-checking section still prints. `erased` and `into` are each either executed or rejected with a real diagnostic. Inventory passes. Commit. Push.

## Phase 31 — Staging

### Files

- `lessons/10-metaprogramming/04_staging.scala`
- `README.md`, the metaprogramming row

### Object

See also `lessons/10-metaprogramming/01_inline.scala` and `lessons/10-metaprogramming/macros/`.

A quoted expression compiled and run with `scala.quoted.staging` in the same process. The staged expression adds one to `2`. The program prints `3`. A comment says how this differs from `inline`: `inline` rewrites the call site at the program's own compile, and staging compiles a quote when the program runs.

Add the staging artifact with `//> using dep` if it is not on the default classpath. Use the artifact published for Scala 3.9.0.

### Exit

`./run.sh lessons/10-metaprogramming/04_staging.scala` exits 0 and prints `3`. Inventory passes. Commit. Push.

## Phase 32 — Cats Effect runtime

### Files

- `ecosystem/typelevel/05_cats_effect_runtime.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/typelevel/03_cats_effect.scala`. That file already runs fibers, `Resource`, `Ref`, `Deferred`, `Queue`, `Semaphore`, cancellation, and retry. Do not repeat them.

This file runs three things:

- `IOLocal`: set a value, `parTupled` two fibers, each fiber reads its own value. Print both reads. They must not be the same leaked value.
- `Supervisor`: start a fiber that would print after a long sleep, close the supervisor scope, and print that the scope ended. The delayed print must not appear after the scope line.
- `Dispatcher`: unsafe-run a callback that completes a `Deferred`, and print the value the callback delivered.

Use the cats-effect version already in `03_cats_effect.scala` (`3.7.1`) if it still resolves.

### Exit

`./run.sh ecosystem/typelevel/05_cats_effect_runtime.scala` exits 0. The delayed supervisor print does not appear after the scope line. Inventory passes. Commit. Push.

## Phase 33 — fs2 Pull and Chunk

### Files

- `ecosystem/typelevel/06_fs2_pulls.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/typelevel/04_fs2_streams.scala`. That file already runs pure streams, `evalMap`, resource safety, concurrency, files, and pipes. Do not repeat the file-I/O section.

Build a stream with `Pull` that emits `1, 2, 3` and stops. Print the list. Print the `Chunk`s of a stream created with an explicit chunk size, so the output shows a boundary and not only the flattened elements. Use `interruptScope` on a stream that would otherwise not finish, and print that it stopped.

Use the fs2 version already in `04_fs2_streams.scala` (`3.14.0`) if it still resolves.

### Exit

`./run.sh ecosystem/typelevel/06_fs2_pulls.scala` exits 0 and prints `List(1, 2, 3)` or the same numbers in the same order. Inventory passes. Commit. Push.

## Phase 34 — ZIO Test

### Files

- `ecosystem/zio/02_zio_test.test.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/zio/01_zio.scala`. That file already runs typed errors, layers, fibers, STM, schedules, `Scope`, and `ZStream`. Do not copy those demos into a test.

A `ZIOSpecDefault` with three tests: `assertTrue` on a pure value, a test that provides a `ZLayer`, and a test whose effect fails with a typed error and asserts the failure. Use ZIO `2.1.27`, the version in `01_zio.scala`, and the matching `zio-test` artifact.

### Exit

`./run.sh ecosystem/zio/02_zio_test.test.scala` exits 0. Inventory passes. Commit. Push.

## Phase 35 — Ox select

### Files

- `ecosystem/direct-style/02_ox_select.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/direct-style/ox.scala`. That file already runs `supervised`, `fork`, `par`, `raceSuccess`, `timeoutOption`, `mapPar`, retry, one channel, a failing fork that cancels its sibling, and `Flow`. Do not repeat those.

This file uses `//> using jvm 21` and the same Ox version as `ox.scala` (`1.1.0`) if it still resolves.

`select` waits on two channels. Send a value on the second channel only. Print which channel delivered it. The first channel stays empty.

### Exit

`./run.sh ecosystem/direct-style/02_ox_select.scala` exits 0 and the output names the second channel. Inventory passes. Commit. Push.

## Phase 36 — mainargs

### Files

- `ecosystem/lihaoyi/03_mainargs.scala`
- `ecosystem/README.md`

### Object

See also `lessons/01-basics/01_hello.scala`, which is `@main`.

Parse an argument vector in-process with mainargs. Do not look at `args` from the real command line, so `./run.sh` with no extra arguments still succeeds. One vector is a valid subcommand with a flag. Print the parsed values. One vector is invalid. Print the usage text mainargs returns, and do not let that failure crash the program.

### Exit

`./run.sh ecosystem/lihaoyi/03_mainargs.scala` exits 0 and prints both the parsed value and a usage line. Inventory passes. Commit. Push.

## Phase 37 — ciris

### Files

- `ecosystem/config/01_ciris.scala`
- `ecosystem/README.md`

### Object

See also `lessons/08-errors/03_validation_accumulation.scala`.

Load a host and a port. The success path reads them from the environment, which this program sets in the same process before loading, or from a temp file it writes and then deletes. Print the loaded host and port.

The failure path loads a config whose port is not an integer and whose host is empty. Print one combined error that names both fields. Do not stop at the first field.

Use a stable `is.cir::ciris` that resolves. No HOCON, no config server.

### Exit

`./run.sh ecosystem/config/01_ciris.scala` exits 0. The output contains the loaded port and both field names in the error. The temp file is gone. Inventory passes. Commit. Push.

## Phase 38 — Logging

### Files

- `ecosystem/logging/01_log4cats_scribe.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/json/02_upickle_jsoniter.scala` for the shape of a two-library file.

One section uses log4cats over `slf4j-simple`. Log one line at info. Put a correlation id in the logging context for a second line, and make sure that id is in the output.

One section uses scribe. Log one line at info and one at debug, with debug enabled for that logger so the debug line is not dropped.

Both sections run in one `@main`. The process exits 0 even though slf4j-simple writes to stderr.

### Exit

`./run.sh ecosystem/logging/01_log4cats_scribe.scala` exits 0. Stdout or stderr contains the correlation id and the scribe debug line. Inventory passes. Commit. Push.

## Phase 39 — Chimney

### Files

- `ecosystem/mapping/01_chimney.scala`
- `ecosystem/README.md`

### Object

Two case classes with the same fields. A Chimney transformer copies one into the other. Print the result.

A second pair where one field is renamed. The transformer maps that field by name. Print the result.

A partial transformer: the source has a `String` port, the target has an `Int` port. On `"no"` the program prints the partial-transform error and does not throw out of `@main`.

### Exit

`./run.sh ecosystem/mapping/01_chimney.scala` exits 0 and prints a mapped value and an error. Inventory passes. Commit. Push.

## Phase 40 — Monocle

### Files

- `ecosystem/optics/01_monocle.scala`
- `ecosystem/README.md`

### Object

A nested case class, two levels deep, with an `Int` field. A `Lens` reads it and replaces it. Print the updated value.

An `Optional` or `Prism` over an `Option` field. One value is `Some`. Print the modified `Some`. One value is `None`. Print the original value, unchanged.

Use `dev.optics` Monocle artifacts that resolve for Scala 3.9.0 (`monocle-core`, and `monocle-macro` if the lens is a macro).

### Exit

`./run.sh ecosystem/optics/01_monocle.scala` exits 0 and prints the updated integer and the unchanged `None` value. Inventory passes. Commit. Push.

## Phase 41 — cats-parse

### Files

- `ecosystem/parsing/01_cats_parse.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/lihaoyi/02_fastparse.scala`, which is an arithmetic evaluator. Do not write another arithmetic evaluator.

Parse a tiny predicate language: `name = value` and `name = value and name = value`. Print the pairs for one valid input. For `name =`, print the error and the offset where the parser stopped.

A short comment says what is different from fastparse: the result type is explicit, and the error carries an offset rather than a stack of parser names.

### Exit

`./run.sh ecosystem/parsing/01_cats_parse.scala` exits 0. The valid parse prints both pairs. The bad parse prints an offset. Inventory passes. Commit. Push.

## Phase 42 — CSV

### Files

- `ecosystem/data/03_csv.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/data/01_doobie.scala` only as a neighbour. Do not open a database.

fs2-data-csv reads and writes a temp file of a case class with two fields. Two rows are valid. One row has a non-integer in an integer field. Print the valid rows and the decode error for the bad row. Delete the temp file.

No Spark. No Docker.

### Exit

`./run.sh ecosystem/data/03_csv.scala` exits 0. The output contains both valid rows and the bad row's error. The temp file is gone. Inventory passes. Commit. Push.

## Phase 43 — Iron in circe

### Files

- `ecosystem/json/03_iron_circe.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/refined/01_iron.scala` and `ecosystem/json/01_circe.scala`.

Decode JSON into a case class whose email and age are Iron types: a non-empty string constrained as an email, and a positive integer. Use the `iron-circe` module. Keep the Iron version at `3.4.1`, the version in `01_iron.scala`. If that version has no circe module that resolves, bump Iron in both files to a version that has one, and re-run `01_iron.scala` in this phase.

Print a successful decode. Print the circe error for `{"email": "nope", "age": -1}` without throwing out of `@main`.

### Exit

`./run.sh ecosystem/json/03_iron_circe.scala` exits 0. `./run.sh ecosystem/refined/01_iron.scala` still exits 0. Inventory passes. Commit. Push.

## Phase 44 — http4s auth middleware

### Files

- `ecosystem/http/03_http4s_middleware.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/http/01_http4s.scala`. That file already starts Ember on a random port and already applies `Logger`. Do not copy the todo routes. Do not add another `Logger`.

A route `GET /ping` returns 200 and the text `pong`. A middleware requires the header `X-Token: ok`. Without the header the response is 401. With the header the response is 200. Bind port 0, call the server with the Ember client, shut down with `Resource`.

Use the http4s version already in `01_http4s.scala` (`0.23.38`) if it still resolves. `slf4j-simple` is fine.

### Exit

`./run.sh ecosystem/http/03_http4s_middleware.scala` exits 0 and prints 401 and 200. Inventory passes. Commit. Push.

## Phase 45 — Discipline

### Files

- `ecosystem/testing/discipline.test.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/testing/munit.test.scala`, which already shows `test("...".fail)`.

A Discipline suite, through MUnit, checks the `Functor` laws for `Option` using cats-laws. That suite passes.

A second instance is a `Functor` that is not lawful: `map` ignores the function and returns `pure` of a constant, or otherwise breaks identity. That test is marked `.fail`, in the same way `munit.test.scala` marks an expected failure. Do not `.ignore` it.

### Exit

`./run.sh ecosystem/testing/discipline.test.scala` exits 0. Inventory passes. Commit. Push.

## Phase 46 — MUnit on Cats Effect

### Files

- `ecosystem/testing/munit_cats_effect.test.scala`
- `ecosystem/README.md`

### Object

See also `ecosystem/typelevel/03_cats_effect.scala` and `ecosystem/testing/munit.test.scala`.

A `CatsEffectSuite` with three tests: an `IO` that asserts a computed value, a `Resource` fixture that acquires a value and releases it, and an `IO` that fails with `IllegalArgumentException` which the test expects.

Use cats-effect `3.7.1` and a `munit-cats-effect` version that resolves against MUnit `1.3.6`.

### Exit

`./run.sh ecosystem/testing/munit_cats_effect.test.scala` exits 0. Inventory passes. Commit. Push.

## Phase 47 — Cross-build

### Files

- `ecosystem/build-tools/cross-project/` (new)
- `ecosystem/README.md`, the build-tools section
- `lessons/12-scala2/02_migration.scala`, only if its cross-build paragraph still says the repo does not contain one. One sentence may name this directory. Do not rewrite the lesson.

### Object

See also `lessons/12-scala2/02_migration.scala`, which describes `crossScalaVersions` and `scala-2` / `scala-3` source folders. The sbt sample and the Mill sample stay single-version. Do not edit them.

A new sbt 1.13 project. `crossScalaVersions` is `2.13.17` and `3.9.0`. Shared code defines `square`. A Scala 2 source defines an implicit class with `times10`. A Scala 3 source defines an extension method with the same caller-facing name. An MUnit test checks `square(4)` is `16` and `times10` of `3` is `30`. The shared file uses syntax both versions accept.

`sbt +test` runs both. Document the command in `ecosystem/README.md` the way the existing sbt sample is documented. The inventory script ignores this tree if it lives under a path the script already excludes. Put it next to `sbt-project`, and extend the script's exclude list with `/cross-project/` only if a `*.scala` there would otherwise fail the `//> using scala` check. Those sources are sbt sources. They do not need a using directive.

### Exit

From `ecosystem/build-tools/cross-project`, `sbt +test` exits 0. Inventory passes. Commit. Push.

## Phase 48 — First ten files

### Files

- `README.md`

### Object

Add a section `First ten files` before the curriculum table. Ten links, in this order, each an existing file:

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

One sentence says the curriculum table is the full reference.

Then check every Scala file added in phases 25–47. Each has a `// See also:` comment whose path exists. If one is missing, add the comment. Do not edit the body of the lesson.

### Exit

Inventory passes. All ten paths exist. Commit. Push.

## Phase 49 — Glossary

### Files

- `GLOSSARY.md`
- `README.md`, one link to the glossary, next to the Agents section

### Object

About thirty terms. Each entry is two or three lines and names the file that demonstrates the term. Use these pairs. Do not send the reader to a file that does not show the term.

- `@main` — `lessons/01-basics/01_hello.scala`
- `val` and `lazy val` — `lessons/01-basics/02_values_variables.scala`
- significant indentation — `lessons/01-basics/07_syntax_styles.scala`
- by-name parameter — `lessons/02-functions/04_by_name_laziness.scala`
- dependent function — `lessons/02-functions/07_dependent_functions.scala`
- `@tailrec` — `lessons/02-functions/05_recursion_tailrec.scala`
- case class — `lessons/03-oop/04_case_classes.scala`
- enum as an ADT — `lessons/03-oop/06_enums_adts.scala`
- extension method — `lessons/03-oop/08_extension_methods.scala`
- export — `lessons/03-oop/09_export_composition.scala`
- extractor — `lessons/04-pattern-matching/02_extractors.scala`
- variance — `lessons/06-types/02_generics_variance.scala`
- `Factory` — `lessons/05-collections/08_factories.scala`
- opaque type — `lessons/06-types/10_opaque_types.scala`
- union type — `lessons/06-types/05_union_intersection.scala`
- match type — `lessons/06-types/07_type_lambdas_match_types.scala`
- `given` and `using` — `lessons/07-contextual/01_given_using.scala`
- type class — `lessons/07-contextual/02_type_classes.scala`
- `NotGiven` — `lessons/07-contextual/07_notgiven.scala`
- derivation and `Mirror` — `lessons/07-contextual/03_derivation.scala`
- `Option`, `Either`, `Try` — `lessons/08-errors/02_option_either_try.scala`
- `boundary` and `break` — `lessons/01-basics/05_control_flow.scala`
- `Future` — `lessons/09-concurrency/02_futures_promises.scala`
- `inline` — `lessons/10-metaprogramming/01_inline.scala`
- quote and splice — `lessons/10-metaprogramming/macros/Main.scala`
- staging — `lessons/10-metaprogramming/04_staging.scala`
- multiversal equality — `lessons/11-advanced/02_multiversal_equality.scala`
- explicit nulls — `lessons/11-advanced/06_explicit_nulls.scala`
- capture checking — `lessons/11-advanced/07_experimental_capture_checking.scala`
- Scala 2 `implicit` — `lessons/12-scala2/01_scala2_idioms.scala`
- `IO` — `ecosystem/typelevel/03_cats_effect.scala`
- ZIO — `ecosystem/zio/01_zio.scala`
- Iron — `ecosystem/refined/01_iron.scala`

Confirm every new lesson path from phases 25–47 is in a README table in backticks. Add any that are missing. Do not rewrite table rows that are already right.

### Exit

`./scripts/check-inventory.sh` exits 0. Every path in the glossary exists. This is the last phase. Commit. Push. Stop.
