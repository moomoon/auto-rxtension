package com.dxm.auto.rxtension.internal

import javax.lang.model.element.*
import javax.lang.model.element.ElementKind.*
import javax.lang.model.type.TypeKind

/**
 * Created by ants on 9/12/16.
 */

val Element.enclosingTypeElement: TypeElement?
  get() = enclosingElement?.run {
    when (kind) {
      CLASS, INTERFACE, ENUM, ANNOTATION_TYPE -> this as TypeElement
      else -> enclosingTypeElement
    }
  }

val Element.topLevelTypeElement: TypeElement?
  get() = enclosingElement?.run {
    when (kind) {
      PACKAGE -> this@topLevelTypeElement as? TypeElement
      else -> topLevelTypeElement
    }
  }

val Element.packageElement: PackageElement
  get() = when (kind) {
    PACKAGE -> this as PackageElement
    else -> enclosingElement.packageElement
  }

val ExecutableElement.returnsKind: (TypeKind) -> Boolean
    get() = { returnType.kind.equals(it) }

enum class Scope {
  Public, Protected, Private, Package
}

private fun Element.contains(modifier: Modifier) = modifiers.contains(modifier)
private val Element.public: Boolean
  get() = contains(Modifier.PUBLIC)
private val Element.protected: Boolean
  get() = contains(Modifier.PROTECTED)
private val Element.private: Boolean
  get() = contains(Modifier.PRIVATE)
val Element.scope: Scope
  get() = if (public) Scope.Public else if (protected) Scope.Protected else if (private) Scope.Private else Scope.Package
val TypeElement.abstract: Boolean
  get() = contains(Modifier.ABSTRACT)
val ExecutableElement.abstract: Boolean
  get() = contains(Modifier.ABSTRACT)
val Element.static: Boolean
  get() = contains(Modifier.STATIC)
val Element.final: Boolean
  get() = contains(Modifier.FINAL)
val VariableElement.transient: Boolean
  get() = contains(Modifier.TRANSIENT)
val VariableElement.volatile: Boolean
  get() = contains(Modifier.VOLATILE)
val ExecutableElement.native: Boolean
  get() = contains(Modifier.NATIVE)