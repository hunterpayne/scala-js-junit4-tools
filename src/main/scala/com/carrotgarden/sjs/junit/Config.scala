package com.carrotgarden.sjs.junit

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.scalajs.core.tools.linker.ModuleKind

import Config.EnvConf
import Config.Module
import Config.WebConf

/**
 * Scala.js JavaScript VM testing session configuration.
 */
@SerialVersionUID( 1L )
case class Config(
  framework : String  = "com.novocode.junit.JUnitFramework",
  envConf :   EnvConf = EnvConf(),
  webConf :   WebConf = WebConf(),
  module :    Module  = Module()
)

object Config {

  /**
   * Scala.js JavaScript VM linker module.
   */
  @SerialVersionUID( 1L )
  case class Module(
    path : String = "./target/test-classes/META-INF/resources/script-test/runtime-test.js",
    // serializable
    kind : ModuleKind = ModuleKind.NoModule,
    // serializable
    name : Option[ String ] = None
  )

  /**
   * Scala.js JavaScript VM environment configuration.
   */
  @SerialVersionUID( 1L )
  case class EnvConf(
    envType : String = "nodejs-jsdom",
    envExec : String = "./test-tool/node/node",
    // serializable
    envArgs : List[ String ] = List(),
    // serializable
    envVars : Map[ String, String ] = Map( "NODE_PATH" -> "./test-tool/node/node_modules" )
  )

  /**
   * Webjars provisioning configuration.
   *
   * @param webjarsDir
   * 	webjars extract folder
   * @param scriptList
   * 	list of extracted scripts to make available during tests
   * 	these scripts are provided by some webjars
   *  these are relative paths, with version erasure
   *  example:
   *  archive entry: META-INF/resources/webjars/jquery/3.2.1/dist/jquery.js
   *  extract entry: jquery/dist/jquery.js
   */
  @SerialVersionUID( 1L )
  case class WebConf(
    webjarsDir : String = "./test-tool/webjars",
    // serializable
    scriptList : Seq[ String ] = Seq()
  )

  def charset = StandardCharsets.UTF_8

  def configExtract( file : File ) : Config = {
    val data = Files.readAllBytes( file.toPath )
    val text = new String( data, charset )
    configParse( text )
  }

  def configPersist( config : Config, file : File ) : Unit = {
    import java.nio.file.StandardOpenOption._
    val text = configUnparse( config )
    val data = text.getBytes( charset )
    Files.write( file.toPath, data, CREATE, SYNC )
  }

  import upickle._
  import upickle.default._

  implicit def codecConfig : ReadWriter[ Config ] = macroRW
  implicit def codecEnvConf : ReadWriter[ EnvConf ] = macroRW
  implicit def codecWebConf : ReadWriter[ WebConf ] = macroRW
  implicit def codecModule : ReadWriter[ Module ] = macroRW
  implicit def codecModuleKind : ReadWriter[ ModuleKind ] = macroRW

  def configParse( config : String ) : Config = read[ Config ]( config )
  def configUnparse( config : Config ) : String = write( config, indent = 4 )

}
