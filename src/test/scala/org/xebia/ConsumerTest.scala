package org.xebia

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import akka.camel.{Producer, CamelExtension, CamelMessage, Consumer}
import akka.testkit.TestKit
import akka.actor.{Actor, Props, ActorSystem}
import akka.pattern.ask
import scala.concurrent.util.duration._
import language.postfixOps
import akka.util.Timeout
import concurrent.Await
import akka.actor.IO.Failure

class ConsumerTest extends TestKit(ActorSystem("camel")) with WordSpec with MustMatchers{
  implicit val timeoutDuration = 10 seconds
  implicit val timeout = Timeout(timeoutDuration)
  implicit val ec = system.dispatcher
  val camel = CamelExtension(system)
  implicit val context = camel.context

  "A consumer" must {
    "Respond to a request from a producer" in {
      //Create a producer
      val producerActorRef = system.actorOf(Props(new MyFirstProducer))

      //Create a consumer
      val consumerActorRef = system.actorOf(Props(new MyFirstConsumer))

      // Wait for the consumer to be ready
      Await.ready(camel.activationFutureFor(consumerActorRef),10 seconds)

      // send a request response to the consumer, using the producer
      val future = producerActorRef ask "test"

      // Wait for the result
      val response = Await.result(future.mapTo[CamelMessage],10 seconds)
      response.bodyAs[String] must be ("TEST")
    }
  }
}

class MyFirstProducer extends Actor with Producer {
  // any camel URI will do
  def endpointUri = "direct:test"
}

class MyFirstConsumer extends Consumer {
  // any camel URI will do
  def endpointUri = "direct:test"

  def receive = {
    case msg:CamelMessage=>
      val body = msg.bodyAs[String]
      val headers = msg.headers
      println(s"Received body $body")
      println(s"Received headers $headers")
      sender ! msg.mapBody{ b:String=> b.toUpperCase }
    case _ => //don't care
  }
}
