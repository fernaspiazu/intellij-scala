package org.jetbrains.plugins.scala.lang.refactoring.rename.inplace

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi._
import com.intellij.refactoring.rename.inplace.{InplaceRefactoring, MemberInplaceRenameHandler, MemberInplaceRenamer}
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.light.{PsiClassWrapper, ScPrimaryConstructorWrapper}
import org.jetbrains.plugins.scala.statistics.ScalaRefactoringUsagesCollector

class ScalaMemberInplaceRenameHandler extends MemberInplaceRenameHandler with ScalaInplaceRenameHandler {

  override def isAvailable(element: PsiElement, editor: Editor, file: PsiFile): Boolean = {
    val processor = renameProcessor(element)
    editor.getSettings.isVariableInplaceRenameEnabled &&
      processor != null &&
      processor.canProcessElement(element) &&
      !isLocal(element)
  }

  override def invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext): Unit = {
    ScalaRefactoringUsagesCollector.logRenameMember(project)
    super.invoke(project, editor, file, dataContext)
  }

  protected override def createMemberRenamer(substituted: PsiElement,
                                             elementToRename: PsiNameIdentifierOwner,
                                             editor: Editor): MemberInplaceRenamer = {
    val (maybeFirstElement, maybeSecondElement) = substituted match {
      case _: PsiClass if elementToRename.is[PsiClassWrapper] =>
        (None, None)
      case wrapper: ScPrimaryConstructorWrapper =>
        val definition = wrapper.delegate.containingClass
        (Some(definition), definition.baseCompanion)
      case definition: ScTypeDefinition =>
        (Some(definition), definition.baseCompanion)
      case clazz: PsiClass =>
        (Some(clazz), None)
      case _: PsiNamedElement =>
        (None, None)
      case _ => throw new IllegalArgumentException("Substituted element for renaming has no name")
    }


    new ScalaMemberInplaceRenamer(maybeFirstElement.getOrElse(elementToRename),
      maybeSecondElement.getOrElse(substituted),
      editor)
  }

  override def doRename(elementToRename: PsiElement, editor: Editor, dataContext: DataContext): InplaceRefactoring = {
    afterElementSubstitution(elementToRename, editor) {
      super.doRename(_, editor, dataContext)
    }
  }
}
