//> using scala 3.9.0
//> using platform js
//> using jsModuleKind common
//> using jsDom
//> using dep com.raquo::laminar::17.2.0
// See also: ecosystem/platforms/01_scala_js.scala
// Requirement: npm install jsdom
// Run: scala-cli run ecosystem/platforms/03_laminar.scala
//
// Laminar reactive UI component running under Node.js with jsdom:
// 1. Reactive state management using Var and Signal
// 2. DOM tree mounting with render(container, component)
// 3. Simulating synthetic browser user events (click)
// 4. Reactive text binding and DOM assertion verification

package ecosystem.platforms.laminardemo

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scalajs.dom.document

@main def run(): Unit =
  section("1. Mounting Laminar component into jsdom")
  val container = document.createElement("div")
  container.id = "app-root"
  document.body.appendChild(container)

  val countVar = Var(0)

  val counterComponent = div(
    idAttr := "counter-box",
    button(
      idAttr := "increment-button",
      onClick --> (_ => countVar.update(_ + 1)),
      "Click Me"
    ),
    span(
      idAttr := "counter-label",
      "Count: ",
      child.text <-- countVar.signal.map(_.toString)
    )
  )

  render(container, counterComponent)

  val buttonElem = document.getElementById("increment-button").asInstanceOf[dom.html.Button]
  val labelElem = document.getElementById("counter-label").asInstanceOf[dom.html.Span]

  println(s"Initial label text: '${labelElem.textContent}'")
  assert(labelElem.textContent == "Count: 0")

  section("2. Simulating user click 1")
  buttonElem.click()
  println(s"Label text after first click: '${labelElem.textContent}'")
  assert(labelElem.textContent == "Count: 1")

  section("3. Simulating user click 2")
  buttonElem.click()
  println(s"Label text after second click: '${labelElem.textContent}'")
  assert(labelElem.textContent == "Count: 2")

  println("\nLaminar component verified under jsdom successfully.")

def section(title: String): Unit = println(s"\n=== $title ===")
