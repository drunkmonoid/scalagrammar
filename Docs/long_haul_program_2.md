# Long-haul program 2: phases 50–87

Leon asked for this on 9 Nov 2026. Claude Code wrote this plan. The agent Leon assigns executes it. Execute these phases in order. One phase is one commit on `main`, pushed to `origin/main`. Then start the next phase.

This program continues `Docs/antigravity_long_haul_program.md` (phases 20–49). It does not replace it.

## Start condition

Run `git log --oneline -1 --grep '^Phase 49:'` on `main`. If it prints nothing, phase 49 is not done. Stop the run and report that. Do not start phase 50 early, and do not do phases 34–49 yourself unless Leon says so.

`git status` must be clean before phase 50. If it is not clean, stop and report the dirty paths. Do not commit, stash, or revert someone else's edit.

## Goal

The language tour, the first ecosystem pass and the gap-filling run are done. This program adds three things:

1. **Exercises with solutions** (phases 51–65). Each exercise is a failing MUnit suite the reader makes pass. A solution file with the same tests passes. CI proves that every stub compiles and every solution is green.
2. **Language depth** (phases 66–75). Rules people usually learn by accident: overloading and inference, given priority, the initialization checker, lints, bytecode, benchmarks, equality contracts, pattern evaluation order, initialization under concurrency, the numeric tower.
3. **More ecosystem** (phases 76–85). Pekko Streams and HTTP, Caliban, Slick, Skunk on an embedded Postgres, Kafka Streams without a broker, Laminar on jsdom, Smithy4s, ZIO HTTP, Cask, Magnolia.

Phases 86–87 tie it together and verify everything.

## How to run

- Start from current `main` after the start condition holds.
- Read this file's phase, then the files that phase names, before editing.
- One phase, one commit, one push. Commit message shape: `Phase N: <object>`.
- Write commit messages with `git commit -F -` and a quoted heredoc (`<<'EOF'`). Never put backticks in a `-m` argument: the shell runs them as commands and silently drops words.
- Do not amend. Do not force-push. Do not skip hooks. Do not change git config. Do not commit secrets.
- If the push fails, stop the run.
- Run the commands the phase's Exit names, plus `./scripts/check-inventory.sh`. Do not run `./run.sh --all` as a habit. Phase 87 is the full run.
- A new file that does not run is a failed phase. Fix it in that phase. Do not delete the demonstration to go green. Do not pick a `typeCheckErrors` snippet that compiles.
- No emojis in source, in comments, or in commit messages. The `⚠` marker in comments is the one allowed symbol.

## File contract

The file contract of `Docs/antigravity_long_haul_program.md` still holds for every new file under `lessons/` and `ecosystem/`:

- First lines are `//> using` directives. Scala 3 files use `//> using scala 3.9.0`. Do not change that version.
- Its own `package`, not one used by another file.
- An `@main` that prints sections through a `def section` defined in the same file. There is no shared helper.
- `// See also: <path>` after the using directives, pointing at the existing file the phase names.
- Expected output as a comment on the printing line, in the style `// 0`. Corner cases marked `// ⚠`.
- Code that must not compile goes through `scala.compiletime.testing.typeCheckErrors`, and the compiler message is printed. A later-phase error is quoted in a comment with the real compiler output.
- Dependencies are `//> using dep` with a stable version that resolves. Pin the exact version. Where this file names a version already used elsewhere in the repo, use that version.

### Exercise contract

Phase 51 adds this contract to `exercises/README.md`. Phases 52–65 follow it.

- A **stub** lives at `exercises/<NN-section>/<exNN_name>.test.scala`.
- Its **solution** lives at the mirrored path `exercises/solutions/<NN-section>/<exNN_name>.test.scala`.
- Both start with `//> using scala 3.9.0` and `//> using test.dep org.scalameta::munit:1.3.6`, plus any deps the exercise needs. Both files have identical directives.
- Packages: the stub is `exercises.<section>.<name>`. The solution is `solutions.<section>.<name>`. Section and name are lowercase identifiers without the number prefix.
- Both carry `// See also: <lesson path>`.
- The stub opens with a comment block headed `// Task:` that states what to implement, in plain sentences, with one example input and output.
- In the stub, every definition the reader writes has body `???`. The one exception is a "make this compile" exercise, where the definition is absent and the `// Task:` block names it. The stub still contains `???` somewhere, compiles, and at least one of its tests fails.
- The test class is `class <Name>Suite extends munit.FunSuite` (or the suite type the phase names). The test bodies in the stub and the solution are character-for-character identical. Only the definitions under test differ.
- Each exercise has at least three tests. At least one is a corner case, marked `// ⚠` in the test.
- The solution contains no `???`, and all its tests pass.
- For "make this compile" exercises, the test is `assertNoDiff(compileErrors("<snippet>"), "")`. The snippet uses fully qualified names, because `compileErrors` (like `typeCheckErrors`) cannot see local definitions. The reader's definition must be top-level in the exercise's package.
- An exercise never needs network access, a server on a fixed port, or more than 30 seconds.

