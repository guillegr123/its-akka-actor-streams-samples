package com.its.akkaactorstreamsamples.actorsource

import akka.actor.{Actor, ActorRef}

import scala.collection.immutable
import scala.concurrent.Future

object NumbersActor {
  import akka.actor.{ ActorRef, Props }

  // Messages
  final case class GetNumbers(dest: ActorRef, numbers: Int)
  final case class NumberResponse(number: Int)

  // Actor metadata
  val name: String = "NumbersActor"

  def props: Props = Props(new NumbersActor)
}

// Actor that generates a sequence of numbers, upon receiving a GetNumbers message
class NumbersActor
  extends Actor {
  import akka.actor.{ PoisonPill, Status }

  import NumbersActor.{ GetNumbers, NumberResponse }

  override def receive: Receive = {
    case GetNumbers(dest, numbers) =>
      for (n <- 1 to numbers) dest ! NumberResponse(n)  // Send each number to the destination
      dest ! Status.Success // Notify that all the numbers were sent, via an special message
      self ! PoisonPill // Kill the current actor
  }
}

object ActorSourceSample extends App {
  import scala.concurrent.{ Await, ExecutionContext }
  import scala.concurrent.duration._

  import akka.actor.ActorSystem
  import akka.stream.{ ActorMaterializer, OverflowStrategy }
  import akka.stream.scaladsl.{Keep, Sink, Source}

  import NumbersActor._

  // Get implicits for actors and streams
  implicit val system: ActorSystem = ActorSystem("ActorSourceSampleSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  // Define an actor source
  val numbersSource: Source[Int, ActorRef]
    = Source.actorRef[NumberResponse](Int.MaxValue, OverflowStrategy.dropNew)
        .map {
          // Unbox the number
          case NumberResponse(number) => number
        }

  // Materialize, using a sequence as sink, and run
  val (sourceActor: ActorRef, resultFuture: Future[immutable.Seq[Int]]) = numbersSource.toMat(Sink.seq[Int])(Keep.both).run()

  // Create an instance of NumbersActor, and get its reference
  val numbersActor: ActorRef = system.actorOf(NumbersActor.props, NumbersActor.name)

  // Tell the actor to generate the numbers, and send them to the source actor
  numbersActor ! GetNumbers(sourceActor, 10)

  // Wait for the results to be piled up in the sequence
  val numbers: Seq[Int] = Await.result(resultFuture.map(_.asInstanceOf[Seq[Int]]), 10 second)

  // Print the resulting sequence
  numbers.foreach(n => println(n))

  // Shutdown
  system.terminate()

  Await.ready(system.whenTerminated, 5 seconds)
}