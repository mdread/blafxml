package net.caoticode.blafxml

import java.io.Reader
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants, XMLInputFactory}
import xml.XML
import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem, Actor}
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
  private val accumulators = scala.collection.mutable.Map[String, StringBuilder]()

  def forEach(nodeName: String)(callback: XMLProcessor): BlafParser = {
    listeners += (nodeName -> callback)
    this
  }

  def process {
    val config = ConfigFactory.load()
    println(config)
    val system = ActorSystem("BlaFXML", config.getConfig("blafxml").withFallback(config))
    val router = system.actorOf(Props[Worker].withRouter(FromConfig()), "workersrouter")



    val factory = XMLInputFactory.newInstance()
    val xmlr = factory.createXMLStreamReader(reader)
//    val futures = scala.collection.mutable.Set[Future[Any]]()

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

//            val xml = XML.loadString(accumulator.toString)
//            listeners(xmlr.getLocalName)(xml)
            router ! Process(listeners(xmlr.getLocalName), accumulator.toString)
//            futures.add(listeners(xmlr.getLocalName) !! Process(accumulator.toString))
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

      // wait for all the actors to end
//      futures.foreach(_.apply())
    } catch {
      case e => throw e
    } finally {
      // Stop all the actors
//      listeners.foreach(_._2 ! Stop)
      xmlr.close()
      system.shutdown()
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
    case Process(processor, xml) => processor(XML.loadString(xml))
  }
}

case object Stop
case class Process(processor: XMLProcessor, xml: String)