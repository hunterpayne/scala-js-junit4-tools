package com.carrotgarden.sjs.junit

import org.scalajs.core.tools.io.VirtualJSFile
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.jsenv.AsyncJSRunner
import org.scalajs.jsenv.ComJSEnv
import org.scalajs.jsenv.ComJSRunner
import org.scalajs.jsenv.ExternalJSEnv
import org.scalajs.jsenv.JSRunner
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.jsenv.phantomjs.PhantomJSEnv

/**
 * Scala.js JavaScript VM environment definitions.
 *
 * Customize environments provided by org.scalajs.jsenv:
 * - provide clean environment variables
 * - provide comprehensive runner logging
 */
object JSEnv {

  import Config.EnvConf

  trait Type {
    val name : String
    def apply( config : EnvConf ) : ComJSEnv with ExternalJSEnv
    override def toString : String = name
  }

  object Type {

    val nameList = List(
      NodejsBasic.name,
      NodejsJsdom.name,
      PhandomjsBasic.name
    )

    def apply( name : String ) : Type = {
      name match {
        case NodejsBasic.name    => NodejsBasic
        case NodejsJsdom.name    => NodejsJsdom
        case PhandomjsBasic.name => PhandomjsBasic
        case _ =>
          val message = s"Wrong JS-VM: ${name}; use: ${nameList.mkString( ", " )}"
          throw new RuntimeException( message )
      }
    }

    case object NodejsBasic extends Type {
      override val name = "nodejs-basic"
      override def apply( envConf : EnvConf ) = {
        import envConf._
        val setup = NodeJSEnv.Config()
          .withExecutable( envExec )
          .withArgs( envArgs )
          .withEnv( envVars )
          .withSourceMap( true )
        new JSEnv.NodeBasic( setup )
      }
    }

    case object NodejsJsdom extends Type {
      override val name = "nodejs-jsdom"
      override def apply( envConf : EnvConf ) = {
        import envConf._
        val setup = JSDOMNodeJSEnv.Config()
          .withExecutable( envExec )
          .withArgs( envArgs )
          .withEnv( envVars )
        new JSEnv.NodeJsdom( setup )
      }
    }

    case object PhandomjsBasic extends Type {
      override val name = "phantomjs-basic"
      override def apply( envConf : EnvConf ) = {
        import envConf._
        val setup = PhantomJSEnv.Config()
          .withExecutable( envExec )
          .withArgs( envArgs )
          .withEnv( envVars )
          .withAutoExit( true )
        new JSEnv.PhantomBasic( setup )
      }
    }

  }

  trait VmName {
    def vmName : String
  }

