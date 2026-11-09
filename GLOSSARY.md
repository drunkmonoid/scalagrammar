# Scala 3 Glossary

A reference of essential concepts, terms, and keywords in Scala 3 and its ecosystem, with pointers to the files demonstrating them.

### `@main`
An annotation designating an entry-point method that generates a standard executable main class. It supports command-line arguments parsed automatically by the compiler.  
Demonstrated in: [`lessons/01-basics/01_hello.scala`](lessons/01-basics/01_hello.scala)

### `val` and `lazy val`
`val` defines an immutable value evaluated eagerly at definition time. `lazy val` delays evaluation until first access and caches the result thread-safely without deadlocks.  
Demonstrated in: [`lessons/01-basics/02_values_variables.scala`](lessons/01-basics/02_values_variables.scala)

### significant indentation
Scala 3's optional indentation-based syntax using `:` and whitespace instead of braces (`{ ... }`) for blocks, classes, and control flow structures.  
Demonstrated in: [`lessons/01-basics/07_syntax_styles.scala`](lessons/01-basics/07_syntax_styles.scala)

### by-name parameter
A method parameter declared with `=> T` whose argument expression is passed unevaluated and re-evaluated on every reference within the method body.  
Demonstrated in: [`lessons/02-functions/04_by_name_laziness.scala`](lessons/02-functions/04_by_name_laziness.scala)

### dependent function
A function type whose result type depends on the value of one or more preceding parameters, expressed as `(x: T) => x.Out`.  
Demonstrated in: [`lessons/02-functions/07_dependent_functions.scala`](lessons/02-functions/07_dependent_functions.scala)

### `@tailrec`
A compiler annotation verifying that a recursive method occupies constant stack space via tail-call elimination; produces a compile-time error if the recursion is not in tail position.  
Demonstrated in: [`lessons/02-functions/05_recursion_tailrec.scala`](lessons/02-functions/05_recursion_tailrec.scala)

### case class
An immutable class with compiler-generated structural equality (`equals`/`hashCode`), `copy` method, extractor pattern matching, and universal apply.  
Demonstrated in: [`lessons/03-oop/04_case_classes.scala`](lessons/03-oop/04_case_classes.scala)

### enum as an ADT
A concise language construct declaring Algebraic Data Types (sum types) with parameterized cases and compiler-checked pattern matching exhaustiveness.  
Demonstrated in: [`lessons/03-oop/06_enums_adts.scala`](lessons/03-oop/06_enums_adts.scala)

### extension method
A method attached to an existing type without modifying its source code or incurring runtime allocation overhead, declared via `extension (x: T) def ...`.  
Demonstrated in: [`lessons/03-oop/08_extension_methods.scala`](lessons/03-oop/08_extension_methods.scala)

### export
A clause that exposes selected members of an aggregated object directly in the enclosing scope, favoring composition over inheritance.  
Demonstrated in: [`lessons/03-oop/09_export_composition.scala`](lessons/03-oop/09_export_composition.scala)

### extractor
An object defining an `unapply` or `unapplySeq` method that decomposes values during pattern matching.  
Demonstrated in: [`lessons/04-pattern-matching/02_extractors.scala`](lessons/04-pattern-matching/02_extractors.scala)

### variance
Subtyping relationships for generic types: covariant (`+T`), contravariant (`-T`), or invariant (`T`), ensuring type-safe substitution.  
Demonstrated in: [`lessons/06-types/02_generics_variance.scala`](lessons/06-types/02_generics_variance.scala)

### `Factory`
The collection companion abstraction that encapsulates how to construct a target collection type from arbitrary element streams or builder operations.  
Demonstrated in: [`lessons/05-collections/08_factories.scala`](lessons/05-collections/08_factories.scala)

### opaque type
A zero-overhead type abstraction whose underlying representation is visible only inside its defining scope and treated as completely distinct elsewhere.  
Demonstrated in: [`lessons/06-types/10_opaque_types.scala`](lessons/06-types/10_opaque_types.scala)

### union type
A type `A | B` representing values that can be either an instance of `A` or an instance of `B`, without requiring a shared class hierarchy.  
Demonstrated in: [`lessons/06-types/05_union_intersection.scala`](lessons/06-types/05_union_intersection.scala)

### match type
A type-level pattern match that reduces to different types depending on the input type arguments at compile time.  
Demonstrated in: [`lessons/06-types/07_type_lambdas_match_types.scala`](lessons/06-types/07_type_lambdas_match_types.scala)

### `given` and `using`
Scala 3's contextual abstraction keywords: `given` declares contextual values or instances, and `using` requests them as contextual parameters.  
Demonstrated in: [`lessons/07-contextual/01_given_using.scala`](lessons/07-contextual/01_given_using.scala)

