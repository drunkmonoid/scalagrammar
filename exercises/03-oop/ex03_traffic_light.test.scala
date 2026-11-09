//> using scala 3.9.0
//> using test.dep org.scalameta::munit:1.3.6
// See also: lessons/03-oop/06_enums_adts.scala

package exercises.oop.trafficlight

// Task:
// Implement an enum `TrafficLight` with three cases: `Red`, `Amber`, and `Green`.
// Each case must specify a `durationSeconds: Int`: Red = 30, Amber = 5, Green = 45.
// Implement `def next: TrafficLight` cycling: Red -> Green, Green -> Amber, Amber -> Red.
// ⚠ Ensure enum cases are declared in the order Red, Amber, Green so TrafficLight.values is [Red, Amber, Green].

enum TrafficLight(val durationSeconds: Int):
  def next: TrafficLight = ???
  case Red extends TrafficLight(30)
  case Amber extends TrafficLight(5)
  case Green extends TrafficLight(45)

class TrafficLightSuite extends munit.FunSuite:
  test("cycles through states correctly") {
    assertEquals(TrafficLight.Red.next, TrafficLight.Green)
    assertEquals(TrafficLight.Green.next, TrafficLight.Amber)
    assertEquals(TrafficLight.Amber.next, TrafficLight.Red)
  }

  test("has expected duration in seconds for each state") {
    assertEquals(TrafficLight.Red.durationSeconds, 30)
    assertEquals(TrafficLight.Amber.durationSeconds, 5)
    assertEquals(TrafficLight.Green.durationSeconds, 45)
  }

  test("preserves declaration order in values") { // ⚠
    val expected = List(TrafficLight.Red, TrafficLight.Amber, TrafficLight.Green)
    assertEquals(TrafficLight.values.toList, expected)
  }
