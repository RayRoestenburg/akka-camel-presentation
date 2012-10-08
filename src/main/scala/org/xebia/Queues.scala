package org.xebia

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.camel.{CamelExtension, Producer, CamelMessage, Consumer}
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.{Exchange, Processor}
import org.apache.activemq.camel.component.ActiveMQComponent
import concurrent.Await
import akka.util.Timeout
import scala.concurrent.util.duration._

/**
 * An example that shows both a file and a queue, providing the same input to
 * a system, using a route.
 */
object QueueSample extends App {
  implicit val timeoutDuration = 10 seconds
  implicit val timeout = Timeout(timeoutDuration)
  //Start the system
  val system = ActorSystem("queue-example")
  implicit val ec = system.dispatcher

  val camel = CamelExtension(system)
  val camelContext = camel.context
  //Add ActiveMQ as a Camel Component, so that URI's starting with activemq are handled.
  camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm:(broker:(tcp://localhost:6000)?persistent=false)"))

  camelContext.addRoutes(new RouteBuilder() {
    def configure() {
      from("file:///tmp/input").to("activemq:queue:Xebia")
// uncomment for some post-processing
//      from("file:///tmp/output").process(new Processor {
//        def process(exchange: Exchange) {
//          val lines = exchange.getIn.getBody(classOf[String])
//          exchange.getOut.setBody(s"Processed $lines")
//        }
//      }).to("file:///tmp/nextOutput")
    }
  })

  // Create a File Producer that will write to disk
  val fileProducerRef = system.actorOf(Props(new FileProducer("file:///tmp/output")))

  val internalActorRef = system.actorOf(Props(new InternalActor(fileProducerRef)))

  // Create a Queue Consumer that receives from activeMQ queue
  val qConsumerRef = system.actorOf(Props(new QConsumer("activemq:queue:Xebia", internalActorRef)))

  // Create a Queue Producer that produces to an activeMQ queue
  val qProducerRef = system.actorOf(Props(new QProducer("activemq:queue:Xebia")))

  // Wait for the Consumer to be activated.
  Await.ready(camel.activationFutureFor(qConsumerRef), 10 seconds)

  // send out some data
  qProducerRef tell ("some data that I need processed!")
}


class FileProducer(path:String) extends Actor with Producer {
  def endpointUri = path
}

class QProducer(queue:String) extends Actor with Producer{
  def endpointUri = queue
  override def oneway = true
}

class QConsumer(queue:String, next:ActorRef) extends Consumer {
  def endpointUri = queue

  def receive = {
    case msg:CamelMessage =>
     msg.body match {
       case m:String => next forward msg
       case m:Array[Byte] => next forward msg.mapBody{b:Array[Byte] => new String(b)}
     }
  }
}

class InternalActor(output:ActorRef) extends Actor {
  def receive = {
    case msg:CamelMessage =>
      output forward msg.mapBody{b:String =>
        // do some extremely intelligent processing
        b.map{ c => if(c.isWhitespace) '_' else c}
      }
  }
}
