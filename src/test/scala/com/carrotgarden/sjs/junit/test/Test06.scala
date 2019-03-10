package com.carrotgarden.sjs.junit.test

import org.junit.Test
import org.junit.Assert._
import org.junit.Ignore

/**
 * Invoked in JS-VM.
 *
 * Detected by Scala.js JUnit runtime, since using JUnit 4.
 */
class Test06 {

  @Test
  def verifyPrint(): Unit = {
    println( s"### Message from JS-VM ${getClass.getName} ###" )
  }

  @Test
  def verifyVM(): Unit = {
    // https://github.com/scala-js/scala-js/blob/master/javalanglib/src/main/scala/java/lang/System.scala
    assertEquals( "Running in Scala.js VM", "Scala.js", System.getProperty( "java.vm.name" ) )
  }

  @Ignore
  @Test
  def verifyIgnore(): Unit = {
    throw new Exception( "Should not happen." )
  }

}
