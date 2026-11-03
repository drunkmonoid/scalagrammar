//> using scala 3.9.0
//> using dep org.apache.pekko::pekko-actor-typed:1.7.1
//> using dep org.slf4j:slf4j-nop:2.0.20
// Run: scala-cli run ecosystem/actors/01_pekko_typed.scala
//
// Apache Pekko: the open-source (Apache 2.0) fork of Akka 2.6, after Akka changed its license.
// Actors are isolated units of state that communicate ONLY by asynchronous messages:
//   * an actor processes one message at a time → no locks around its state
//   * Behavior[Msg] describes how to react to a message, and returns the NEXT behavior
//   * ActorRef[Msg] is a typed address; `!` (tell) is fire-and-forget, `ask` expects a reply
//   * parents supervise children: on failure they can restart, resume or stop them

package ecosystem.actors.pekkodemo

import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.util.Timeout
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*

// ---------- a bank account: state lives in the behavior's parameters ----------
object Account:
  sealed trait Command
  final case class Deposit(amount: Long) extends Command
  final case class Withdraw(amount: Long, replyTo: ActorRef[Result]) extends Command
  final case class GetBalance(replyTo: ActorRef[Long]) extends Command
  case object Crash extends Command

  sealed trait Result
  case object Ok extends Result
  final case class Rejected(reason: String) extends Result

  def apply(balance: Long = 0): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match
        case Deposit(a) =>
          ctx.log.info(s"deposit $a")
          apply(balance + a)                                   // new state = new behavior
        case Withdraw(a, replyTo) if a > balance =>
          replyTo ! Rejected(s"insufficient funds: balance $balance < $a")
          Behaviors.same
        case Withdraw(a, replyTo) =>
          replyTo ! Ok
          apply(balance - a)
        case GetBalance(replyTo) =>
          replyTo ! balance
          Behaviors.same
        case Crash =>
          throw IllegalStateException("simulated failure")
    }

// ---------- a guardian that spawns and supervises children ----------
object Guardian:
  sealed trait Command
  final case class GetAccount(replyTo: ActorRef[ActorRef[Account.Command]]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    // Restart the account on failure. Restarting resets its state (balance back to 100).
    val account = ctx.spawn(
      Behaviors.supervise(Account(balance = 100)).onFailure[IllegalStateException](SupervisorStrategy.restart),
      "account")
    Behaviors.receiveMessage { case GetAccount(replyTo) =>
      replyTo ! account
      Behaviors.same
    }
  }

// ---------- a timer-driven actor ----------
object Ticker:
  case object Tick
  def apply(remaining: Int, done: ActorRef[String]): Behavior[Tick.type] =
    Behaviors.withTimers { timers =>
      timers.startTimerAtFixedRate(Tick, 50.millis)
      def counting(n: Int): Behavior[Tick.type] = Behaviors.receiveMessage { _ =>
        println(s"  tick ${remaining - n + 1}")
        if n == 1 then { done ! "ticker finished"; Behaviors.stopped } else counting(n - 1)
      }
      counting(remaining)
    }

@main def run(): Unit =
  val system = ActorSystem(Guardian(), "scalagrammar")
  given ActorSystem[?] = system
  given Timeout = 3.seconds
  def await[A](f: Future[A]): A = Await.result(f, 3.seconds)

  try
    println("=== ask the guardian for its child ===")
    // the reply type cannot be inferred from `GetAccount(_)`, so it is given explicitly
    val account = await(system.ask[ActorRef[Account.Command]](Guardian.GetAccount(_)))
    println(s"  got ${account.path}")

    println("\n=== tell (fire and forget) and ask (request/response) ===")
    account ! Account.Deposit(50)
    println(s"  balance: ${await(account.ask(Account.GetBalance(_)))}")
    println(s"  withdraw 500: ${await(account.ask(Account.Withdraw(500, _)))}")
    println(s"  withdraw 120: ${await(account.ask(Account.Withdraw(120, _)))}")
    println(s"  balance: ${await(account.ask(Account.GetBalance(_)))}")

    println("\n=== supervision: a crash restarts the child with its INITIAL state ===")
    account ! Account.Crash
    Thread.sleep(100)
    println(s"  balance after restart: ${await(account.ask(Account.GetBalance(_)))}")

    println("\n=== messages are processed one at a time: no race on the balance ===")
    (1 to 1000).foreach(_ => account ! Account.Deposit(1))
    println(s"  balance after 1000 concurrent deposits: ${await(account.ask(Account.GetBalance(_)))}")

    println("\n=== timers ===")
    // A tiny listener actor bridges back to non-actor code by completing a Promise
    val done = scala.concurrent.Promise[String]()
    val listener = system.systemActorOf(
      Behaviors.receiveMessage[String] { m => done.success(m); Behaviors.stopped }, "listener")
    system.systemActorOf(Ticker(3, listener), "ticker")
    println(s"  ${await(done.future)}")
  finally
    system.terminate()
    Await.ready(system.whenTerminated, 5.seconds)
