package net.caoticode.blafxml

import java.io.FileReader

object ExampleApp extends App {
//  val reader = new FileReader("/home/chaos/Source/scala/stanbolator/docs/xslt/EsempioOutputAnnotato.xml")
  val reader = new FileReader("/home/chaos/Source/scala/stanbolator/client/src/main/resources/testIndiciIBCFull_2.xml")
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

  BlafParser(reader).forEach("c"){xml => println((xml \ "@id").text)}.process


  println("END OF PROCEDURE")
}