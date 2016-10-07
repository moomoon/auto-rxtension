package com.dxm.auto.rxtension.processor

import com.dxm.auto.rxtension.*
import com.dxm.auto.rxtension.internal.*
import com.google.common.base.CaseFormat.LOWER_CAMEL
import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeSpec.classBuilder
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind.VOID
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.LazyThreadSafetyMode.NONE

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

sealed class RXtensionContainer(val typeElement: TypeElement) {
  abstract val containerClassName: String
  val packageName = typeElement.packageElement.simpleName.toString()
  fun uniqueType(types: Types) = UniqueType(typeElement.asType(), types)
  fun emptyJavaFileHolder() = JavaFileHolder(packageName, classBuilder(containerClassName))

  class AnnotatedType(val annotation: RXtensionClass, type: TypeElement) : RXtensionContainer(type) {
    override val containerClassName: String = annotation.value
  }

  class Type(type: TypeElement) : RXtensionContainer(type) {
    override val containerClassName: String = type.simpleName.toString() + "_Extensions"
  }

  companion object {
    val create: (typeElement: TypeElement) -> RXtensionContainer = { typeElement -> typeElement.getAnnotation(RXtensionClass::class.java)?.let { AnnotatedType(it, typeElement) } ?: Type(typeElement) }
  }
}

private val ExecutableElement.container: RXtensionContainer?
  get() = RXtensionContainer.create mapOver topLevelTypeElement

class RXtensionTarget(val element: ExecutableElement, val annotation: RXtension, val container: RXtensionContainer)

private val ExecutableElement.target: RXtensionTarget?
  get() = container?.let { RXtensionTarget(this, this.getAnnotation(RXtension::class.java), it) }

private fun RXtensionTarget.build(builders: MutableMap<UniqueType, JavaFileHolder>, context: Context) = type.builder(this).build(builders, context)

//private fun RXtensionTarget.methodReceiverAsReceiver(context: Context) =
//    element.enclosingTypeElement?.let { type -> element.parseScope { receiver, _, _ -> ReceiverType.Type(type, receiver, context.processingEnv.typeUtils) } }
//
//private fun RXtensionTarget.methodReceiverAsPartial(context: Context) =
//    element.enclosingTypeElement?.let { type -> element.parseScope { _, partial, _ -> partial?.let { PartialType.ConstructorPartialType(type, it, context.processingEnv.typeUtils) } } }

//private fun RXtensionTarget.receivers(context: Context): List<ReceiverType> {
//  val parameterReceivers = element.parameters.mapNotNull { parameter ->
//    parameter.parseScope { receiver, _, _ ->
//      receiver?.let { ReceiverType.Parameter(parameter, it, context.processingEnv.typeUtils) }
//    }
//  }
//  return listOf(methodReceiverAsReceiver(context)).append(parameterReceivers).filterNotNull()
//}
//
//private fun RXtensionTarget.constructorParameters(context: Context): List<PartialType> {
//  val methodReceiver = methodReceiverAsReceiver(context)?.let(PartialType::ConstructorReceiverType) ?: methodReceiverAsPartial(context)
//  val parameters = element.parameters.mapNotNull { parameter ->
//    parameter.parseScope { receiver, partial, _ ->
//      receiver?.let { PartialType.ConstructorReceiverParameter(parameter, it, context.processingEnv.typeUtils) } ?:
//          partial?.let { PartialType.ConstructorPartialParameter(parameter, it, context.processingEnv.typeUtils) }
//    }
//  }
//  return listOf(methodReceiver).append(parameters).filterNotNull()
//}

//private fun RXtensionTarget.dynamicParameters(context: Context): List<DynamicType> {
//  val methodReceiver = element.enclosingTypeElement?.let { type -> element.parseScope { _, _, dynamic -> dynamic?.let { DynamicType.Type(type, it, context.processingEnv.typeUtils) } } }
//  val parameters = element.parameters.mapNotNull { parameter ->
//    parameter.parseScope { _, _, dynamic -> DynamicType.Parameter(parameter, dynamic, context.processingEnv.typeUtils) }
//  }
//  return listOf(methodReceiver).append(parameters).filterNotNull()
//}


abstract class RXtensionBuilder(val target: RXtensionTarget) {
  fun build(builders: MutableMap<UniqueType, JavaFileHolder>, context: Context) {
    val uniqueType = target.container.uniqueType(context.processingEnv.typeUtils)
    builders.getOrPut(uniqueType) { target.container.emptyJavaFileHolder() }.add(target, context)
  }

  abstract fun methodSpec(methodName: String, type: TypeSpec, context: Context): MethodSpec
  abstract fun typeSpec(typeName: String, context: Context): TypeSpec
}

interface RXtensionClassBuilder {
  fun build(target: RXtensionTarget, typeName: String, context: Context): TypeSpec
}

interface RXtensionMethodBuilder {
  fun build(target: RXtensionTarget, type: TypeSpec, methodName: String, context: Context): MethodSpec

  companion object : RXtensionMethodBuilder {
    override fun build(target: RXtensionTarget, type: TypeSpec, methodName: String, context: Context): MethodSpec {
      throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
  }
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
