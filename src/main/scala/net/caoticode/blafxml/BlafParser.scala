package net.caoticode.blafxml

import java.io.{FileReader, Reader}
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants, XMLInputFactory}
import xml.{NodeSeq, XML}

/**
 * @author Daniel Camarda (0xcaos@gmail.com)
 * */

object BlafParser{
  def apply(reader: Reader): BlafParser = {
    new BlafParser(reader)
  }
}

class BlafParser(reader: Reader) {

  private var listeners = Map[String, NodeSeq => Unit]()

  def forEach(nodeName: String)(callback: NodeSeq => Unit): BlafParser = {
    listeners += (nodeName -> callback)
    this
  }

  def process {
    val accumulators = scala.collection.mutable.Map[String, StringBuilder]()
    val factory = XMLInputFactory.newInstance()
    val xmlr = factory.createXMLStreamReader(reader)
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

            val xml = XML.loadString(accumulator.toString)
            listeners(xmlr.getLocalName)(xml)
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
      case e => throw e
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
