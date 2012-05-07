package net.caoticode.blafxml

import _root_.java.io.Reader
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants, XMLInputFactory}
import xml.XML
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.dispatch.{Await, Future, ExecutionContext}
import akka.util.duration._

/**
 * @author Daniel Camarda (0xcaos@gmail.com)
 * */

object ordered {
  def apply(block: => Unit): Option[() => Unit] = {
    val f = () => try {
      block
    }catch{
      case e => e.printStackTrace()
    }

    Some[() => Unit](f)
  }
}

object unordered {
  def apply(block: => Unit): Option[() => Unit] = {
    try {
      block
    }catch{
      case e => e.printStackTrace()
    }
    None
  }
}

object BlafParser{
  def apply(reader: Reader): BlafParser = {
    new BlafParser(reader)
  }
}

class BlafParser(reader: Reader) {
  private var listeners = Map[String, XMLProcessor]()
  private val consumeFutures = (futures: List[Future[Any]]) => {
    for(future <- futures.reverseIterator){
      try{
        Await.result(future, 5 second) match {
          case Some(func: (() => Unit)) => func()
          case _ =>
        }
      }catch{
        case e => e.printStackTrace()
      }
    }
  }

  def forEach(nodeName: String)(callback: XMLProcessor): BlafParser = {
    listeners += (nodeName -> callback)
    this
  }

  def process {
    var counter = 0
    var results = List[Future[Any]]()

    val factory = XMLInputFactory.newInstance()
    val xmlr = factory.createXMLStreamReader(reader)
    val accumulators = scala.collection.mutable.Map[String, StringBuilder]()

    val config = ConfigFactory.load()
    implicit val system = ActorSystem("BlaFXML", config.getConfig("blafxml").withFallback(config))
    implicit val ec = ExecutionContext.defaultExecutionContext

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

            val f = ((name:String, xmlstr: String) => {
              Future {
                try{
                  listeners(name)(XML.loadString(xmlstr))
                } catch {
                  case e => {
                    e.printStackTrace() // TODO handle exceptions
                    println("XMLSTR = " + xmlstr)
                    None
                  }
                }
              }
            })(xmlr.getLocalName, accumulator.toString)

            counter += 1
            if (counter % 40 == 0){
              consumeFutures(results)

              results = f :: Nil
            } else{
              results = f :: results
            }
          }
          case XMLStreamConstants.END_ELEMENT => {
            for (accumulator <- accumulators)
              appendEndElement(accumulator._2, xmlr)

          }

          case XMLStreamConstants.CHARACTERS => {
            for (accumulator <- accumulators)
              accumulator._2.append(scala.xml.Text(new String(xmlr.getTextCharacters, xmlr.getTextStart, xmlr.getTextLength)).toString())
          }

          case XMLStreamConstants.CDATA => {
            for (accumulator <- accumulators){
              accumulator._2
                .append("<![CDATA[")
                .append(scala.xml.Text(new String(xmlr.getTextCharacters, xmlr.getTextStart, xmlr.getTextLength)).toString())
                .append("]]>")
            }
          }

          case _ =>
        }

        xmlr.next()
      }

      consumeFutures(results)
    } catch {
      case e => e.printStackTrace() // TODO handle exception
    } finally {
      xmlr.close()
      reader.close()
      system.shutdown()
    }
  }

  private def appendStartElement(sb: StringBuilder, xmlr: XMLStreamReader) {
    sb.append("<" + xmlr.getLocalName)
    for (i <- 0 until xmlr.getAttributeCount) {
      sb.append(" " + xmlr.getAttributeLocalName(i) + "=\"" + scala.xml.Text(xmlr.getAttributeValue(i)).toString() + "\"")
    }
    sb.append(">")
  }

  private def appendEndElement(sb: StringBuilder, xmlr: XMLStreamReader) {
    sb.append("</" + xmlr.getLocalName + ">")
  }
}

