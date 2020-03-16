package org.jetbrains.plugins.scala.externalHighlighters

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.scala.tasty.TastyProvider
import org.jetbrains.plugins.scala.util.CompilationId

/**
 * Project state generated by compiler.
 * Using for storing highlightings and caching tasty files.
 */
object CompilerGeneratedStateManager {

  case class FileCompilerGeneratedState(compilationId: CompilationId,
                                        highlightings: Set[ExternalHighlighting],
                                        tastyProvider: TastyProvider)

  type CompilerGeneratedState = Map[VirtualFile, FileCompilerGeneratedState]
  type HighlightingState = Map[VirtualFile, Set[ExternalHighlighting]]

  def get(project: Project): CompilerGeneratedState =
    mutableState(project).state

  def update(project: Project, newState: CompilerGeneratedState): CompilerGeneratedState = {
    mutableState(project).state = newState
    val highlightingState = newState.toHighlightingState
    ExternalHighlighters.updateOpenEditors(project, highlightingState)
    ExternalHighlighters.informWolf(project, highlightingState)
    newState
  }

  private def mutableState(project: Project): MutableState =
    ServiceManager.getService(project, classOf[MutableState])

  private class MutableState {
    var state: CompilerGeneratedState = Map.empty
  }

  implicit class CompilerGeneratedStateExt(val state: CompilerGeneratedState)
    extends AnyVal {

    def toHighlightingState: HighlightingState =
      state.mapValues(_.highlightings)
  }
}
