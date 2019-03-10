package com.carrotgarden.sjs.junit

import java.io.File

/**
 * Classes support.
 */
object Classer {

  // FIXME make flexible
  def currentClassPath : Seq[ File ] = {

    //    val loader = getClass.getClassLoader.asInstanceOf[ URLClassLoader ]
    //    loader.getURLs.map( url => new File( url.toURI() ) ).toSeq

    val classpath = System.getProperty( "java.class.path" );
    val entryList = classpath.split( File.pathSeparator );
    entryList.map( entry => new File( entry ) )

  }

}
