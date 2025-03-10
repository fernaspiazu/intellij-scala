package org.jetbrains.plugins.scala.annotator.quickfix

import com.intellij.codeInsight._
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiDocumentManager, PsiElement, PsiFile}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.lexer.ScalaModifier
import org.jetbrains.plugins.scala.lang.psi.api.base.ScModifierList
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScModifierListOwner
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

import java.util.Collections

sealed abstract class ModifierQuickFix(listOwner: ScModifierListOwner)
                                      (@Nls modifierToText: String => String)
                                      (modifier: ScalaModifier, value: Boolean = false)
  extends intention.IntentionAction {

  override final def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean =
    listOwner.isValid &&
      listOwner.getContainingFile == file

  override final def invoke(project: Project, editor: Editor, file: PsiFile): Unit =
    listOwner.getModifierList match {
      case null => ModifierQuickFix.logger.error("Illegal modifier list in: " + listOwner)
      case modifierList => onModifierList(modifierList)(project, editor, file)
    }

  protected def onModifierList(modifierList: ScModifierList)
                              (implicit project: Project, editor: Editor, file: PsiFile): Unit = {
    modifierList.setModifierProperty(modifier.text, value)
  }

  override final def startInWriteAction = true

  override final val getText: String = modifierToText(modifier.text)

  override def getFamilyName: String = getText

  override def getFileModifierForPreview(target: PsiFile): FileModifier =
    withListOwner(PsiTreeUtil.findSameElementInCopy(listOwner, target))

  protected def withListOwner(newListOwner: ScModifierListOwner): ModifierQuickFix
}

object ModifierQuickFix {

  import ScalaModifier._

  private val logger = Logger.getInstance(getClass)

  final class Remove(listOwner: ScModifierListOwner, nameId: PsiElement, modifier: ScalaModifier)
    extends ModifierQuickFix(listOwner)(
      if (nameId == null) ScalaBundle.message("remove.named.modifier.fix", _)
      else daemon.QuickFixBundle.message("remove.modifier.fix", nameId.getText, _)
    )(modifier) {

    override def onModifierList(modifierList: ScModifierList)
                               (implicit project: Project, editor: Editor, file: PsiFile): Unit = {
      super.onModifierList(modifierList)

      // Should be handled by auto formatting
      val textRange = modifierList.getTextRange
      val codeStyleManager = CodeStyleManager.getInstance(project)
      if (textRange.isEmpty)
        codeStyleManager.adjustLineIndent(file, textRange)
      else
        codeStyleManager.reformatText(file, Collections.singleton(textRange))
    }

    override protected def withListOwner(newListOwner: ScModifierListOwner): Remove =
      new Remove(newListOwner, nameId, modifier)
  }

  sealed class Add(listOwner: ScModifierListOwner, nameId: PsiElement, modifier: ScalaModifier)
    extends ModifierQuickFix(listOwner)(
      if (nameId == null) ScalaBundle.message("add.modifier.fix.without.name", _)
      else daemon.QuickFixBundle.message("add.modifier.fix", nameId.getText, _)
    )(modifier, value = true) {
    override protected def withListOwner(newListOwner: ScModifierListOwner): Add =
      new Add(newListOwner, nameId, modifier)
  }

  final class AddWithKeyword(listOwner: ScModifierListOwner, nameId: PsiElement, keywordType: IElementType)
    extends Add(listOwner, nameId, Override) {

    override def onModifierList(modifierList: ScModifierList)
                               (implicit project: Project, editor: Editor, file: PsiFile): Unit = {
      val keyword = ScalaPsiElementFactory.createDeclarationFromText(
        keywordType.toString + " x",
        listOwner.getParent,
        listOwner
      ).findFirstChildByType(keywordType).get
      listOwner.addAfter(keyword, modifierList)

      super.onModifierList(modifierList)
    }

    override protected def withListOwner(newListOwner: ScModifierListOwner): AddWithKeyword =
      new AddWithKeyword(newListOwner, nameId, keywordType)
  }

  sealed abstract class MakeNonPrivate(listOwner: ScModifierListOwner, @Nls modifierToText: String => String)
                                      (modifier: ScalaModifier, value: Boolean)
    extends ModifierQuickFix(listOwner)(modifierToText)(modifier, value) {

    override protected def onModifierList(modifierList: ScModifierList)
                                         (implicit project: Project, editor: Editor, file: PsiFile): Unit = {
      super.onModifierList(modifierList)
      PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument)
      CodeStyleManager.getInstance(project).adjustLineIndent(file, modifierList.getTextRange.getStartOffset)
    }

    override def getFamilyName: String = ScalaBundle.message("make.non.private.title")
  }

  final class MakePublic(listOwner: ScModifierListOwner)
    extends MakeNonPrivate(listOwner, ScalaBundle.message("make.public.fix", _))(Private, false) {
    override protected def withListOwner(newListOwner: ScModifierListOwner): MakePublic =
      new MakePublic(newListOwner)
  }

  final class MakeProtected(listOwner: ScModifierListOwner)
    extends MakeNonPrivate(listOwner, ScalaBundle.message("make.protected.fix", _))(Protected, value = true) {

    override protected def onModifierList(modifierList: ScModifierList)
                                         (implicit project: Project, editor: Editor, file: PsiFile): Unit = {
      modifierList.setModifierProperty(PRIVATE, false)
      super.onModifierList(modifierList)
    }

    override protected def withListOwner(newListOwner: ScModifierListOwner): MakeProtected =
      new MakeProtected(newListOwner)
  }
}