### type class
A functional design pattern separating interface definition from data type implementation, instantiated through `given` instances.  
Demonstrated in: [`lessons/07-contextual/02_type_classes.scala`](lessons/07-contextual/02_type_classes.scala)

### `NotGiven`
A special contextual type that succeeds only when no `given` instance for a specific type can be found in the current scope, enabling negative implicit evidence.  
Demonstrated in: [`lessons/07-contextual/07_notgiven.scala`](lessons/07-contextual/07_notgiven.scala)

### derivation and `Mirror`
Compile-time synthesis of type class instances for case classes and enums using compiler-generated `Mirror` structural metadata.  
Demonstrated in: [`lessons/07-contextual/03_derivation.scala`](lessons/07-contextual/03_derivation.scala)

### `Option`, `Either`, `Try`
Idiomatic functional containers for representing optional values (`Some`/`None`), disjoint computations or typed failures (`Right`/`Left`), and exception-handling results (`Success`/`Failure`).  
Demonstrated in: [`lessons/08-errors/02_option_either_try.scala`](lessons/08-errors/02_option_either_try.scala)

### `boundary` and `break`
A label-safe, non-local control flow mechanism providing early exit from nested expressions without manual exception handling or stack unwinding overhead.  
Demonstrated in: [`lessons/01-basics/05_control_flow.scala`](lessons/01-basics/05_control_flow.scala)

### `Future`
A standard library abstraction representing an asynchronous computation whose result becomes available at some point, managed by an `ExecutionContext`.  
Demonstrated in: [`lessons/09-concurrency/02_futures_promises.scala`](lessons/09-concurrency/02_futures_promises.scala)

### `inline`
A modifier signaling the compiler to inline method bodies or evaluate conditionals at call sites, serving as the basis for Scala 3 metaprogramming.  
Demonstrated in: [`lessons/10-metaprogramming/01_inline.scala`](lessons/10-metaprogramming/01_inline.scala)

### quote and splice
The core primitives of Scala 3 macros: quotes (`'{ ... }`) turn typed expressions into code trees, while splices (`${ ... }`) insert code trees into quotes.  
Demonstrated in: [`lessons/10-metaprogramming/macros/Main.scala`](lessons/10-metaprogramming/macros/Main.scala)

### staging
Multi-stage programming with `scala.quoted.staging` allowing code to be constructed, typed, compiled, and executed at runtime within the same process.  
Demonstrated in: [`lessons/10-metaprogramming/04_staging.scala`](lessons/10-metaprogramming/04_staging.scala)

### multiversal equality
Strict compile-time equality checking enabled via `strictEquality`, requiring a `CanEqual[L, R]` given instance before comparing types `L` and `R` with `==` or `!=`.  
Demonstrated in: [`lessons/11-advanced/02_multiversal_equality.scala`](lessons/11-advanced/02_multiversal_equality.scala)

### explicit nulls
A compiler flag (`-Yexplicit-nulls`) making `Null` a distinct reference type separate from `AnyRef`, turning null pointer bugs into compile-time type errors.  
Demonstrated in: [`lessons/11-advanced/06_explicit_nulls.scala`](lessons/11-advanced/06_explicit_nulls.scala)

### capture checking
An experimental capability-based type system tracking what side-effecting resources and capabilities values capture within their types.  
Demonstrated in: [`lessons/11-advanced/07_experimental_capture_checking.scala`](lessons/11-advanced/07_experimental_capture_checking.scala)

### Scala 2 `implicit`
The unified Scala 2 mechanism encompassing implicit parameters, implicit conversions, and implicit classes, superseded in Scala 3 by `given`, `using`, and `extension`.  
Demonstrated in: [`lessons/12-scala2/01_scala2_idioms.scala`](lessons/12-scala2/01_scala2_idioms.scala)

### `IO`
The Cats Effect purely functional data structure representing a suspended asynchronous, concurrent, and cancelable computation with managed resources.  
Demonstrated in: [`ecosystem/typelevel/03_cats_effect.scala`](ecosystem/typelevel/03_cats_effect.scala)

### ZIO
A zero-allocation, purely functional effect type `ZIO[R, E, A]` modeling computations requiring environment `R`, failing with typed error `E`, or succeeding with `A`.  
Demonstrated in: [`ecosystem/zio/01_zio.scala`](ecosystem/zio/01_zio.scala)

### Iron
A zero-overhead compile-time and runtime type refinement library for Scala 3 powered by inline methods and opaque types.  
Demonstrated in: [`ecosystem/refined/01_iron.scala`](ecosystem/refined/01_iron.scala)

