package org.jetbrains.plugins.scala
package refactoring
package introduceVariable

import junit.framework.{Test, TestCase}
import org.jetbrains.plugins.scala.base.ScalaSdkOwner
import org.jetbrains.plugins.scala.base.libraryLoaders.{HeavyJDKLoader, LibraryLoader, ScalaSDKLoader}

class IntroduceVariableTest extends TestCase

object IntroduceVariableTest {
  private val DATA_PATH = "/refactoring/introduceVariable/data"

  def suite(): Test = new AbstractIntroduceVariableTestBase(IntroduceVariableTest.DATA_PATH) {
    override protected def needsSdk: Boolean = true
  }
}
