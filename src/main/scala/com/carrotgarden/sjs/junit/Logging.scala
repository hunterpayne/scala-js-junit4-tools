package com.carrotgarden.sjs.junit

/*
 * https://github.com/scala-js/scala-js/blob/02be3eafcce8d2c43ae4b133969a7d5817b74bc8/tools/js/src/test/scala/org/scalajs/core/tools/test/js/TestRunner.scala
 */

import org.slf4j.LoggerFactory

/**
 * A logger for JS-VM test environment output.
 */
object Logging {

  val consoleLogger = LoggerFactory.getLogger( "[JS-VM/console]" )

  val invokerLogger = LoggerFactory.getLogger( "[JS-VM/invoker]" )

  val managerLogger = LoggerFactory.getLogger( "[JS-VM/manager]" )

}

/**
 * A logger for JS-VM test environment output.
 */
class ConsoleLogger() extends org.scalajs.jsenv.JSConsole {
  import Logging._
  override def log( message : Any ) : Unit = {
    consoleLogger.info( "" + message )
  }
}

/**
 * A logger for JS-VM test environment output.
 */
class InvokerLogger() extends sbt.testing.Logger {
  import Logging._
  override def ansiCodesSupported() : Boolean = false
  override def debug( message : String ) : Unit = invokerLogger.debug( message )
  override def info( message : String ) : Unit = invokerLogger.info( message )
  override def warn( message : String ) : Unit = invokerLogger.warn( message )
  override def error( message : String ) : Unit = invokerLogger.error( message )
  override def trace( error : Throwable ) : Unit = invokerLogger.error( error.getMessage, error )
}

/**
 * A logger for JS-VM test environment output.
 */
class ManagerLogger() extends org.scalajs.core.tools.logging.Logger {
  import Logging._
  import org.scalajs.core.tools.logging.Level
  override def log( level : Level, message : => String ) : Unit = {
    level match {
      case Level.Debug => managerLogger.debug( message )
      case Level.Info  => managerLogger.info( message )
      case Level.Warn  => managerLogger.warn( message )
      case Level.Error => managerLogger.error( message )
    }
  }
  override def success( message : => String ) : Unit = {
    managerLogger.info( message )
  }
  override def trace( error : => Throwable ) : Unit = {
    managerLogger.error( error.getMessage, error )
  }
}
