package com.dxm.auto.rxtension.internal

import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.*
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
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