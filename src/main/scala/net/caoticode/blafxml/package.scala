package net.caoticode

package object blafxml {
  type XMLProcessor = (xml.NodeSeq => Option[() => Unit])
}
