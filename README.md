BlaFXML
=======

BlaFXML is a Scala/Java library to process big XML files combining the java streamig api for xml and a dom parser

Scala example
-------------

    BlafParser(reader).forEach("news"){ xml =>
        // do whatever you want with NodeSeq
        println((xml \ "title").text)
    }.forEach("author"){ xml =>
        // do whatever you want with NodeSeq
        println((xml \ "name").text)
    } process

Java example
------------

    new JBlafParser(reader).forEach("news", new BlafFunction(){
        @Override
        public void process(Document xml) {
            // do whatever you want with org.w3c.dom.Document
            System.out.println(xml.getFirstChild().getNodeName());
        }
    }).forEach("author", new BlafFunction(){
        @Override
        public void process(Document xml) {
            // do whatever you want with org.w3c.dom.Document
            System.out.println(xml.getFirstChild().getNodeName());
        }
    }).process();

