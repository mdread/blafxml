package net.caoticode.blafxml

import java.io.Reader
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants, XMLInputFactory}
import xml.XML
import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.routing.FromConfig

/**
 * @author Daniel Camarda (0xcaos@gmail.com)
 * */


object BlafParser{
  def apply(reader: Reader): BlafParser = {
    new BlafParser(reader)
  }
}

class BlafParser(reader: Reader) {
  private var listeners = Map[String, XMLProcessor]()

  def forEach(nodeName: String)(callback: XMLProcessor): BlafParser = {
    listeners += (nodeName -> callback)
    this
  }

  def process {
    val config = ConfigFactory.load()
    val system = ActorSystem("BlaFXML", config.getConfig("blafxml").withFallback(config))
    val counter = system.actorOf(Props[Counter], "counter")
    val router = system.actorOf(Props(new Worker(counter)).withRouter(FromConfig()), "workersrouter")

    val factory = XMLInputFactory.newInstance()
    val xmlr = factory.createXMLStreamReader(reader)
    val accumulators = scala.collection.mutable.Map[String, StringBuilder]()

    try{
      while(xmlr.hasNext){
        xmlr.getEventType match {
          case XMLStreamConstants.START_ELEMENT if listeners.contains(xmlr.getLocalName) =>{
            val sb = StringBuilder.newBuilder
            accumulators.put(xmlr.getLocalName, sb)

            for (accumulator <- accumulators)
              appendStartElement(accumulator._2, xmlr)
          }
          case XMLStreamConstants.START_ELEMENT => {
            for (accumulator <- accumulators)
              appendStartElement(accumulator._2, xmlr)
          }

          case XMLStreamConstants.END_ELEMENT if listeners.contains(xmlr.getLocalName) =>{
            for (accumulator <- accumulators)
              appendEndElement(accumulator._2, xmlr)

            // get a reference to the actual accumulator and remove it from the Map
            val accumulator = accumulators(xmlr.getLocalName)
            accumulators.remove(xmlr.getLocalName)

            counter ! Increment
            router ! Process(listeners(xmlr.getLocalName), accumulator.toString)
          }
          case XMLStreamConstants.END_ELEMENT => {
            for (accumulator <- accumulators)
              appendEndElement(accumulator._2, xmlr)

          }

          case XMLStreamConstants.CHARACTERS => {
            for (accumulator <- accumulators)
              accumulator._2.append(new String(xmlr.getTextCharacters, xmlr.getTextStart, xmlr.getTextLength))
          }

          case XMLStreamConstants.CDATA => {
            for (accumulator <- accumulators){
              accumulator._2
                .append("<![CDATA[")
                .append(new String(xmlr.getTextCharacters, xmlr.getTextStart, xmlr.getTextLength))
                .append("]]>")
            }
          }

          case _ =>
        }

        xmlr.next()
      }
    } catch {
      case e => e.printStackTrace() // TODO handle exception
    } finally {
      xmlr.close()
    }

    counter ! StartCountdown
    system.awaitTermination()
  }

  private def appendStartElement(sb: StringBuilder, xmlr: XMLStreamReader) {
    sb.append("<" + xmlr.getLocalName)
    for (i <- 0 until xmlr.getAttributeCount) {
      sb.append(" " + xmlr.getAttributeLocalName(i) + "=\"" + xmlr.getAttributeValue(i) + "\"")
    }
    sb.append(">")
  }

  private def appendEndElement(sb: StringBuilder, xmlr: XMLStreamReader) {
    sb.append("</" + xmlr.getLocalName + ">")
  }
}

class Worker(counter: ActorRef) extends Actor {
  def receive = {
    case Process(processor, xml) => try{
      processor(XML.loadString(xml))
    } catch {
      case e => e.printStackTrace() // TODO handle exceptions (sending back an Exception message?)
    }
    finally {
      counter ! Decrement
    }
  }
}

class Counter extends Actor {
  var counter = 0
  var countdown = false

  def receive = {
    case Increment => counter += 1;
    case Decrement => {
      counter -= 1
      if (countdown && counter == 0)
        context.system.shutdown()
    }
    case StartCountdown => countdown = true
  }
}

case object Increment
case object Decrement
case object StartCountdown
case class Process(processor: XMLProcessor, xml: String)