## Known traps

These were all hit in this repo. Read them before writing code.

- `typeCheckErrors` and MUnit's `compileErrors` cannot see local definitions (a `def` or `class` inside the `@main` body). Put what the snippet needs at top level and use fully qualified names, or define it inside the snippet.
- `typeCheckErrors` stops after type checking. `@tailrec`, "trait already implemented", override checks done in refchecks, `-Wsafe-init` and capture checking happen later. Quote those errors in comments with the real compiler output.
- `import scala.annotation.*` also imports the packages `scala.annotation.unchecked` and `scala.annotation.meta`. They shadow `@unchecked` and any user package named `meta`.
- `@main` ignores default parameter values. A missing argument prints `Illegal command line: more arguments expected`.
- Scala 3 cannot construct `scala.NoSuchElementException` through universal apply (it's an alias). Write `java.util.NoSuchElementException(...)`.
- A local `val` that refers to itself (a self-referential `LazyList`, say) must be a `lazy val`.
- Inside an extension method named `show`, `x.show` resolves to that same method. Call the instance explicitly.
- A wildcard import of both `scala.jdk.CollectionConverters.*` and `scala.jdk.FunctionConverters.*` makes `Seq(...).asJava` ambiguous, because a `Seq[Int]` is also an `Int => Int`.
- Multi-file macro lessons crash the Bloop server. Run them with `scala-cli run --server=false <dir>`.
- Ox `fork` is a daemon fork, interrupted when the scope body returns. `forkUser` makes the scope wait.
- The tapir Netty sync server must start inside an Ox `supervised` scope.
- Scala.js `js.Date()` returns a string. `new js.Date()` returns a date.
- Spark needs a `log4j2.properties` resource to stay quiet. Setting the level in code is overwritten.
- Library versions move. Look up the current stable release on Maven Central before pinning. Do not guess.

## Hard stops

- Do not edit a file from phases 1–49, except `README.md`, `ecosystem/README.md`, `GLOSSARY.md`, `run.sh`, `scripts/check-inventory.sh`, `.github/workflows/ci.yml` and `exercises/README.md`, and only where a phase names it.
- Do not change `//> using scala 3.9.0` or `//> using scala 2.13.17` on any existing file.
- Do not add Play, Akka (Pekko is the actor library here), gRPC, protobuf, Graal native-image, or a second Spark lesson.
- Do not add a library or demo that needs Docker or a service outside the process. Servers bind `127.0.0.1` on port 0 (or a free port found at runtime) and shut down before `@main` returns. Databases are in-memory or embedded.
- No lesson calls an external host at runtime. Dependency resolution is the only network access.
- Every program ends on its own within five minutes, except a JMH benchmark run without its short flags.
- Do not put `-experimental` on any new file.
- Do not import one lesson's package from another lesson. Exercises do not import lessons.
- Do not commit a stub that passes all its tests. Do not commit a solution that contains `???`.
- Do not format the repository. Do not add scalafmt or Scalafix to the sbt or Mill samples.

---

# Part A — Setup

## Phase 50 — Charter link

### Files

- `README.md`, the `Agents` section only

### Object

This file is already committed. Add one sentence to the `Agents` section: phases 50–87 are `Docs/long_haul_program_2.md`.

### Exit

Inventory passes. No Scala file changed. Commit. Push.

## Phase 51 — Exercise infrastructure

### Files

- `exercises/README.md` (new)
- `run.sh`
- `scripts/check-inventory.sh`
- `.github/workflows/ci.yml`
- `README.md`

### Object

`exercises/README.md` explains how to work through the exercises. Run a stub with `./run.sh <stub>`, read the failing tests, fill in the `???`, and repeat until green. Compare with the solution only after that. It contains the exercise contract from this file, word for word, and an empty table `| Section | Exercises |` that later phases fill.

`run.sh` gets `--exercises`, which does two things:

1. `scala-cli compile --test` on every stub under `exercises/`, excluding `exercises/solutions/`. A stub that does not compile fails the run.
2. `scala-cli test` on every file under `exercises/solutions/`. A failing solution fails the run.

`--all` and `--fast` never run stubs. A stub fails by design. If `--all exercises` is given, print a message pointing at `--exercises` and exit 1. `--help` and `--list` mention `--exercises`. `--list` shows stubs and solutions.

`scripts/check-inventory.sh` gets three new checks. Document them in its header comment.

- Every stub has a solution at the mirrored path, and every solution has a stub.
- Every stub contains `???`. No solution contains `???`.
- Every stub and solution contains `//> using scala` in its first 15 lines and a line starting with `package `. The stub's package starts with `exercises.` and the solution's with `solutions.`.

Extend the README backtick check so paths containing `exercises/` are checked too.

CI gets a job `exercises` that runs `./run.sh --exercises`, with `timeout-minutes` at least 60.

`README.md` gets a short `Exercises` section after the curriculum: one sentence and a link to `exercises/README.md`.

With no exercises yet, `./run.sh --exercises` prints that there is nothing to run and exits 0.

### Exit

`./run.sh --exercises` exits 0. `./scripts/check-inventory.sh` exits 0. The `exercises` CI job is green after the push. Commit. Push.

---

# Part B — Exercises (phases 52–65)

Each phase adds the stubs and solutions it lists, adds the row to the table in `exercises/README.md`, and runs:

- `./run.sh --exercises`, which exits 0;
- `./run.sh <one stub>`, which exits non-zero. Write in the commit message which test failed;
- `./scripts/check-inventory.sh`, which exits 0.

The tasks below are the minimum. Add tests, not tasks. Section directories use the lesson numbers: `exercises/01-basics/`, `exercises/02-functions/`, and so on.

## Phase 52 — Basics exercises

See also: `lessons/01-basics/`.

- `ex01_fizzbuzz`: `def fizz(n: Int): String`, written with `match` and guards. ⚠ Negative multiples of 3 are `Fizz` (`-3 % 3 == 0`), and 0 is `FizzBuzz`.
- `ex02_safe_add`: `def safeAdd(a: Int, b: Int): Option[Int]`, with `None` on overflow and no `Long` arithmetic. ⚠ `safeAdd(Int.MinValue, -1)`.
- `ex03_interpolator`: a `title"..."` string interpolator that capitalizes every interpolated argument and leaves the literal parts alone.
- `ex04_first_duplicate`: `def firstDuplicate(xs: List[Int]): Option[Int]`, using `boundary`/`break` with no `return`. ⚠ Duplicates that are not adjacent.

## Phase 53 — Functions exercises

See also: `lessons/02-functions/`.

- `ex01_compose_all`: `def composeAll[A](fs: List[A => A]): A => A`. ⚠ The empty list is `identity`. The order is left to right.
- `ex02_retry`: `def retry[A](times: Int)(action: => A): Either[Throwable, A]`. It re-evaluates the by-name argument. Tests count the evaluations.
- `ex03_digits`: a `@tailrec` `sumDigits(n: Long): Int`. ⚠ `Long.MinValue`.
- `ex04_collect_lengths`: build a `PartialFunction[Any, Int]` that is defined only for non-empty `String`s and `List`s, and use it with `collect`.

## Phase 54 — OOP exercises

See also: `lessons/03-oop/`.

- `ex01_stackable`: traits `Doubling` and `Incrementing` over an `IntQueue`. The tests check that mixin order changes the result.
- `ex02_percentage`: a case class with a private constructor and `Percentage.from(d: Double): Either[String, Percentage]`. A `compileErrors` test asserts that `Percentage(150)` does not compile, and stays green in both stub and solution.
- `ex03_traffic_light`: an enum with `next` and `durationSeconds`. ⚠ `values` order.
- `ex04_is_prime`: an extension method `Int.isPrime`. ⚠ 0, 1 and negative numbers.

## Phase 55 — Pattern matching exercises

See also: `lessons/04-pattern-matching/`.

- `ex01_email_extractor`: an `object Email` with `unapply(s: String): Option[(String, String)]`.
- `ex02_describe_list`: `def describe(xs: List[Int]): String` using sequence patterns, for empty, singleton, and starting with 0. Other cases print first and last.
- `ex03_expr_eval`: an `enum Expr` evaluator, plus `simplify` with the rules `x * 1`, `x + 0`, `0 * x`. ⚠ Nested simplification.

## Phase 56 — Collections exercises

See also: `lessons/05-collections/`.

- `ex01_top_words`: the top `n` words by frequency. ⚠ Ties are broken alphabetically.
- `ex02_group_runs`: `groupRuns(xs: List[Int]): List[List[Int]]` groups consecutive equal values.
- `ex03_window_max`: the maximum over a sliding window of size `k`. ⚠ `k` larger than the list.
- `ex04_primes`: `primes: LazyList[Int]`. The tests take the first 100.

## Phase 57 — Types exercises

See also: `lessons/06-types/`.

- `ex01_box_variance`: the stub declares an invariant `final case class Box[A](value: A)` with `def replace[B >: A](b: B): Box[B] = ???`. The reader makes `Box` covariant and implements `replace`. A "make this compile" test assigns a `Box[Int]` to a `Box[Any]`.
- `ex02_meters`: an opaque type `Meters` with `+` and `toFeet`. A `compileErrors` test asserts that `val m: Meters = 1.0` does not compile.
- `ex03_elem_match_type`: a match type `Elem[X]` and `def first[X](x: X): Elem[X]` for `String`, `List[t]` and `Option[t]`.
- `ex04_union_handler`: `def render(x: Int | String | Boolean): String`, exhaustive with no default case.

## Phase 58 — Contextual abstractions exercises

See also: `lessons/07-contextual/`.

- `ex01_show`: a `Show` type class. The stub has the `Int` and `String` instances with `???` bodies, and no `List` instance. The reader adds `given [A: Show]: Show[List[A]]`. A "make this compile" test summons `Show[List[List[Int]]]`.
- `ex02_monoid`: `Monoid` with `combineAll`, and instances for `Int` and `Map[K, V]` given `Monoid[V]`. ⚠ The empty list.
- `ex03_derived_eq`: `derived` for a hand-written `Eq` type class using `Mirror.ProductOf`, so `case class P(...) derives Eq` works.
- `ex04_transaction`: a context function `Tx ?=> A` and `def transaction[A](body: Tx ?=> A): (A, List[String])`.

## Phase 59 — Errors exercises

See also: `lessons/08-errors/`.

- `ex01_parse_config`: parse three fields into an accumulating `Either[List[String], Config]`. ⚠ All three bad fields report three errors.
- `ex02_using_order`: open three resources with `Using.Manager`. The tests check that they close in reverse order, even when the body throws.
- `ex03_try_to_either`: wrap a throwing API in `Try` and map it to a domain `enum` error. Fatal errors are not caught.

## Phase 60 — Concurrency exercises

See also: `lessons/09-concurrency/`.

- `ex01_parallel_sum`: split a `Vector[Long]` into `k` chunks, sum them in `Future`s, and combine. The tests use `Await` with a timeout.
- `ex02_first_success`: `firstSuccess[A](fs: List[Future[A]]): Future[A]` built on a `Promise`. ⚠ It fails only when every future fails.
- `ex03_counter`: a thread-safe counter. The tests run 8 threads × 10 000 increments.

The tests must be deterministic. No `Thread.sleep`-based assertions.

## Phase 61 — Metaprogramming exercises

See also: `lessons/10-metaprogramming/`. No macros: an exercise is one file.

- `ex01_inline_power`: `inline def power(x: Double, inline n: Int): Double`. A `compileErrors` test asserts that a non-constant `n` does not compile.
- `ex02_tuple_size`: `inline def size[T <: Tuple]: Int` using `constValue` or `erasedValue`.
- `ex03_checked_port`: `inline def port(inline n: Int): Int`, which raises `compiletime.error` outside 1..65535. A `compileErrors` test covers `port(0)`.

## Phase 62 — Advanced exercises

See also: `lessons/11-advanced/`.

- `ex01_state_monad`: a `State[S, A]` with `map` and `flatMap`, used in a for-comprehension that numbers the elements of a list.
- `ex02_strict_equality`: the stub compiles with `-language:strictEquality` through a `//> using options` line. The reader adds `derives CanEqual` so that a "make this compile" test passes.
- `ex03_named_stats`: `def stats(xs: List[Double]): Option[(min: Double, max: Double, mean: Double)]`. ⚠ The empty list returns `None` and does not throw.

## Phase 63 — Scala 2 migration exercises

See also: `lessons/12-scala2/`.

Each stub quotes a short Scala 2.13 snippet in a comment, built on an implicit class, an implicit parameter or an implicit conversion. The reader writes the Scala 3 equivalent with `extension`, `using`/`given` or `Conversion`. The tests check behaviour. Three exercises.

## Phase 64 — Typelevel exercises

See also: `ecosystem/typelevel/`.

Use the cats, cats-effect and munit-cats-effect versions already in the repo. Phase 46 pinned `munit-cats-effect`.

- `ex01_validated_form`: `ValidatedNec` for a three-field form, with `mapN`.
- `ex02_ref_counter`: an `IO` program that increments a `Ref` from 100 fibers. Suite type: `CatsEffectSuite`.
- `ex03_fs2_chunks`: an fs2 pipe that groups a stream into chunks of `n` and sums each chunk. ⚠ The last chunk is partial.

## Phase 65 — ZIO and Ox exercises

See also: `ecosystem/zio/` and `ecosystem/direct-style/`.

- `ex01_zio_typed_errors`: a ZIO program with an `enum` error type. Tested with ZIO Test (the version pinned in phase 34), so the suite type is `ZIOSpecDefault`. The MUnit directive is replaced by the zio-test deps. Both files still use identical directives.
- `ex02_ox_par_timeout`: with `//> using jvm 21`, `fetchBoth` runs two blocking calls with `par` and wraps them in `timeoutOption`. The tests use short fixed delays and assert the `Option`.

---

# Part C — Language depth (phases 66–75)

## Phase 66 — Overloading and inference

### Files

- `lessons/11-advanced/11_overloading_inference.scala`
- `README.md`, the advanced row

### Object

See also: `lessons/02-functions/01_methods.scala` and `lessons/06-types/01_type_hierarchy.scala`.

Sections:

- A lambda passed to an overloaded method gets parameter types only if every alternative agrees. One `typeCheckErrors` case where they don't.
- `x => x` with no expected type is an error.
- A Unit-returning lambda is ambiguous between `Runnable` and `Callable[T]` (the `ExecutorService.submit` case). Show the error and the fix.
- Overloading picks the most specific alternative: Int vs Long vs Double vs Any, with `Char` and `Long` arguments. Print which alternative ran.
- At most one overloaded alternative may have default arguments. Show the error.
- Inference flows left to right across parameter lists. Contrast `foldLeft(Nil)(...)` (error) with `foldLeft(List.empty[Int])(...)`.

### Exit

`./run.sh lessons/11-advanced/11_overloading_inference.scala` exits 0. Inventory passes. Commit. Push.

## Phase 67 — Given priority, measured

### Files

- `lessons/07-contextual/08_given_priority.scala`
- `README.md`, the contextual row

### Object

See also: `lessons/07-contextual/06_resolution_rules.scala`, which hedges that the rules for type specificity changed in Scala 3.7. This file measures the actual behaviour of 3.9.0 by printing which instance was chosen.

Cases:

- Both `given Monad[Option]` and `given Functor[Option]` are in scope while searching for `Functor[Option]`.
- `given [A]: Show[A]` and `given Show[Int]` are both in scope while searching for `Show[Int]`.
- The same two cases where the candidates sit at different nesting levels.

Each case prints the instance's name. If a case is ambiguous, show it through `typeCheckErrors` and print the message. In a comment, quote the 3.9.0 behaviour you observed and the compiler option that restores the old rule, only after checking it with scala-cli.

### Exit

The file runs. Every printed choice matches the comment beside it. Inventory passes. Commit. Push.

## Phase 68 — The initialization checker

### Files

- `lessons/11-advanced/12_safe_init.scala`
- `README.md`, the advanced row

### Object

See also: `lessons/03-oop/05_inheritance.scala` and `lessons/03-oop/03_traits.scala`, which show initialization-order bugs at runtime.

The file uses `//> using options -Wsafe-init`. It shows three patterns at runtime: a superclass reading an abstract `val`, an overridable method called from a constructor, and a forward reference between objects. Each pattern prints the `null` or `0` it observes. Each is followed by a comment quoting the exact `-Wsafe-init` warning scala-cli printed while compiling this file. The warnings appear in compile output. They are not errors, so the file still runs.

A final section shows the fixed version of each pattern, which produces no warning.

### Exit

The file runs. Each quoted warning matches the compiler output you saw, and the commit message says that the quotes were checked. Inventory passes. Commit. Push.

## Phase 69 — Lints and compiler flags

### Files

- `lessons/11-advanced/13_lints.scala`
- `README.md`, the advanced row

### Object

See also: `lessons/11-advanced/10_processes_and_io.scala` for running a subprocess.

The lesson writes one temp file. That file contains an unused import, an unused private method, a discarded non-Unit value, a non-Unit statement, a deprecated call and a feature that needs an import. The lesson runs `scala-cli compile --server=false` on it with `-Wunused:all -Wvalue-discard -Wnonunit-statement -deprecation -feature`. It prints the warning lines grouped by category, then compiles once more with `-Werror` and prints that compilation failed. Delete the temp file.

If `scala-cli` is not on `PATH`, print one line saying so and exit 0. CI has it, so CI exercises the real path.

A comment table lists the flags people should know: `-explain`, `-source future`, `-Xprint:typer`, `-Wconf`, `-Werror`, `-rewrite`.

### Exit

The file runs and prints at least four warning categories and the failed `-Werror` compilation. The temp file is gone. Inventory passes. Commit. Push.

## Phase 70 — Bytecode with javap

### Files

- `lessons/11-advanced/14_bytecode.scala`
- `README.md`, the advanced row

### Object

The lesson finds its own class files through `getProtectionDomain.getCodeSource` and runs `$JAVA_HOME/bin/javap -p` (and `-c` where useful) on classes it defines. It prints filtered lines showing:

- A case class's synthetic members: `copy`, `productArity`, `hashCode`, `equals`, and `unapply` on the companion.
- An `object`'s `MODULE$` field.
- A trait with a concrete method compiled to an interface default method.
- A `lazy val`'s generated holder or offset field. Describe what you see. Do not invent names.
- Boxing: a generic method called with `Int` shows `Integer.valueOf` or `BoxesRunTime`.
- An `inline def` that has no call left at its use site.

Each filtered output is followed by a one-line explanation.

### Exit

The file runs and prints `MODULE$`. Inventory passes. Commit. Push.

## Phase 71 — Benchmarks with JMH

### Files

- `ecosystem/benchmarks/01_collections.bench.scala`
- `run.sh`
- `.github/workflows/ci.yml`
- `ecosystem/README.md`

### Object

See also: `lessons/05-collections/07_performance_pitfalls.scala`, which times code by hand and says to use JMH.

A JMH benchmark file for `scala-cli --power run --jmh`, comparing:

- appending to a `List` versus a `Vector`;
- `Array[Int].sum` versus a `while` loop.

It uses `@Benchmark`, `@State`, `@Param` and `Blackhole`. The header shows the full command with short flags, `-- -wi 1 -i 2 -f 1 -w 1s -r 1s`. A comment explains warm-up, forks and why the hand-timed lesson can mislead.

`run.sh` runs `*.bench.scala` with those short flags, and `--fast` skips them. CI gets a `benchmarks` job on `workflow_dispatch` only.

The inventory check applies to this file. It has no `@main` and no `def section`, because JMH generates the entry point. Say so in its header comment.

### Exit

`./run.sh ecosystem/benchmarks/01_collections.bench.scala` exits 0 and prints the JMH result table. Inventory passes. Commit. Push.

## Phase 72 — Equality contracts

### Files

- `lessons/11-advanced/15_equality_contracts.scala`
- `README.md`, the advanced row

### Object

See also: `lessons/03-oop/01_classes.scala` and `lessons/11-advanced/02_multiversal_equality.scala`.

- A subclass that adds a field breaks the symmetry of `equals`. Fix it with `canEqual`. Print `a == b` and `b == a` before and after.
- An `Ordering` that is inconsistent with `equals` makes a `TreeSet` drop elements. ⚠ Order strings by length.
- `BigDecimal("1.0") == BigDecimal("1.00")` in Scala versus `equals` on `java.math.BigDecimal`, and the `hashCode` of each.
- `##` versus `hashCode` for numbers.
- A mutable key inside a `HashMap`. Link to the collections lesson that shows it. Do not repeat it.

### Exit

The file runs. Inventory passes. Commit. Push.

## Phase 73 — Pattern evaluation order

### Files

- `lessons/04-pattern-matching/04_evaluation_order.scala`
- `README.md`, the pattern-matching row

### Object

See also: `lessons/04-pattern-matching/02_extractors.scala`.

Count calls with counters and print the counts:

- How often the scrutinee expression runs: once.
- How often an extractor's `unapply` runs across several cases with the same extractor. Report what 3.9.0 does, measured.
- Guards evaluated only for cases whose pattern matched, with side effects visible.
- An extractor inside an alternative.
- A `PartialFunction` built from cases, used with `isDefinedAt` then `apply`. Link to `lessons/02-functions/06_partial_functions.scala` and do not repeat its guard example.

### Exit

The file runs. Each printed count matches the comment beside it. Inventory passes. Commit. Push.

## Phase 74 — Initialization under concurrency

### Files

- `lessons/09-concurrency/04_init_races.scala`
- `README.md`, the concurrency row

### Object

See also: `lessons/02-functions/04_by_name_laziness.scala` and `lessons/03-oop/02_objects_companions.scala`.

- A `lazy val` read by 16 threads at once runs its initializer exactly once. Print the count.
- Two top-level objects whose initializers reference each other, touched from two threads at the same time. Detect the deadlock: start both threads as daemon threads, `join` with a two-second timeout, and print whether each is still alive. The program must not hang, and it must exit 0. ⚠ The deadlock is the point of the demo. Do not let it block `@main`.
- A `lazy val` whose initializer throws is retried on the next access, on another thread.

### Exit

The file runs in under 30 seconds and prints the detected deadlock. Inventory passes. Commit. Push.

## Phase 75 — The numeric tower

### Files

- `lessons/11-advanced/16_numeric_tower.scala`
- `README.md`, the advanced row

### Object

See also: `lessons/01-basics/03_types_literals.scala`. Do not repeat its overflow and floating-point examples.

- Generic arithmetic with `Numeric`, `Integral` and `Fractional`. ⚠ A generic `average` that uses `Integral` truncates.
- `BigDecimal` with a `MathContext` and each `RoundingMode` on the same value.
- `Double.toString` is the shortest round-trip representation. Parse and print 0.1, 1e21 and 1e-7.
- `formatLocal` with `Locale.GERMANY` versus `Locale.US`. Do not change the default locale of the process.
- `Ordering[Double]` and NaN: the total ordering versus IEEE comparison.

### Exit

The file runs. Inventory passes. Commit. Push.

---

# Part D — More ecosystem (phases 76–85)

Every phase in this part adds one row to `ecosystem/README.md` and runs its file with `./run.sh <file>`.

## Phase 76 — Pekko Streams and Pekko HTTP

### Files

- `ecosystem/actors/02_pekko_streams_http.scala`

### Object

See also: `ecosystem/actors/01_pekko_typed.scala`. Use the same Pekko version (`1.7.1`) and a Pekko HTTP version that is compatible with it.

- Streams: `Source`, `Flow`, `Sink`, `throttle`, `mapAsync` with parallelism, and `grouped`. Print the materialized results.
- A Pekko HTTP server on port 0 with one JSON route. The same program calls it with the Pekko HTTP client and prints status and body. It unbinds and terminates the actor system before returning.

### Exit

The file runs and prints the HTTP status. Inventory passes. Commit. Push.

## Phase 77 — Caliban GraphQL

### Files

- `ecosystem/graphql/01_caliban.scala`

### Object

Derive a schema from case classes and run queries in-process, with no HTTP server:

- a valid query, printing the JSON response;
- the rendered SDL;
- an invalid field, printing the validation error;
- one query with an argument.

### Exit

The file runs and prints the SDL and the validation error. Inventory passes. Commit. Push.

## Phase 78 — Slick on H2

### Files

- `ecosystem/data/04_slick.scala`

### Object

See also: `ecosystem/data/01_doobie.scala`. Use the same H2 version.

Table definitions, schema creation, inserts, a join, `filter`/`map`/`sortBy`, a transaction with `.transactionally` that rolls back, and `.result.statements` to print the generated SQL. Close the database at the end.

### Exit

The file runs and prints the generated SQL and the rolled-back state. Inventory passes. Commit. Push.

## Phase 79 — Skunk on embedded Postgres

### Files

- `ecosystem/data/05_skunk.scala`

### Object

Start Postgres in-process with an embedded Postgres library that downloads binaries from Maven Central, with no Docker. `io.zonky.test:embedded-postgres` is the expected choice. Connect with Skunk through a `Session` resource. Show a `Query` with a codec, a `Command`, a prepared statement, and a transaction rolled back on error. Stop Postgres in a `Resource` finalizer.

If no embedded Postgres runs on ubuntu-latest CI without Docker, stop the run and report it. Do not substitute a different database under the Skunk name.

### Exit

The file runs locally and in CI. Postgres is stopped at the end, with no stray process left. Inventory passes. Commit. Push.

## Phase 80 — Kafka Streams without a broker

### Files

- `ecosystem/streaming/01_kafka_streams.scala`

### Object

Build a word-count topology with the Kafka Streams Java DSL from Scala 3, and drive it with `TopologyTestDriver` from `kafka-streams-test-utils`, with no broker. Pipe input records, read output records, and print the counts. Print `topology.describe()`. Close the driver.

### Exit

The file runs and prints the counts. Inventory passes. Commit. Push.

## Phase 81 — Laminar on jsdom

### Files

- `ecosystem/platforms/03_laminar.scala`
- `run.sh`
- `.github/workflows/ci.yml`

### Object

See also: `ecosystem/platforms/01_scala_js.scala`.

A Laminar component (a counter with a button and a reactive label) rendered under Node with jsdom. Simulate two clicks and print the label's text after each. The header documents the jsdom requirement and the exact scala-cli option or directive that enables it. If jsdom must be installed with npm, the header says `npm install jsdom`, CI's `platforms` job installs it, and `node_modules` stays untracked (add it to `.gitignore` if needed).

`--fast` skips this file. CI's `platforms` job runs it.

### Exit

`./run.sh ecosystem/platforms/03_laminar.scala` exits 0 locally after any documented npm install, and prints the label after each click. CI `platforms` is green. Inventory passes. Commit. Push.

## Phase 82 — Smithy4s

### Files

- `ecosystem/api/01_smithy4s.scala`

### Object

Smithy4s without a code generator: load a small Smithy model held in a string at runtime with `smithy4s-dynamic` and the Smithy model assembler, then use the dynamic schema to encode a value to JSON and decode it back. Print both. Decode an invalid document and print the error.

If `smithy4s-dynamic` cannot do this in one scala-cli file, stop the run and report what failed. Do not substitute generated code checked into the repo.

### Exit

The file runs and prints the encoded JSON and the decode error. Inventory passes. Commit. Push.

## Phase 83 — ZIO HTTP and zio-json

### Files

- `ecosystem/zio/03_zio_http.scala`

### Object

See also: `ecosystem/zio/01_zio.scala`. Use the same ZIO version.

A ZIO HTTP server on a free port with two routes, one of them JSON through zio-json `derives JsonCodec`. A ZIO HTTP client calls both and prints the responses. A 404 is shown. The server stops when the program's scope closes.

### Exit

The file runs and prints both responses and the 404. Inventory passes. Commit. Push.

## Phase 84 — Cask

### Files

- `ecosystem/lihaoyi/04_cask.scala`

### Object

See also: `ecosystem/lihaoyi/01_os_lib_requests.scala`.

A Cask routes object with `@cask.get`, `@cask.post` and a JSON endpoint. Start it on a free port found at runtime, call it with `requests`, print the responses, and stop the server. The program exits 0 on its own.

### Exit

The file runs and prints the responses. No process keeps running afterwards. Inventory passes. Commit. Push.

## Phase 85 — Magnolia

### Files

- `ecosystem/derivation/01_magnolia.scala`

### Object

See also: `lessons/07-contextual/03_derivation.scala`, which derives `Show` by hand with `Mirror`.

Derive the same kind of `Show` with Magnolia's `AutoDerivation` (or `Derivation`) for a case class, a nested case class, and a sealed trait or enum. Print the results. A comment compares the two approaches: what Magnolia's `join`/`split` replace, and what the hand-written version still controls.

### Exit

The file runs. Inventory passes. Commit. Push.

---

# Part E — Close

## Phase 86 — Docs and CI

### Files

- `README.md`
- `ecosystem/README.md`
- `exercises/README.md`
- `GLOSSARY.md`
- `.github/workflows/ci.yml`

### Object

- Every lesson file from phases 66–75 appears in backticks in its `README.md` curriculum row.
- Every ecosystem file from phases 71 and 76–85 has a row in `ecosystem/README.md`.
- `exercises/README.md` lists every exercise section with its exercise names.
- Add these terms to `GLOSSARY.md`, each pointing at the file that shows it: exercise stub, overloading resolution, given priority, `-Wsafe-init`, lint, `javap`, JMH, equality contract, numeric tower, Pekko Streams, GraphQL, embedded Postgres, `TopologyTestDriver`, Laminar, Smithy4s, Magnolia.
- CI has the jobs `inventory`, `lessons`, `ecosystem`, `exercises`, `platforms`, `build-tools`, `spark` (dispatch only) and `benchmarks` (dispatch only). Do not weaken a job to make it green.

### Exit

Inventory passes. Every glossary path exists. CI is green on push. Commit. Push.

## Phase 87 — Full verification

### Files

- None, unless a check fails.

### Object

Run, in this order:

1. `./scripts/check-inventory.sh`
2. `./run.sh --all lessons`
3. `./run.sh --fast ecosystem`
4. `./run.sh --exercises`
5. the three `ecosystem/platforms/` files
6. the sbt and Mill samples, and the cross-build project, as `ecosystem/README.md` documents them

If anything fails, fix it in this phase. A fix to a file from this program is allowed. A fix to a file from phases 1–49 is allowed only if the file is broken because a dependency it pins no longer resolves. Say so in the commit message.

Then confirm that the CI run for the final commit is green.

### Exit

All six steps exit 0, and CI is green. This phase always ends with one commit, `Phase 87: full verification`. It adds one sentence to `README.md`'s `Agents` section saying phases 50–87 are done, plus any fixes. The commit message lists the six results. Push. Stop.
