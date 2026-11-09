# Exercises

How to work through the exercises:

Run a stub with `./run.sh <stub>`, read the failing tests, fill in the `???`, and repeat until green. Compare with the solution only after that.

To run all exercises across the repository, use:

```bash
./run.sh --exercises
```

---

### Exercise contract

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

---

## Exercise Index

| Section | Exercises |
|---------|-----------|
| `01-basics` | `ex01_fizzbuzz`, `ex02_safe_add`, `ex03_interpolator`, `ex04_first_duplicate` |
| `02-functions` | `ex01_compose_all`, `ex02_retry`, `ex03_digits`, `ex04_collect_lengths` |
| `03-oop` | `ex01_stackable`, `ex02_percentage`, `ex03_traffic_light`, `ex04_is_prime` |
| `04-pattern-matching` | `ex01_email_extractor`, `ex02_describe_list`, `ex03_expr_eval` |
| `05-collections` | `ex01_top_words`, `ex02_group_runs`, `ex03_window_max`, `ex04_primes` |
| `06-types` | `ex01_box_variance`, `ex02_meters`, `ex03_elem_match_type`, `ex04_union_handler` |
| `07-contextual` | `ex01_show_tree`, `ex02_json_derivation`, `ex03_at_least_one`, `ex04_context_function` |
| `08-errors` | `ex01_parse_config`, `ex02_using_order`, `ex03_try_to_either` |
| `09-concurrency` | `ex01_parallel_sum`, `ex02_first_success`, `ex03_counter` |
| `10-metaprogramming` | `ex01_inline_power`, `ex02_tuple_size`, `ex03_checked_port` |
| `11-advanced` | `ex01_state_monad`, `ex02_strict_equality`, `ex03_named_stats` |
| `12-scala2` | `ex01_implicit_class`, `ex02_implicit_param`, `ex03_implicit_conversion` |
| `13-typelevel` | `ex01_validated_form`, `ex02_ref_counter`, `ex03_fs2_chunks` |
| `14-zio-direct` | `ex01_zio_typed_errors`, `ex02_ox_par_timeout` |
