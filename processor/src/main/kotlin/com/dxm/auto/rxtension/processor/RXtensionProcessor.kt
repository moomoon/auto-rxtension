package com.dxm.auto.rxtension.processor

import com.dxm.auto.rxtension.*
import com.dxm.auto.rxtension.internal.*
import com.dxm.auto.rxtension.processor.RXtensionType.Action
import com.dxm.auto.rxtension.processor.RXtensionType.Func
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

private val RXtensionTarget.bindingClassName: String
  get() = annotation.value.isNotBlankOr { element.simpleName.toString() }.let(LOWER_CAMEL to UPPER_CAMEL) + "Binding"
private val RXtensionTarget.bindingMethodName: String
  get() = annotation.value.isNotBlankOr { element.simpleName.toString() }

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

private inline fun <T, A : Element> A.parseAnnotation(creator: (Receiver?, Partial?, Dynamic?) -> T) =
    if ((this as? ExecutableElement)?.static.isTrue) {
      (getAnnotation(Receiver::class.java) ?:
          getAnnotation(Partial::class.java) ?:
          getAnnotation(Dynamic::class.java))?.let {
        throw IllegalArgumentException("Static function $this cannot be annotated with $it")
      }
    } else {
      val receiver = getAnnotation(Receiver::class.java)
      val partial = getAnnotation(Partial::class.java)
      val dynamic = getAnnotation(Dynamic::class.java)
      listOf(receiver, partial, dynamic).filterNotNull().let {
        when (it.count()) {
          0, 1 -> creator(receiver, partial, dynamic)
          else -> throw IllegalArgumentException("$this cannot be annotated with both $it at the same time.")
        }
      }
    }

//private fun RXtensionTarget.methodReceiverAsReceiver(context: Context) =
//    element.enclosingTypeElement?.let { type -> element.parseAnnotation { receiver, _, _ -> ReceiverType.Type(type, receiver, context.processingEnv.typeUtils) } }
//
//private fun RXtensionTarget.methodReceiverAsPartial(context: Context) =
//    element.enclosingTypeElement?.let { type -> element.parseAnnotation { _, partial, _ -> partial?.let { ConstructorParameter.ConstructorPartialType(type, it, context.processingEnv.typeUtils) } } }

//private fun RXtensionTarget.receivers(context: Context): List<ReceiverType> {
//  val parameterReceivers = element.parameters.mapNotNull { parameter ->
//    parameter.parseAnnotation { receiver, _, _ ->
//      receiver?.let { ReceiverType.Parameter(parameter, it, context.processingEnv.typeUtils) }
//    }
//  }
//  return listOf(methodReceiverAsReceiver(context)).append(parameterReceivers).filterNotNull()
//}
//
//private fun RXtensionTarget.constructorParameters(context: Context): List<ConstructorParameter> {
//  val methodReceiver = methodReceiverAsReceiver(context)?.let(ConstructorParameter::ConstructorReceiverType) ?: methodReceiverAsPartial(context)
//  val parameters = element.parameters.mapNotNull { parameter ->
//    parameter.parseAnnotation { receiver, partial, _ ->
//      receiver?.let { ConstructorParameter.ConstructorReceiverParameter(parameter, it, context.processingEnv.typeUtils) } ?:
//          partial?.let { ConstructorParameter.ConstructorPartialParameter(parameter, it, context.processingEnv.typeUtils) }
//    }
//  }
//  return listOf(methodReceiver).append(parameters).filterNotNull()
//}

//private fun RXtensionTarget.dynamicParameters(context: Context): List<DynamicParameter> {
//  val methodReceiver = element.enclosingTypeElement?.let { type -> element.parseAnnotation { _, _, dynamic -> dynamic?.let { DynamicParameter.Type(type, it, context.processingEnv.typeUtils) } } }
//  val parameters = element.parameters.mapNotNull { parameter ->
//    parameter.parseAnnotation { _, _, dynamic -> DynamicParameter.Parameter(parameter, dynamic, context.processingEnv.typeUtils) }
//  }
//  return listOf(methodReceiver).append(parameters).filterNotNull()
//}

