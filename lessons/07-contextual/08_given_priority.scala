//> using scala 3.9.0
// See also: lessons/07-contextual/06_resolution_rules.scala
// Run: scala-cli run lessons/07-contextual/08_given_priority.scala
//
// Measuring Given Priority in Scala 3.9.0:
// 1. Subtype specificity: Monad[F] vs Functor[F] at the same scope
// 2. Monomorphic vs Polymorphic specificity: Show[Int] vs [A] => Show[A]
// 3. Lexical nesting precedence over type specificity
// 4. Ambiguity when two candidates have equal priority

package contextual.priority

import scala.compiletime.testing.typeCheckErrors

trait Functor[F[_]]:
  def name: String

trait Monad[F[_]] extends Functor[F]

trait Show[A]:
  def name: String

@main def run(): Unit =
  section("1. Subtype specificity at the same scope: Monad[Option] vs Functor[Option]")
  // Both Monad[Option] and Functor[Option] are in scope while searching for Functor[Option].
  // In Scala 3.9.0, Monad[Option] <: Functor[Option], so Monad[Option] is strictly more specific
  // and is chosen by given resolution.
  locally {
    given monadOpt: Monad[Option] with
      def name: String = "monadOpt (Monad[Option])"

    given functorOpt: Functor[Option] with
      def name: String = "functorOpt (Functor[Option])"

    val chosen = summon[Functor[Option]]
    println(s"Chosen instance: ${chosen.name}")
    assert(chosen.name == "monadOpt (Monad[Option])")
  }

  section("2. Monomorphic vs Polymorphic specificity: Show[Int] vs [A] => Show[A]")
  // Both generic [A] => Show[A] and concrete Show[Int] are in scope for Show[Int].
  // In Scala 3.9.0, the monomorphic candidate without type parameters is strictly more
  // specific than the generic candidate and is chosen.
  locally {
    given genericShow: [A] => Show[A] with
      def name: String = "genericShow ([A] => Show[A])"

    given intShow: Show[Int] with
      def name: String = "intShow (Show[Int])"

    val chosen = summon[Show[Int]]
    println(s"Chosen instance: ${chosen.name}")
    assert(chosen.name == "intShow (Show[Int])")
  }

  section("3. Lexical nesting beats type specificity: outer Monad vs inner Functor")
  // Even though Monad is more specific than Functor, an inner scope candidate
  // shadows and takes precedence over outer scope candidates.
  locally {
    given outerMonad: Monad[Option] with
      def name: String = "outerMonad (Monad[Option])"

    locally {
      given innerFunctor: Functor[Option] with
        def name: String = "innerFunctor (Functor[Option])"

      val chosen = summon[Functor[Option]]
      println(s"Chosen instance: ${chosen.name}")
      assert(chosen.name == "innerFunctor (Functor[Option])")
    }
  }

  section("4. Lexical nesting beats monomorphic specificity: outer Show[Int] vs inner Show[A]")
  locally {
    given outerIntShow: Show[Int] with
      def name: String = "outerIntShow (Show[Int])"

    locally {
      given innerGenericShow: [A] => Show[A] with
        def name: String = "innerGenericShow ([A] => Show[A])"

      val chosen = summon[Show[Int]]
      println(s"Chosen instance: ${chosen.name}")
      assert(chosen.name == "innerGenericShow ([A] => Show[A])")
    }
  }

  section("5. Ambiguity when two candidates have equal priority at the same scope")
  // When two candidates at the same lexical level have identical types (or mutually unrelated types),
  // neither is more specific, causing an ambiguity compile error.
  val ambErrors = typeCheckErrors("""
    trait Tag[A]
    given tagA: Tag[String] = new Tag[String] {}
    given tagB: Tag[String] = new Tag[String] {}
    scala.compiletime.summonInline[Tag[String]]
  """)
  println(s"Ambiguity error: ${ambErrors.head.message}")

  // Note on Scala 3.7+ changes:
  // Prior to Scala 3.7, certain subtle subtype specificity comparisons between complex
  // type constructors could produce ambiguity or unexpected tie-breakers.
  // In Scala 3.7+ and 3.9.0, candidate specificity is strictly ordered by subtyping and type arguments.
  // The `-source:3.5` compiler option can be used to restore pre-3.7 resolution semantics if needed.

def section(title: String): Unit = println(s"\n=== $title ===")
