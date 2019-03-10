
package com.carrotgarden.sjs.junit

import java.io.File
import java.net.URLClassLoader

import org.webjars.WebJarExtractor

/**
 * Settings for testing.
 */
class SuiteSetupImpl(linker: LinkerImpl) {

  //System.setProperty( "scala-js-junit-tools.junit-event.print-debug", "true" )

  import Config._

  lazy val loader : ClassLoader = {
    println( "XXX loader" )
    val entryList = classpath.map( _.toURI().toURL() ).toArray
    new URLClassLoader( entryList )
  }

  lazy val webjars = {
    println( "XXX webjars" )
    val folder = new File( config.webConf.webjarsDir )
    folder.mkdirs()
    val extractor = new WebJarExtractor( loader )
    extractor.extractAllWebJarsTo( folder )
    folder
  }

  lazy val classpath : Seq[ File ] = {
    println( "XXX classpath" )
    Classer.currentClassPath
  }

  lazy val config : Config = {
    println( "XXX config" )
    val scriptList = Seq(
      "jquery/jquery.js"
    )
    val webConf = WebConf(
      scriptList = scriptList
    )
    val module = Config.Module( path = "./target/junit-tools.js" )
    Config(
      webConf = webConf,
      module  = module
    )
  }

  lazy val runtime : File = {
    println( "XXX runtime" )
    val runtime = new File( config.module.path )
    linker.link( classpath, runtime )
    runtime
  }

  //  lazy val tester : ScalaJSFramework = {
  //    println( "XXX tester" )
  //    runtime
  //    Context.cachedTester( config )
  //  }

  //  lazy val framework : Framework = {
  //    println( "XXX framework" )
  //    Context.cachedTester(config)
  //  }

}

object TestInit {

  def setup(setup: SuiteSetupImpl): Unit = {
    println( "@@@ setup" )
    val config = setup.config
    Context.configPersist( config )
    val runtime = setup.runtime
    val webjars = setup.webjars
  }  
}