private fun RXtensionTarget.parameters(context: Context): Triple<List<ReceiverType>, List<ConstructorParameter>, List<DynamicParameter>> {
  val types = context.processingEnv.typeUtils
  val enclosingType = element.enclosingTypeElement ?: throw RuntimeException("Cannot find enclosing type of $element.")
  val receiverTriple = element.parseAnnotation { receiver, partial, dynamic ->
    Triple(
        (partial.isNull && dynamic.isNull) whenTrue { ReceiverType.Type(enclosingType, receiver, types) },
        partial?.let { ConstructorParameter.Type(enclosingType, it, types) },
        dynamic?.let { DynamicParameter.Type(enclosingType, it, types) })
  }
  val parameterTriple = element.parameters.map { parameter ->
    parameter.parseAnnotation { receiver, partial, dynamic ->
      Triple(receiver?.let { ReceiverType.Parameter(parameter, it, types) },
          partial?.let { ConstructorParameter.Parameter(parameter, it, types) },
          (receiver.isNull && partial.isNull) whenTrue { DynamicParameter.Parameter(parameter, dynamic, types) })
    } ?: throw RuntimeException("Expecting VariableElement, got $parameter.")
  }
  val (receiverAsReceiver, receiverAsConstructor, receiverAsDynamic) = receiverTriple.nullableElements()
  val (parameterReceiver, parameterConstructor, parameterDynamic) = parameterTriple.rotate()
  return Triple(listOf(receiverAsReceiver).append(parameterReceiver).filterNotNull(),
      listOf(receiverAsConstructor).append(parameterConstructor).filterNotNull(),
      listOf(receiverAsDynamic).append(parameterDynamic).filterNotNull())
}

abstract class RXtensionBuilder(val target: RXtensionTarget) {
  fun build(builders: MutableMap<UniqueType, JavaFileHolder>, context: Context) {
    val uniqueType = target.container.uniqueType(context.processingEnv.typeUtils)
    val (receivers, constructorParameters, dynamicParameters) = target.parameters(context)
    builders.getOrPut(uniqueType) { target.container.emptyJavaFileHolder() }
        .add(receivers, constructorParameters, dynamicParameters, target.element.returnType)
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

sealed class ReceiverType(val type: UniqueType, private val element: Element) {
  protected abstract val overrideName: String?
  val name: String by lazy(NONE) { overrideName ?: element.receiverName }

  constructor(element: Element, types: Types) : this(UniqueType(element.asType(), types), element)

  class Type(type: TypeElement, val annotation: Receiver?, types: Types) : ReceiverType(type, types) {
    override val overrideName = annotation?.value?.unless(String::isBlank)
  }

  class Parameter(parameter: VariableElement, val annotation: Receiver, types: Types) : ReceiverType(parameter, types) {
    override val overrideName = annotation.value unless String::isBlank
  }

  fun equalsReceiver(other: ReceiverType) = other.type.equals(type) && other.name.equals(name)
  val Element.receiverName: String
    get() = simpleName.toString().let(UPPER_CAMEL to LOWER_CAMEL)
  val debugString: String
    get() = overrideName?.let { "Receiver($it) ${type.typeMirror}" } ?: "${type.typeMirror}"
}

sealed class ConstructorParameter(val uniqueType: UniqueType, val annotation: Partial, private val element: Element) {
  constructor(element: Element, types: Types, annotation: Partial) : this(UniqueType(element.asType(), types), annotation, element)

  val name = annotation.value.isNotBlankOr { element.constructorParameterName }

  class Type(val type: TypeElement, annotation: Partial, types: Types) : ConstructorParameter(type, types, annotation)
  class Parameter(val parameter: VariableElement, annotation: Partial, types: Types) : ConstructorParameter(parameter, types, annotation)

  fun equalsConstructorParameter(other: ConstructorParameter) = other.uniqueType.equals(uniqueType) && other.name.equals(name)
  val Element.constructorParameterName: String
    get() = simpleName.toString().let(UPPER_CAMEL to LOWER_CAMEL)
}

sealed class DynamicParameter(val uniqueType: UniqueType, private val element: Element) {
  protected abstract val overrideName: String?
  val name: String by lazy(NONE) { overrideName ?: element.dynamicParameterName }

  constructor(element: Element, types: Types) : this(UniqueType(element.asType(), types), element)

  class Type(val type: TypeElement, val annotation: Dynamic, types: Types) : DynamicParameter(type, types) {
    override val overrideName = annotation.value unless String::isEmpty
  }

  class Parameter(val parameter: VariableElement, val annotation: Dynamic?, types: Types) : DynamicParameter(parameter, types) {
    override val overrideName = annotation?.value?.unless(String::isEmpty)
  }

  fun equalsDynamicParameter(other: DynamicParameter) = other.uniqueType.equals(uniqueType) && other.name.equals(name)
  val Element.dynamicParameterName: String
    get() = simpleName.toString().let(UPPER_CAMEL to LOWER_CAMEL)

}

