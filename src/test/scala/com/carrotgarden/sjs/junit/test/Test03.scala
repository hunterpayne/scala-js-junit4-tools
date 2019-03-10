package com.carrotgarden.sjs.junit.test

import org.junit._
//import org.junit.Assert._

import scala.scalajs.js.annotation.JSExportTopLevel
import org.scalajs.jquery.jQuery
import org.scalajs.dom
import dom.document

/**
 * Invoked in JS-VM.
 *
 * Detected by Scala.js JUnit runtime, since using JUnit 4.
 */
class Test03 {

  import Test03._

  @Test
  def verifyPrint(): Unit = {
    println(s"### Message from JS-VM ${getClass.getName} ###")
  }

  @Test()
  def verifyDOM(): Unit = {
    appendParagraph(document.body, "Hello DOM")
  }

  @Test()
  def verifyJQuery(): Unit = {
    jQuery("body").append("<p>Hello JQuery</p>")
  }
}

object Test03 {

  def appendParagraph(targetNode: dom.Node, text: String): Unit = {
    val parNode = document.createElement("p")
    val textNode = document.createTextNode(text)
    parNode.appendChild(textNode)
    targetNode.appendChild(parNode)
  }

  @JSExportTopLevel("addClickedMessage")
  def addClickedMessage(): Unit = {
    appendParagraph(document.body, "You clicked the button!")
  }
}
