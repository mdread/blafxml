package net.caoticode.blafxml

import java.io.FileReader

object ExampleApp extends App {
  val reader = new FileReader("/home/chaos/Source/scala/stanbolator/client/src/main/resources/testIndiciIBCFull.xml")

  println("START OF PROCEDURE")

  BlafParser(reader).forEach("c"){ xml =>
    println("----------- C NODE ---------")
    println(xml)
    println()
    println()
  }.forEach("unittitle"){ xml =>
    println("----------- UNITTITLE NODE ---------")
    println(xml)
    println()
    println()
  } process

  println("END OF PROCEDURE")
}