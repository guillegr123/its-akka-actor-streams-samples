package com.its.akkaactorstreamsamples.actorsource

import akka.actor.Actor

object NumbersActor {
  import akka.actor.{ ActorRef, Props }

  final case class GetNumbers(dest: ActorRef, numbers: Int)
  final case class NumberResponse(number: Int)

  val name = "MovieAggregator"

  def props
    = Props(new NumbersActor)
}

class NumbersActor
  extends Actor {
  import akka.actor.{ PoisonPill, Status }

  import NumbersActor.{ GetNumbers, NumberResponse }

  override def receive: Receive = {
    case GetNumbers(dest, numbers) =>
      for (n <- 1 to numbers) dest ! NumberResponse(n)
      dest ! Status.Success
      self ! PoisonPill
  }
}

object ActorSourceSample extends App {
  import scala.concurrent.{ Await, ExecutionContext }
  import scala.concurrent.duration._

  import akka.actor.ActorSystem
  import akka.stream.{ ActorMaterializer, OverflowStrategy }
  import akka.stream.scaladsl.{Keep, Sink, Source}

  import NumbersActor._

  implicit val system: ActorSystem = ActorSystem("ActorSourceSampleSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val numbersSource
    = Source.actorRef[NumberResponse](Int.MaxValue, OverflowStrategy.dropNew)
    .map {
      case NumberResponse(number) => number
    }

  val (sourceActor, resultFuture) = numbersSource.toMat(Sink.seq[Int])(Keep.both).run()

  val numbersActor = system.actorOf(NumbersActor.props, NumbersActor.name)

  numbersActor ! GetNumbers(sourceActor, 10)

  val numbers = Await.result(resultFuture.map(_.asInstanceOf[Seq[Int]]), 10 second)

  numbers.foreach(n => println(n))

  system.terminate()

  Await.ready(system.whenTerminated, 5 seconds)
}