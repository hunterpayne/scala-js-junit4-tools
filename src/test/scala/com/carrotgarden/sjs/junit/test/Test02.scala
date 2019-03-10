package com.carrotgarden.sjs.junit.test

import org.junit._
import org.junit.Assert._

/**
 * Invoked in JS-VM.
 *
 * Detected by Scala.js JUnit runtime, since using JUnit 4.
 */
class Test02 {

  @Test
  def verifyPrint(): Unit = {
    println( s"### Message from JS-VM ${getClass.getName} ### VM " + System.getProperty( "java.vm.name" ) )
  }

  /**
   * Not supported in Scala.js: "expected".
   */
  @Ignore
  @Test( expected = classOf[ Exception ] )
  def verifyException(): Unit = {
    throw new Exception()
  }

}