### exercise stub
A starter test file containing `???` definitions and failing tests that a reader implements, paired with an exact mirrored solution.  
Demonstrated in: [`exercises/README.md`](exercises/README.md)

### overloading resolution
The compiler process of selecting the most specific method alternative among overloads, influenced by parameter type inference across argument lists and SAM conversions.  
Demonstrated in: [`lessons/11-advanced/11_overloading_inference.scala`](lessons/11-advanced/11_overloading_inference.scala)

### given priority
The rules determining which contextual instance the compiler chooses when multiple candidates are in scope, where lexical nesting and subtype specificity take precedence.  
Demonstrated in: [`lessons/07-contextual/08_given_priority.scala`](lessons/07-contextual/08_given_priority.scala)

### `-Wsafe-init`
The Scala 3 compiler flag activating the safe initialization checker, detecting uninitialized field reads, forward references, and virtual method dispatches during construction.  
Demonstrated in: [`lessons/11-advanced/12_safe_init.scala`](lessons/11-advanced/12_safe_init.scala)

### lint
Compiler diagnostic flags (such as `-Wunused:all`, `-Wvalue-discard`, and `-Wnonunit-statement`) that catch potential bugs, dead code, and unhandled expressions at compile time.  
Demonstrated in: [`lessons/11-advanced/13_lints.scala`](lessons/11-advanced/13_lints.scala)

### `javap`
The Java class file disassembler used to inspect compiled JVM bytecode, synthetic methods, singleton `MODULE$` fields, and inlined method call sites.  
Demonstrated in: [`lessons/11-advanced/14_bytecode.scala`](lessons/11-advanced/14_bytecode.scala)

### JMH
The Java Microbenchmark Harness, providing isolated JVM forks, warm-up iterations, and dead-code elimination safeguards via `Blackhole` for reliable benchmarking.  
Demonstrated in: [`ecosystem/benchmarks/01_collections.bench.scala`](ecosystem/benchmarks/01_collections.bench.scala)

### equality contract
The mathematical equivalence relation requirement (`reflexive`, `symmetric`, `transitive`, `consistent`) for `equals` and `hashCode`, preserved in subtyping hierarchies via `canEqual`.  
Demonstrated in: [`lessons/11-advanced/15_equality_contracts.scala`](lessons/11-advanced/15_equality_contracts.scala)

### numeric tower
The algebraic hierarchy of generic numeric type classes (`Numeric`, `Integral`, `Fractional`) and floating-point total orderings vs IEEE 754 NaN semantics.  
Demonstrated in: [`lessons/11-advanced/16_numeric_tower.scala`](lessons/11-advanced/16_numeric_tower.scala)

### Pekko Streams
A reactive streams implementation on the Apache Pekko actor foundation providing backpressured asynchronous streaming, throttling, and concurrent stages.  
Demonstrated in: [`ecosystem/actors/02_pekko_streams_http.scala`](ecosystem/actors/02_pekko_streams_http.scala)

### GraphQL
A query language and execution engine for APIs providing schema-driven querying; in Scala, Caliban derives type-safe GraphQL schemas directly from case classes.  
Demonstrated in: [`ecosystem/graphql/01_caliban.scala`](ecosystem/graphql/01_caliban.scala)

### embedded Postgres
An in-process PostgreSQL database runner (`io.zonky.test:embedded-postgres`) that downloads and executes native PostgreSQL binaries for brokerless integration testing.  
Demonstrated in: [`ecosystem/data/05_skunk.scala`](ecosystem/data/05_skunk.scala)

### `TopologyTestDriver`
The Kafka Streams test utility that executes a streaming topology synchronously and deterministically in-memory without requiring a running Kafka broker cluster.  
Demonstrated in: [`ecosystem/streaming/01_kafka_streams.scala`](ecosystem/streaming/01_kafka_streams.scala)

### Laminar
A reactive UI library for Scala.js built on Airstream signals and observable streams, enabling declarative DOM manipulation with zero virtual-DOM overhead.  
Demonstrated in: [`ecosystem/platforms/03_laminar.scala`](ecosystem/platforms/03_laminar.scala)

### Smithy4s
A Scala code-generation and dynamic interpretation library for AWS Smithy interface definition models, providing dynamic schema codecs without build-time compilation.  
Demonstrated in: [`ecosystem/api/01_smithy4s.scala`](ecosystem/api/01_smithy4s.scala)

### Magnolia
A fast, macro-based generic type class derivation library for Scala providing `join` and `split` combinators for automated product and sum typeclass generation.  
Demonstrated in: [`ecosystem/derivation/01_magnolia.scala`](ecosystem/derivation/01_magnolia.scala)
