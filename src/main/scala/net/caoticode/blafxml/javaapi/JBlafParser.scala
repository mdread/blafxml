package net.caoticode.blafxml.javaapi

import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import java.io.{StringReader, Reader}

/**
 * java version of the blafxml parser
 */

trait BlafFunction {
  def process(xml: Document)
}

class JBlafParser(reader: Reader) {
  val blaf = net.caoticode.blafxml.BlafParser(reader)

  def forEach(nodeName: String, function: BlafFunction): JBlafParser = {
    blaf.forEach(nodeName){ xml =>
      val dbf = DocumentBuilderFactory.newInstance();
      val db = dbf.newDocumentBuilder();
      val inputSource =  new InputSource(new StringReader(xml.toString()))
      val doc = db.parse(inputSource);

      function.process(doc)
    }

    this
  }

  def process() {
    blaf.process
  }
}
