package org.jetbrains.plugins.scala.extensions

import com.intellij.psi.PsiElement

object ElementText {
  def unapply(e: PsiElement): Some[String] = Some(e.getText)
}