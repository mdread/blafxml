package net.caoticode.blafxml

import java.io.Reader
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants, XMLInputFactory}
import xml.XML
import com.typesafe.config.ConfigFactory
import akka.routing.FromConfig
import akka.util.duration._
import akka.pattern.gracefulStop
import akka.dispatch.{Future, Await}
import akka.actor._

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
    println("STARTING MASTER ACTOR")
    system.actorOf(Props[Master]) ! Start(reader, listeners)
    println("WAITING FOR TERMINATION")
    system.awaitTermination()
    println("SYSTEM TERMINATED")
  }
}

class Master extends Actor{
  private var counter = 0
  private val router = context.system.actorOf(Props[Worker].withRouter(FromConfig()), "workersrouter")

  def receive = {
    case Start(reader, listeners) => {
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

              router ! Process(listeners(xmlr.getLocalName), accumulator.toString)
              counter += 1
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
        case e => // TODO handle exception
      } finally {
        xmlr.close()
      }
    }

    case WorkerDone => {
      counter -= 1
      println("COUNTER = " + counter)
      if (counter == 0)
        context.system.shutdown()
    }
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

class Worker extends Actor {
  def receive = {
    case Process(processor, xml) => try{
      processor(XML.loadString(xml))
    } catch {
      case e =>
    } // TODO handle exceptions (sending back an Exception message?)
    finally {
      sender ! WorkerDone
    }
  }
}

case class Start(reader: Reader, listeners: Map[String, XMLProcessor])
case object WorkerDone
case class Process(processor: XMLProcessor, xml: String)