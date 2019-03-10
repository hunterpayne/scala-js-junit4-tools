package com.carrotgarden.sjs.junit

import org.junit.runner.Runner
import org.junit.runner.Description
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner

/**
 * JUnit 4 block runner for Scala.js.
 *
 * TODO
 */
case class ScalaJS_Runner( testClass : Class[ _ ] ) extends BlockJUnit4ClassRunner( testClass ) {

  //  override def getDescription() : Description = {
  //    Description.createTestDescription( testClass, "Scala.js" )
  //  }

}
