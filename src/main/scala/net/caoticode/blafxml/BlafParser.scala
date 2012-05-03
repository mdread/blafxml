package net.caoticode.blafxml

import _root_.java.io.Reader
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants, XMLInputFactory}
import scala.actors.Future
import scala.actors.Futures._
import xml.{NodeSeq, XML}

/**
 * @author Daniel Camarda (0xcaos@gmail.com)
 * */

object ordered {
  def apply(block: => Unit): Option[() => Unit] = {
    val f = () => block
    Some[() => Unit](f)
  }
}

object unordered {
  def apply(block: => Unit): Option[() => Unit] = {
    block
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
              future {
                try{
                  listeners(name)(XML.loadString(xmlstr))
                } catch {
                  case e => {
                    e.printStackTrace() // TODO handle exceptions
                    None
                  }
                }
              }
            })(xmlr.getLocalName, accumulator.toString)

            counter += 1
            if (counter % 40 == 0){
              results.foreach(_.apply() match {
                case Some(func: (() => Unit)) => func()
                case _ =>
              })
              results = List(f)
            } else{
              results = results ::: List(f)
            }
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

      results.foreach(_.apply() match {
        case Some(func: (() => Unit)) => func()
        case _ =>
      })
    } catch {
      case e => e.printStackTrace() // TODO handle exception
    } finally {
      xmlr.close()
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

