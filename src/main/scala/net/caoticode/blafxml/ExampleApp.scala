package net.caoticode.blafxml

import _root_.java.io.{FileInputStream, FileReader}


object ExampleApp extends App {
//  val reader = new FileReader("/home/chaos/Source/scala/stanbolator/docs/xslt/EsempioOutputAnnotato.xml")
//  val reader = new FileReader("/home/chaos/tmp/uploads/castelsanpietro.xml")
  val reader = new FileInputStream("/home/chaos/tmp/uploads/castelsanpietro.xml")



//  val reader = new FileReader("/home/chaos/tmp/uploads/testIndiciCodronchiJr.xml")
  println("START OF PROCEDURE")

//  BlafParser(reader).forEach("c"){ xml =>
////    println("----------- C NODE ---------")
//    println(xml)
//    println()
////    println()
//  }.forEach("unittitle"){ xml =>
////    println("----------- UNITTITLE NODE ---------")
//    println(xml)
//    println()
////    println()
//  }.process

//  BlafParser(reader).forEach("c"){xml => println((xml \ "@id").text)}.process
//

  BlafParser(reader).forEach("c"){xml =>
    val id = (xml \ "@id").text
    val scopecontent = (xml \ "scopecontent").text
    ordered {
      println(id)
      println(scopecontent)
      println("-" * 50)
    }
  }.process

//  BlafParser(reader).forEach("c"){xml =>
//    unordered{
//      val id = (xml \ "@id").text
//      println(id)
//    }
//  }.process

  println("END OF PROCEDURE")
}