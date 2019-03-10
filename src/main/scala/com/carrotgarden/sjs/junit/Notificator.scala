package com.carrotgarden.sjs.junit

import scala.util.matching.Regex

import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier

import sbt.testing.Event
import sbt.testing.EventHandler
import sbt.testing.NestedTestSelector
import sbt.testing.Selector
import sbt.testing.Status

/**
 * Convert SBT testing events into JUnit notification events.
 */
class Notificator( rootMeta : Description, notifier : RunNotifier ) extends EventHandler {

  import Notificator._

  lazy val logger = new HandlerLogger( rootMeta, notifier )

  /**
   * Notify test result at a given level.
   */
  def fireFinished( nodeMeta : Description, event : Event ) = {
    notifier.fireTestFinished( nodeMeta )
  }

  /**
   * Notify test result at a given level.
   */
  def fireIgnored( nodeMeta : Description, event : Event ) = {
    notifier.fireTestIgnored( nodeMeta )
  }

  /**
   * Notify test result at a given level.
   */
  def fireFailure( nodeMeta : Description, event : Event ) = {
    val error =
      if ( event.throwable.isDefined ) event.throwable.get
      else new Exception()
    notifier.fireTestFailure( new Failure( nodeMeta, error ) )
  }

  /**
   * Convert SBT testing events into JUnit notification events.
   */
  override def handle( event : Event ) : Unit = {

    // println( s"HANDLE ${event.status()} @ ${event.selector()}" )

    val nodeMeta : Description = event.selector() match {
      case selector : NestedTestSelector =>
        val nested = selector.asInstanceOf[ NestedTestSelector ]
        // TODO should always find
        findNodeMeta( rootMeta, nested ).getOrElse( rootMeta )
      case selector : Selector =>
        // TODO support more types
        rootMeta
    }

    event.status match {
      // success
      case Status.Success  => // JUnit is success by default.
      // failure
      case Status.Error    => fireFailure( nodeMeta, event )
      case Status.Failure  => fireFailure( nodeMeta, event )
      // ignored
      case Status.Skipped  => fireIgnored( nodeMeta, event )
      case Status.Ignored  => fireIgnored( nodeMeta, event )
      case Status.Canceled => fireIgnored( nodeMeta, event )
      case Status.Pending  => fireIgnored( nodeMeta, event )
    }

    // FIXME duplicate
    fireFinished( nodeMeta, event )

  }

}

object Notificator {

  /**
   * Locate nested test notification target.
   */
  // Hard coded in org.scalajs.junit: className.methodName
  def findNodeMeta(
    rootMeta : Description,
    selector : NestedTestSelector
  ) : Option[ Description ] = {
    val className = selector.suiteId()
    val methodName = selector.testName()
    if ( rootMeta.getClassName == className && rootMeta.getMethodName == methodName ) {
      Some( rootMeta )
    } else {
      val iter = rootMeta.getChildren.iterator()
      while ( iter.hasNext() ) {
        val nodeMeta = iter.next()
        val option = findNodeMeta( nodeMeta, selector )
        if ( option.isDefined ) {
          return option
        }
      }
      None
    }
  }

  def hasClass( nodeMeta : Description, selector : String ) : Boolean = {
    selector != null && nodeMeta.getClassName != null && selector.contains( nodeMeta.getClassName )
  }

  def hasMethod( nodeMeta : Description, selector : String ) : Boolean = {
    selector != null && nodeMeta.getMethodName != null && selector.contains( nodeMeta.getMethodName )
  }

  // Hard coded in org.scalajs.junit.JUnitExecuteTest: className.methodName
  def findNodeMeta(
    rootMeta : Description,
    selector : String
  ) : Option[ Description ] = {
    if ( hasClass( rootMeta, selector ) && hasMethod( rootMeta, selector ) ) {
      Some( rootMeta )
    } else {
      val iter = rootMeta.getChildren.iterator()
      while ( iter.hasNext() ) {
        val nodeMeta = iter.next()
        val option = findNodeMeta( nodeMeta, selector )
        if ( option.isDefined ) {
          return option
        }
      }
      None
    }
  }

  def hasRegexMatch( regex : Regex, selector : String ) = {
    regex.pattern.matcher( selector ).matches()
  }

  /**
   * Hack around Scala.js assumptions: recover started/finished events from logs.
   */
  class HandlerLogger( rootMeta : Description, notifier : RunNotifier )
    extends sbt.testing.Logger with Reference {

    override def ansiCodesSupported() : Boolean = false
    override def debug( message : String ) : Unit = fireEvent( message )
    override def info( message : String ) : Unit = fireEvent( message )
    override def warn( message : String ) : Unit = fireEvent( message )
    override def error( message : String ) : Unit = fireEvent( message )
    override def trace( error : Throwable ) : Unit = ()

    lazy val referenceEvent = referenceJUnitEvent

    /**
     * Hack around Scala.js assumptions: recover started/finished events from logs.
     */
    def fireEvent( message : String ) : Unit = {
      import referenceEvent._

      if ( message == null ) {
        // NOOP
      } else if ( fireTest.fromLogs && hasRegexMatch( regex.testStarted, message ) ) {
        if ( printDebug ) println( s"FIRE test started ${rootMeta}" )
        notifier.fireTestStarted( rootMeta )
      } else if ( fireTest.fromLogs && hasRegexMatch( regex.testFinished, message ) ) {
        if ( printDebug ) println( s"FIRE test finished ${rootMeta}" )
        notifier.fireTestFinished( rootMeta )
      } else if ( fireMethod.fromLogs && hasRegexMatch( regex.methodStarted, message ) ) {
        findNodeMeta( rootMeta, message ).map { nodeMeta =>
          if ( printDebug ) println( s"FIRE method started ${nodeMeta}" )
          notifier.fireTestStarted( nodeMeta )
        }
      } else if ( fireMethod.fromLogs && hasRegexMatch( regex.methodFinished, message ) ) {
        findNodeMeta( rootMeta, message ).map { nodeMeta =>
          if ( printDebug ) println( s"FIRE method finished ${nodeMeta}" )
          notifier.fireTestFinished( nodeMeta )
        }
      }
    }
  }

}
