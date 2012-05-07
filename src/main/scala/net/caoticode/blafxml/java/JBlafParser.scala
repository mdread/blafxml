package net.caoticode.blafxml.java

import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import net.caoticode.blafxml.ordered
import java.io.{InputStream, StringReader, Reader}

/**
* java version of the blafxml parser
*/

trait BlafFunction {
  def process(xml: Document)
  def processOrdered(xml: Document)
}

class JBlafParser(reader: InputStream) {
  private val blaf = net.caoticode.blafxml.BlafParser(reader)

  def forEach(nodeName: String, function: BlafFunction): JBlafParser = {
    blaf.forEach(nodeName){ xml =>
      val dbf = DocumentBuilderFactory.newInstance();
      val db = dbf.newDocumentBuilder();
      val inputSource =  new InputSource(new StringReader(xml.toString()))
      val doc = db.parse(inputSource);

      function.process(doc)

      ordered {
        function.processOrdered(doc)
      }
    }

    this
  }

  def process() {
    blaf.process
  }
}
