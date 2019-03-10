package com.carrotgarden.sjs.junit

import scala.collection.JavaConverters.asScalaBufferConverter

import org.junit.runner.{ Description, Runner }
import org.junit.runner.notification.{ Failure, RunNotifier }
import org.junit.runners.Suite
import org.junit.runners.model.{ RunnerBuilder, InitializationError }

import sbt.testing.{ SuiteSelector, TaskDef }

/**
 * JUnit 4 suite runner for Scala.js.
 *
 * Directs invocations of JUnit 4 tests inside JS-VM.
 *
 * This runner requires a provisioning configuration
 * which describes how to start JS-VM testing environment.
 *
 * Default provisioning configuration location:
 * ${project.basedir}/target/scala-js-junit-tools/default.data
 *
 * Normally, provisioning configuration is prepared by an external plugin
 * before running a test framework which controls JUnit platform launcher.
 *
 * For example:
 * - scalor-maven-plugin:scala-js-env-prov-nodejs prepares configuration,
 * - maven-surefire-plugin:test controls JUnit platform launcher invocation.
 */
class ScalaJS_Suite( klaz : Class[ _ ], runners: java.util.List[Runner] )
  extends Suite( klaz, runners ) with Reference {

  def this( klaz : Class[ _ ], builder : RunnerBuilder ) =
    this(klaz, builder.runners(klaz, ScalaJS_Suite.getAnnotatedClasses(klaz)))

  import ScalaJS_Suite._

  lazy val framework = Context.defaultFramework

  lazy val workerRunner = Context.defaultRunner

  // FIXME JUnit specific
  lazy val marker = framework.fingerprints()( 0 )

  lazy val invokerLogger = new InvokerLogger()

  /**
   * Invoke worker class testing execution inside JS-VM.
   */
  override def runChild( runner : Runner, notifier : RunNotifier ) {
    import referenceEvent._

    // Trigger JS-VM launch on first run.
    val args = workerRunner.args

    val rootMeta = runner.getDescription()
    println("root desc " + rootMeta)
    assert(rootMeta.getTestClass != null)
    val testKlaz = rootMeta.getTestClass.getName

    val handler = new Notificator( rootMeta, notifier )

    try {

      // TODO support nested suites.
      val taskDef = new TaskDef(
        testKlaz, marker, true, Array( new SuiteSelector() )
      )
      val taskDefs = Array[ TaskDef ]( taskDef )
      val taskList = workerRunner.tasks( taskDefs )
      require( taskList.length == 1 )

      if ( fireTest.fromRuns ) {
        if ( printDebug ) println( s"FIRE test started ${rootMeta}" )
        notifier.fireTestStarted( rootMeta )
      }

      val loggerArray : Array[ sbt.testing.Logger ] =
        if ( fireMethod.fromLogs ) {
          Array( invokerLogger, handler.logger )
        } else {
          Array( invokerLogger )
        }

      taskList.foreach { task =>

        if ( fireMethod.fromRuns ) {
          fireMethodStarted( rootMeta, notifier )
        }

        val result = task.execute( handler, loggerArray )
        require( result.length == 0 )

        if ( fireMethod.fromRuns ) {
          fireMethodFinished( rootMeta, notifier )
        }
      }

    } catch {
      case error : Throwable =>
        notifier.fireTestFailure( new Failure( rootMeta, error ) )
    } finally {
      if ( fireTest.fromRuns ) {
        if ( printDebug ) println( s"FIRE test finished ${rootMeta}" )
        notifier.fireTestFinished( rootMeta )
      }
    }
  }

}

object ScalaJS_Suite extends Reference {

  lazy val referenceEvent = referenceJUnitEvent

  import referenceEvent._

  protected[junit] def getAnnotatedClasses(klass: Class[_]): Array[Class[_]] = {
    val annotation: Suite.SuiteClasses = 
      klass.getAnnotation(classOf[Suite.SuiteClasses])
    if (null == annotation) {
      throw new InitializationError(
        "class '%s' must have a SuiteClasses annotation".format(klass.getName()))
    }
    annotation.value()
  }

  /**
   * Hack around Scala.js JUnit provider not reporting per-method events.
   */
  def fireMethodStarted( rootMeta : Description, notifier : RunNotifier ) : Unit = {
    rootMeta.getChildren.asScala.foreach( nodeMeta => {
      if ( printDebug ) println( s"FIRE method started ${nodeMeta}" )
      notifier.fireTestStarted( nodeMeta )
      fireMethodStarted( nodeMeta, notifier )
    } )
  }

  /**
   * Hack around Scala.js JUnit provider not reporting per-method events.
   */
  def fireMethodFinished( rootMeta : Description, notifier : RunNotifier ) : Unit = {
    rootMeta.getChildren.asScala.foreach( nodeMeta => {
      if ( printDebug ) println( s"FIRE method finished ${nodeMeta}" )
      notifier.fireTestFinished( nodeMeta )
      fireMethodFinished( nodeMeta, notifier )
    } )
  }

}
