package com.dxm.auto.rxtension.processor

import com.dxm.auto.rxtension.Processor
import com.dxm.auto.rxtension.RXtension
import com.dxm.auto.rxtension.RXtensionClass
import com.dxm.auto.rxtension.internal.*
import com.dxm.auto.rxtension.processor.RXtensionType.Action
import com.dxm.auto.rxtension.processor.RXtensionType.Func
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeSpec.classBuilder
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind.VOID
import javax.lang.model.util.Types
import javax.tools.Diagnostic

/**
 * Created by Phoebe on 9/11/16.
 */
class RXtensionProcessor : Processor {
  override fun process(builders: MutableMap<UniqueType, JavaFileHolder>, context: Context) =
      context.roundEnv.getElementsAnnotatedWith(RXtension::class.java)
          .forEach { element ->
            (element as? ExecutableElement)?.target?.build(builders, context) ?:
                context.run { processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Cannot create extension for $element") }
          }
}

sealed class RXtensionContainer {
  class AnnotatedType(val annotation: RXtensionClass, val type: TypeElement) : RXtensionContainer()
  class Type(val type: TypeElement) : RXtensionContainer()

  val typeElement: TypeElement
    get() = when (this) {
      is AnnotatedType -> type
      is Type -> type
    }
  val packageName = typeElement.packageElement.simpleName.toString()
  fun uniqueType(types: Types) = UniqueType(typeElement.asType(), types)
  val containerClassName: String
    get() = when (this) {
      is AnnotatedType -> annotation.name
      is Type -> type.simpleName.toString() + "_Extensions"
    }

  companion object {
    val create: (typeElement: TypeElement) -> RXtensionContainer = { typeElement -> typeElement.getAnnotation(RXtensionClass::class.java)?.let { AnnotatedType(it, typeElement) } ?: Type(typeElement) }
  }

  val emptyJavaFileHolder: () -> JavaFileHolder = { JavaFileHolder(packageName, classBuilder(containerClassName)) }
}

private val ExecutableElement.container: RXtensionContainer?
  get() = RXtensionContainer.create mapOver topLevelTypeElement

class RXtensionTarget(val element: ExecutableElement, val annotation: RXtension, val container: RXtensionContainer)

private val ExecutableElement.target: RXtensionTarget?
  get() = container?.let { RXtensionTarget(this, this.getAnnotation(RXtension::class.java), it) }

private val RXtensionTarget.bindingClassName: String
  get() = annotation.name.isNotBlankOr { element.simpleName.toString() + "Binding" }
private val RXtensionTarget.bindingMethodName: String
  get() = annotation.name.isNotBlankOr { element.simpleName.toString() }

enum class RXtensionType {
  Func, Action
}

private fun RXtensionType.builder(target: RXtensionTarget) =
    when (this) {
      Func -> RXtensionFuncBuilder(target)
      Action -> RXtensionActionBuilder(target)
    }

private val RXtensionTarget.type: RXtensionType
  get() = if (element.returnsKind(VOID)) Action else Func

private fun RXtensionTarget.build(builders: MutableMap<UniqueType, JavaFileHolder>, context: Context) = type.builder(this).build(builders, context)

abstract class RXtensionBuilder(val target: RXtensionTarget) {
  fun build(builders: MutableMap<UniqueType, JavaFileHolder>, context: Context) {
    val uniqueType = target.container.uniqueType(context.processingEnv.typeUtils)
    val holder = builders.getOrPut(uniqueType, target.container.emptyJavaFileHolder)
    val type = typeSpec(target.bindingClassName.uniqueIn(holder.typeNames), context)
    val method = methodSpec(target.bindingMethodName.uniqueIn(holder.typeNames), type, context)
    holder.add(method, type)
  }

  abstract fun methodSpec(methodName: String, type: TypeSpec, context: Context): MethodSpec
  abstract fun typeSpec(typeName: String, context: Context): TypeSpec
}

class RXtensionFuncBuilder(target: RXtensionTarget) : RXtensionBuilder(target) {
  override fun methodSpec(methodName: String, type: TypeSpec, context: Context): MethodSpec {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun typeSpec(typeName: String, context: Context): TypeSpec {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}

class RXtensionActionBuilder(target: RXtensionTarget) : RXtensionBuilder(target) {
  override fun methodSpec(methodName: String, type: TypeSpec, context: Context): MethodSpec {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun typeSpec(typeName: String, context: Context): TypeSpec {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}

private sealed class ReceiverType {
  class Type(val type: TypeElement) : ReceiverType()
  class Instance(val type: TypeElement) : ReceiverType()
  class Parameter(val parameter: VariableElement) : ReceiverType()
}

private class Fields()

