
package com.carrotgarden.sjs.junit

import java.io.File

import org.scalajs.core.tools.linker.StandardLinker
import org.scalajs.core.tools.linker.ModuleKind
import org.scalajs.core.tools.logging.ScalaConsoleLogger
import org.scalajs.core.tools.io.WritableFileVirtualJSFile
import org.scalajs.core.tools.io.IRFileCache

trait LinkerImpl {

  lazy val fileCache = new IRFileCache().newCache

  def link(classpath: Seq[File], runtime: File) : Unit = {

    val collected = IRFileCache.IRContainer.fromClasspath(classpath)

    val extracted = fileCache.cached(collected)

    val config = StandardLinker.Config()
    // .withOptimizer(false)
      // .withSemantics(Semantics.Defaults)
      .withPrettyPrint(true)
      .withModuleKind(ModuleKind.NoModule)

    val linker = StandardLinker(config)
    val logger = new ScalaConsoleLogger()
    val result = WritableFileVirtualJSFile(runtime)

    linker.link(extracted, Seq(), result, logger)
  }
}