  /**
   *
   */
  class NodeBasic( config : NodeJSEnv.Config )
    extends NodeJSEnv( config ) with VmName {
    override def vmName : String = Type.NodejsBasic.name
    trait CustomRunner extends AbstractNodeRunner {
      // provide clean env var
      override protected def getVMEnv() : Map[ String, String ] = {
        env
      }
      // provide runner report
      override protected def startVM() : Process = {
        val envArgs = getVMArgs()
        val envVars = getVMEnv()
        val argsList = executable +: envArgs
        val builder = new ProcessBuilder( argsList : _* )
        builder.environment().clear()
        for ( ( name, value ) <- envVars ) builder.environment().put( name, value )
        logger.info( runnerReport(
          RunnerContext( runnerType, executable, envArgs, envVars, libs.map( _.lib.path ), code.path )
        ) )
        builder.start()
      }
    }
    override def jsRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile ) : JSRunner = {
      new NodeRunner( libs, code )
    }
    override def asyncRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile ) : AsyncJSRunner = {
      new AsyncNodeRunner( libs, code )
    }
    override def comRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile ) : ComJSRunner = {
      new ComNodeRunner( libs, code )
    }
    protected class NodeRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile )
      extends ExtRunner( libs, code ) with AbstractBasicNodeRunner with CustomRunner
    protected class AsyncNodeRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile )
      extends AsyncExtRunner( libs, code ) with AbstractBasicNodeRunner with CustomRunner
    protected class ComNodeRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile )
      extends AsyncNodeRunner( libs, code ) with NodeComJSRunner with CustomRunner
  }

  /**
   *
   */
  class NodeJsdom( config : JSDOMNodeJSEnv.Config )
    extends JSDOMNodeJSEnv( config ) with VmName {
    override def vmName : String = Type.NodejsJsdom.name
    trait CustomRunner extends AbstractNodeRunner {
      // provide clean env var
      override protected def getVMEnv() : Map[ String, String ] = {
        env
      }
      // provide runner report
      override protected def startVM() : Process = {
        val envArgs = getVMArgs()
        val envVars = getVMEnv()
        val argsList = executable +: envArgs
        val builder = new ProcessBuilder( argsList : _* )
        builder.environment().clear()
        for ( ( name, value ) <- envVars ) builder.environment().put( name, value )
        logger.info( runnerReport(
          RunnerContext( runnerType, executable, envArgs, envVars, libs.map( _.lib.path ), code.path )
        ) )
        builder.start()
      }
    }
    override def jsRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile ) : JSRunner = {
      new NodeRunner( libs, code )
    }
    override def asyncRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile ) : AsyncJSRunner = {
      new AsyncNodeRunner( libs, code )
    }
    override def comRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile ) : ComJSRunner = {
      new ComNodeRunner( libs, code )
    }
    protected class NodeRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile )
      extends ExtRunner( libs, code ) with AbstractDOMNodeRunner with CustomRunner
    protected class AsyncNodeRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile )
      extends AsyncExtRunner( libs, code ) with AbstractDOMNodeRunner with CustomRunner
    protected class ComNodeRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile )
      extends AsyncNodeRunner( libs, code ) with NodeComJSRunner
  }

  /**
   *
   */
  class PhantomBasic( config : PhantomJSEnv.Config )
    extends PhantomJSEnv( config ) with VmName {
    override def vmName : String = Type.PhandomjsBasic.name
    trait CustomRunner extends AbstractPhantomRunner {
      // provide clean env var
      override protected def getVMEnv() : Map[ String, String ] = {
        env
      }
      // provide runner report
      override protected def startVM() : Process = {
        val envArgs = getVMArgs()
        val envVars = getVMEnv()
        val argsList = executable +: envArgs
        val builder = new ProcessBuilder( argsList : _* )
        builder.environment().clear()
        for ( ( name, value ) <- envVars ) builder.environment().put( name, value )
        logger.info( runnerReport(
          RunnerContext( runnerType, executable, envArgs, envVars, libs.map( _.lib.path ), code.path )
        ) )
        builder.start()
      }
    }
    override def jsRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile ) : JSRunner = {
      new PhantomRunner( libs, code )
    }
    override def asyncRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile ) : AsyncJSRunner = {
      new AsyncPhantomRunner( libs, code )
    }
    override def comRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile ) : ComJSRunner = {
      new ComPhantomRunner( libs, code )
    }
    protected class PhantomRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile )
      extends ExtRunner( libs, code ) with AbstractPhantomRunner with CustomRunner
    protected class AsyncPhantomRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile )
      extends AsyncExtRunner( libs, code ) with AbstractPhantomRunner with CustomRunner
    protected class ComPhantomRunner( libs : Seq[ ResolvedJSDependency ], code : VirtualJSFile )
      extends super.ComPhantomRunner( libs, code ) // with FIXME
  }

  def hasStackMethod( method : String ) : Boolean = {
    val trace = Thread.currentThread().getStackTrace()
    trace.find( entry => method == entry.getMethodName ).isDefined
  }

  def hasBridge = hasStackMethod( "startManagedRunner" )
  def hasInformer = hasStackMethod( "fetchFrameworkInfo" )
  def hasMaster = hasStackMethod( "createRemoteRunner" )
  def hasWorker = hasStackMethod( "createSlave" )

  def runnerType = {
    if ( hasBridge ) "BRIDGE"
    else if ( hasInformer ) "INFORMER"
    else if ( hasMaster ) "MASTER"
    else if ( hasWorker ) "WORKER"
    else "UNKNOWN"
  }

  case class RunnerContext(
    mode : String,
    exec : String,
    args : Seq[ String ],
    vars : Map[ String, String ],
    libs : Seq[ String ],
    code : String
  )

  object RunnerContext {
    import upickle._
    import upickle.default._
    implicit def codecRunnerContext : ReadWriter[ RunnerContext ] = macroRW
    def contextParse( context : String ) : RunnerContext = read[ RunnerContext ]( context )
    def contextUnparse( context : RunnerContext ) : String = write( context, indent = 4 )
  }

  def runnerReport( context : RunnerContext ) : String = {
    RunnerContext.contextUnparse( context )
  }

}
