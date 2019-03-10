package com.carrotgarden.sjs.junit

// https://github.com/gtache/scalajs-gradle

import java.io.File
import java.lang.annotation.Annotation
import java.net.{ URL, URLClassLoader }
import java.nio.file.Paths
import sbt.testing._
import scala.collection.mutable.ArrayBuffer
import java.lang.annotation.Annotation
import scala.reflect.runtime.universe._

/**
 * An object used to retrieve TaskDef for ScalaJS
 */
object ZombieScanner {

  /**
   * Finds all classes contained in an URLClassLoader
   * which match to a fingerprint,
   * or only those specified in explicitelySpecified
   * minus the ones in excluded
   *
   * @param classL              The URLClassLoader
   * @param fingerprints        The fingerprints to
   * @param explicitlySpecified A set of String to use as regex
   * @param excluded            A set of String to use as regex
   * @return The TaskDefs found by the scan
   */
  def scan(
    loader :              URLClassLoader,
    fingerprintList :     Array[ Fingerprint ],
    explicitlySpecified : Set[ String ]        = Set.empty,
    excluded :            Set[ String ]        = Set.empty
  ) : Array[ TaskDef ] = {

    def checkSuperclasses( klaz : Class[ _ ], fingerprint : SubclassFingerprint ) : Boolean = {

      def checkName( klaz : Class[ _ ], name : String ) : Boolean = {
        klaz.getName == name ||
          ( klaz.getCanonicalName != null && klaz.getSimpleName == name ) ||
          klaz.getCanonicalName == name
      }

      def checkRec( klaz : Class[ _ ], name : String ) : Boolean = {
        if ( checkName( klaz, name ) ) {
          true
        } else {
          var parent = klaz.getSuperclass
          while ( parent != null ) {
            if ( checkRec( parent, name ) ) {
              return true
            } else {
              parent = parent.getSuperclass
            }
          }
          klaz.getInterfaces.exists( interf => checkRec( interf, name ) )
        }
      }

      checkRec( klaz, fingerprint.superclassName() )
    }

    val objSuffix = "$"

    val classes = parseClasses( loader, explicitlySpecified, excluded )

    val buffer = ArrayBuffer[ TaskDef ]()

    classes.foreach( klaz => {

      fingerprintList.foreach {

        case annontated : AnnotatedFingerprint =>
          try {
            val mirror = runtimeMirror( loader )
            val symbol = mirror.classSymbol( klaz )
            val annoList = symbol.annotations
            if ( ( klaz.isAnnotationPresent( Class.forName( annontated.annotationName(), false, loader ).asInstanceOf[ Class[ _ <: Annotation ] ] )
              || annoList.exists( a => a.tree.tpe.toString == annontated.annotationName() ) )
              && ( annontated.isModule || ( !annontated.isModule && !klaz.getName.endsWith( objSuffix ) ) ) ) {
              buffer += new TaskDef( klaz.getName.stripSuffix( objSuffix ), annontated, explicitlySpecified.nonEmpty, Array( new SuiteSelector ) )
            }
          } catch {
            case e : ClassNotFoundException =>
              Console.err.println( "Class not found for annotation : " + annontated.annotationName() )
          }
        case parented : SubclassFingerprint =>
          if ( checkSuperclasses( klaz, parented ) ) {
            if ( !parented.requireNoArgConstructor || klaz.isInterface || ( parented.requireNoArgConstructor && checkZeroArgsConstructor( klaz ) )
              && ( parented.isModule || ( !parented.isModule && !klaz.getName.endsWith( objSuffix ) ) ) ) {
              buffer += new TaskDef( klaz.getName.stripSuffix( objSuffix ), parented, explicitlySpecified.nonEmpty, Array( new SuiteSelector ) )
            }
          }
        case _ => throw new IllegalArgumentException( "Unsupported Fingerprint type" )
      }
    } )

    buffer.toArray.distinct
  }

  /**
   * Checks if the given class has a constructor with zero arguments
   *
   * @param c The class
   * @return true or false
   */
  def checkZeroArgsConstructor( c : Class[ _ ] ) : Boolean = {
    c.getDeclaredConstructors.exists( cons => cons.getParameterCount == 0 )
  }

  /**
   * Finds all classes in a URLClassLoader, or only those specified by explicitelySpecified
   * minus the ones in excluded
   *
   * @param classL               The URLClassLoader
   * @param explicitelySpecified A set of String to use as regex
   * @param excluded             A set of String to use as regex
   * @return the classes
   */
  def parseClasses(
    loader :               URLClassLoader,
    explicitelySpecified : Set[ String ]  = Set.empty,
    excluded :             Set[ String ]  = Set.empty
  ) : Array[ Class[ _ ] ] = {

    val URIPathSep = '/'
    val extSep = '.'
    val ext = extSep + "class"

    def checkSpecific( name : String ) : Boolean = {
      !excluded.exists( s => s.r.pattern.matcher( name ).matches() ) &&
        ( explicitelySpecified.isEmpty || explicitelySpecified.exists( s => s.r.pattern.matcher( name ).matches() ) )
    }

    def checkAndAddFile( file : File, buffer : ArrayBuffer[ Class[ _ ] ], meth : () => Unit, packageName : String = "" ) : Unit = {
      if ( !file.isDirectory && file.getName.endsWith( ext ) ) {
        val fileName = file.getName
        val name = packageName + fileName.substring( 0, fileName.indexOf( extSep ) )
        if ( checkSpecific( name ) ) {
          buffer += loader.loadClass( name )
        }
      } else if ( file.isDirectory ) {
        meth()
      }
    }

    def parseClasses( url : URL, idx : Int, explicitlySpecified : Set[ String ] = Set.empty, excluded : Set[ String ] = Set.empty ) : Array[ Class[ _ ] ] = {
      val f = Paths.get( url.toURI ).toFile
      val packageName = {
        if ( url != loader.getURLs()( idx ) ) {
          loader.getURLs()( idx ).toURI.relativize( url.toURI ).toString.replace( URIPathSep, extSep )
        } else {
          ""
        }
      }
      if ( f.isDirectory ) {
        val buffer = ArrayBuffer.empty[ Class[ _ ] ]
        f.listFiles().foreach( file => {
          checkAndAddFile( file, buffer, () => parseClasses( file.toURI.toURL, idx, explicitlySpecified, excluded ).foreach( c => {
            buffer += c
          } ), packageName )
        } )
        buffer.toArray
      } else {
        if ( f.getName.endsWith( ext ) ) {
          val fileName = f.getName
          val name = fileName.substring( 0, fileName.indexOf( extSep ) )
          if ( checkSpecific( name ) ) {
            Array( loader.loadClass( packageName + name.substring( 0, name.indexOf( extSep ) ) ) )
          } else {
            Array.empty[ Class[ _ ] ]
          }
        } else {
          Array.empty[ Class[ _ ] ]
        }
      }
    }

    val buffer = ArrayBuffer.empty[ Class[ _ ] ]

    loader.getURLs.zipWithIndex.foreach( url => {
      val file = Paths.get( url._1.toURI ).toFile
      checkAndAddFile(
        file,
        buffer,
        () => parseClasses( url._1, url._2, explicitelySpecified, excluded ).foreach( klaz => {
          buffer += klaz
        } )
      )
    } )

    buffer.toArray

  }

}
