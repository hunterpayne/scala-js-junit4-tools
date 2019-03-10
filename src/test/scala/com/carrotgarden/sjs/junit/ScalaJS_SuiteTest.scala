
package com.carrotgarden.sjs.junit

import org.junit.Assert.assertTrue
import org.junit.runner.{ RunWith, JUnitCore }
import org.junit.runners.Suite

import ScalaJS_SuiteTest._
import org.junit.BeforeClass

@RunWith( classOf[ Suite ] )
@Suite.SuiteClasses( Array(
  classOf[ Suite01 ], classOf[ Suite02 ], classOf[ Suite03 ], classOf[ Suite10 ]
) )
class ScalaJS_SuiteTest

object ScalaJS_SuiteTest {

  @BeforeClass
  def setup: Unit = TestInit.setup(SuiteSetup)

  @RunWith( classOf[ ScalaJS_Suite ] )
  @Suite.SuiteClasses( Array(
    classOf[ test.Test01 ]
  ) )
  class Suite01

  @RunWith( classOf[ ScalaJS_Suite ] )
  @Suite.SuiteClasses( Array(
    classOf[ test.Test02 ]
  ) )
  class Suite02

  @RunWith( classOf[ ScalaJS_Suite ] )
  @Suite.SuiteClasses( Array(
    classOf[ test.Test03 ]
  ) )
  class Suite03

  @RunWith( classOf[ ScalaJS_Suite ] )
  @Suite.SuiteClasses( Array(
    classOf[ test.Test04 ],
    classOf[ test.Test05 ],
    classOf[ test.Test06 ]
  ) )
  class Suite10

}
