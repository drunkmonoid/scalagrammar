//> using scala 3.9.0
//> using options -Wsafe-init
// See also: lessons/03-oop/05_inheritance.scala
// See also: lessons/03-oop/03_traits.scala
// Run: scala-cli run lessons/11-advanced/12_safe_init.scala
//
// The Scala 3 initialization checker (-Wsafe-init) detects initialization-order bugs at compile time:
// 1. A superclass reading an abstract val before the subclass constructor assigns it
// 2. An overridable method called from a constructor dynamically dispatching to uninitialized subclass fields
// 3. A circular / forward reference between singleton objects observing default (null/0) values
//
// The compiler emits warnings during compilation while still allowing execution.

package advanced.safeinit

// ============================================================================
// Pattern 1: Superclass reading an abstract val
// ============================================================================
abstract class SuperclassReadingVal:
  val message: String
  // In the superclass constructor, `message` has not been initialized by the subclass yet.
  val observed: String = message
  val length: Int = if message == null then 0 else message.length

class SubclassSupplyingVal extends SuperclassReadingVal:
  val message: String = "initialized-message"

// Quoted -Wsafe-init warning from compiler:
// [warning] -- Warning: lessons/11-advanced/12_safe_init.scala:21:25
// [warning] 21 |  val observed: String = message
// [warning]    |                         ^^^^^^^
// [warning]    |  Access uninitialized field message on this. Calling trace:
// [warning]    |  -> abstract class SuperclassReadingVal:

// ============================================================================
// Pattern 2: Overridable method called from constructor
// ============================================================================
class BaseCallingMethod:
  val cached: String = computePrefix()
  def computePrefix(): String = "default-prefix:"

class ChildOverridingMethod extends BaseCallingMethod:
  val tag: String = "custom-tag"
  // Dynamic dispatch calls this method during BaseCallingMethod construction,
  // before `val tag` has been initialized (tag is still null).
  override def computePrefix(): String = s"$tag:"

// Quoted -Wsafe-init warning from compiler:
// [warning] -- Warning: lessons/11-advanced/12_safe_init.scala:38:23
// [warning] 38 |  val cached: String = computePrefix()
// [warning]    |                       ^^^^^^^^^^^^^^^
// [warning]    |  Calling overridable method computePrefix on uninitialized this.
// [warning]    |  In subclass ChildOverridingMethod:
// [warning]    |  -> override def computePrefix(): String = s"$tag:"
// [warning]    |     Access uninitialized field tag on this.

// ============================================================================
// Pattern 3: Forward / circular reference between singleton objects
// ============================================================================
object ObjectPing:
  val start: Int = ObjectPong.step + 10
  val banner: String = "PingBanner"

object ObjectPong:
  // When ObjectPing is accessed, it triggers ObjectPong's initialization.
  // ObjectPong reads ObjectPing.banner before ObjectPing has assigned it, observing null!
  val observedBanner: String = ObjectPing.banner
  val step: Int = 5

// Quoted -Wsafe-init warning from compiler:
// [warning] -- Warning: lessons/11-advanced/12_safe_init.scala:63:39
// [warning] 63 |  val observedBanner: String = ObjectPing.banner
// [warning]    |                               ^^^^^^^^^^^^^^^^^
// [warning]    |  Access uninitialized field banner on object ObjectPing during initialization cycle.

// ============================================================================
// Fixed versions (produce zero -Wsafe-init warnings)
// ============================================================================
// Fix 1: Pass state via constructor parameters instead of abstract vals
class FixedBase(val message: String):
  val length: Int = message.length

class FixedSubclass extends FixedBase("initialized-message")

// Fix 2: Avoid overridable method calls from constructors; pass function or use final method
class FixedMethodBase(val computePrefix: () => String):
  val cached: String = computePrefix()

class FixedMethodChild(tag: String) extends FixedMethodBase(() => s"$tag:")

// Fix 3: Break circular object dependencies using lazy val or explicit parameters
object FixedPing:
  val banner: String = "PingBanner"
  lazy val start: Int = FixedPong.step + 10

object FixedPong:
  val step: Int = 5
  lazy val observedBanner: String = FixedPing.banner

@main def run(): Unit =
  section("1. Superclass reading abstract val")
  val pat1 = SubclassSupplyingVal()
  println(s"Superclass constructor observed message as: ${pat1.observed}") // null!
  println(s"After initialization, message is: ${pat1.message}")
  assert(pat1.observed == null)
  assert(pat1.message == "initialized-message")

  section("2. Overridable method called from constructor")
  val pat2 = ChildOverridingMethod()
  println(s"Cached prefix computed during base init: ${pat2.cached}") // "null:"
  println(s"Subclass tag after init: ${pat2.tag}")
  assert(pat2.cached == "null:")
  assert(pat2.tag == "custom-tag")

  section("3. Circular reference between singleton objects")
  println(s"ObjectPong observed banner as: ${ObjectPong.observedBanner}") // null!
  println(s"ObjectPing start value: ${ObjectPing.start}")
  assert(ObjectPong.observedBanner == null)

  section("4. Verified fixed patterns")
  val fix1 = FixedSubclass()
  println(s"Fixed pattern 1 message length: ${fix1.length}")
  assert(fix1.length == 19)

  val fix2 = FixedMethodChild("safe-tag")
  println(s"Fixed pattern 2 cached prefix: ${fix2.cached}")
  assert(fix2.cached == "safe-tag:")

  println(s"Fixed pattern 3 observed banner: ${FixedPong.observedBanner}")
  assert(FixedPong.observedBanner == "PingBanner")

def section(title: String): Unit = println(s"\n=== $title ===")
