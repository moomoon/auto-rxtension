package com.dxm.auto.rxtension.internal

import com.dxm.auto.rxtension.Dynamic
import com.dxm.auto.rxtension.Partial
import com.dxm.auto.rxtension.Receiver
import com.dxm.auto.rxtension.internal.RXtensionType.Action
import com.dxm.auto.rxtension.internal.RXtensionType.Func
import com.dxm.auto.rxtension.processor.RXtensionTarget
import com.google.common.base.CaseFormat.LOWER_CAMEL
import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.squareup.javapoet.*
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Created by Phoebe on 9/10/16.
 */

class JavaFileHolder(val packageName: String, private val builder: TypeSpec.Builder) {
  private val mutableMap: BiMap<MethodSpec, TypeSpec> = HashBiMap.create()
  private val mutableNameMap: BiMap<String, String> = HashBiMap.create()
  private val receivers: LinkedHashMap<String, ReceiverType> = LinkedHashMap()
  private val nestedClassNames = mutableSetOf<String>()

  private fun add(method: MethodSpec, type: TypeSpec) {
    mutableMap.put(method, type)
    mutableNameMap.put(method.name, type.name)
  }

  fun add(receiver: ReceiverType) = receivers[receiver.name]?.let {
    if (!receiver.equalsReceiver(it)) throw IllegalArgumentException("${receiver.debugString} collides with ${it.debugString}")
  } ?: receivers.put(receiver.name, receiver)

  operator fun plusAssign(methodAndType: Pair<MethodSpec, TypeSpec>) = add(methodAndType.first, methodAndType.second)
  val methodsAndTypes: Map<MethodSpec, TypeSpec>
    get() = mutableMap
//  val typesAndMethods: Map<TypeSpec, MethodSpec>
//    get() = mutableMap.inverse()
//  val methods: Set<MethodSpec>
//    get() = methodsAndTypes.keys
//  val types: Set<TypeSpec>
//    get() = mutableMap.values
//  val methodNames: Set<String>
//    get() = mutableNameMap.keys
//  val typeNames: Set<String>
//    get() = mutableNameMap.values

  fun add(target: RXtensionTarget, context: Context) {
    val (receiverParameters, partialParameters, dynamicParameters) = target.parameters(context)
    context.processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "receivers $receiverParameters, partials $partialParameters, dynamics $dynamicParameters")
    val methodReceiver = target.methodReceiver(context)
    context.processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "methodReceiver $methodReceiver")
    val finalNames = mutableSetOf<String>()
    val dataNames = mutableSetOf<String>()
    receiverParameters.forEach {
      add(it)
      finalNames.add(it.name)
    }
    val uniqueMethodReceiver = when (methodReceiver) {
      is MethodReceiver.Dynamic.Receiver -> {
        add(methodReceiver.type)
        finalNames.add(methodReceiver.name)
        methodReceiver
      }
      is MethodReceiver.Dynamic.Partial -> MethodReceiver.Dynamic.Partial(methodReceiver.type.named(finalNames.unique(methodReceiver.type.name)))
      is MethodReceiver.Dynamic.Data -> {
        dataNames.add(methodReceiver.name)
        methodReceiver
      }
      else -> methodReceiver
    }
    val receiverFinalParameters = receiverParameters.map { NamedFinalParameter.Parameter.Member(it) }
    val externalFinalParameters = partialParameters.map { NamedFinalParameter.Parameter.External(it.named(finalNames.unique(it.name))) }
    val bindingClass = target.bindingClass(uniqueMethodReceiver, nestedClassNames.unique(target.bindingClassName), receiverFinalParameters + externalFinalParameters, dynamicParameters.map { it.named(dataNames.unique(it.name)) }, context)
    builder.addType(bindingClass)
  }

  fun build(): JavaFile {
    val fieldsAndParams = receivers.values.map {
      FieldSpec.builder(it.type.typeMirror.typeName, it.name, Modifier.PRIVATE, Modifier.FINAL).build() to
          ParameterSpec.builder(it.type.typeMirror.typeName, it.name).build()
    }
    val constructorBuilder = MethodSpec.constructorBuilder()
    fieldsAndParams.forEach {
      builder.addField(it.first)
      constructorBuilder.addParameter(it.second)
      constructorBuilder.addStatement("this.\$N = \$N", it.first, it.second)
    }

    builder.addMethod(constructorBuilder.build())
    return JavaFile.builder(packageName, builder.apply { methodsAndTypes.forEach(builder.addEntry) }.build()!!).build()!!
  }
}


private fun RXtensionTarget.bindingClass(methodReceiver: MethodReceiver, className: String, finalParameters: List<NamedFinalParameter.Parameter>, dataParameters: List<DataType.Parameter>, context: Context): TypeSpec {
  val builder = TypeSpec.classBuilder(className).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
  val fieldAndConstructorParam = (listOf(methodReceiver.finalParameter) + finalParameters).filterNotNull().map {
    FieldSpec.builder(it.typeName(), it.name, Modifier.FINAL, Modifier.PRIVATE).build() to
        ParameterSpec.builder(it.typeName(), it.name).build()
  }
  fieldAndConstructorParam.forEach { builder.addField(it.first) }
  val dataParams = (listOf(methodReceiver.dataType) + dataParameters).filterNotNull()
  val rxInterface = element.returnType.rxtensionType(dataParams).rxInterface(context)
  builder.addSuperinterface(rxInterface.typeName)
  fieldAndConstructorParam.isNotEmpty().whenTrue {
    builder.addMethod(fieldAndConstructorParam.constructor())
  }
  val methodParameters = (finalParameters.map { it.methodParameter } + dataParameters.map { it.methodParameter }).sorted()
  builder.addMethod(callMethod(element.returnType.asReturnType, methodReceiver, element.simpleName.toString(), methodParameters, dataParameters, rxInterface.eraseType))
  return builder.build()
}

private val NamedFinalParameter.Parameter.methodParameter: MethodParameter
  get() = when (this) {
    is NamedFinalParameter.Parameter.External -> MethodParameter.Partial(partialParameter)
    is NamedFinalParameter.Parameter.Member -> MethodParameter.Receiver(receiver)
  }

private val DataType.Parameter.methodParameter: MethodParameter
  get() = MethodParameter.Data(this)

sealed class MethodParameter(val index: Int, val name: String, val typeName: TypeName) : Comparable<MethodParameter> {
  class Receiver(val parameter: ReceiverType.Parameter) : MethodParameter(parameter.index, parameter.name, parameter.type.typeMirror.typeName)
  class Partial(val parameter: PartialType.Parameter) : MethodParameter(parameter.index, parameter.name, parameter.uniqueType.typeMirror.typeName)
  class Data(val parameter: DataType.Parameter) : MethodParameter(parameter.index, parameter.name, parameter.uniqueType.typeMirror.typeName)

  override fun compareTo(other: MethodParameter): Int = index - other.index
}

private fun List<Pair<FieldSpec, ParameterSpec>>.constructor(): MethodSpec {
  val builder = MethodSpec.constructorBuilder()
  forEach {
    builder.addParameter(it.second)
    builder.addStatement("this.\$N = \$N", it.first, it.second)
  }
  return builder.build()
}

private val TypeMirror.asReturnType: TypeName
  get() = when (kind) {
    TypeKind.VOID -> TypeName.VOID
    else -> TypeName.get(this).box()
  }

private val MethodReceiver.statementSegment: Statement.Builder.() -> Unit
  get() = when (this) {
    is MethodReceiver.Static -> {
      {
        format += "\$T."
        args += listOf(type)
      }
    }
    is MethodReceiver.Dynamic -> statementSegment
  }

private val MethodReceiver.Dynamic.statementSegment: Statement.Builder.() -> Unit
  get() = when (this) {
    is MethodReceiver.Dynamic.Receiver -> {
      {
        format += "this.${type.name}."
      }
    }
    is MethodReceiver.Dynamic.Partial -> {
      {
        format += "this.${type.name}."
      }
    }
    is MethodReceiver.Dynamic.Data -> {
      {
        format += "${type.name}."
      }
    }
  }


private fun callMethod(returnType: TypeName, methodReceiver: MethodReceiver, methodName: String, methodParameter: List<MethodParameter>, data: List<DataType.Parameter>, eraseType: Boolean): MethodSpec {
  val builder = MethodSpec.methodBuilder("call").addModifiers(Modifier.PUBLIC).returns(returnType).addAnnotation(Override::class.java)
  val statement = Statement.builder()
  if (returnType != TypeName.VOID)
    statement { format += "return " }
  statement(methodReceiver.statementSegment)
  statement {
    format += methodName
    format += "("
  }
  if (eraseType) {
    builder.addParameter(ParameterSpec.builder(ArrayTypeName.of(TypeName.OBJECT), "args").build()).varargs()
  } else {
    if (methodReceiver is MethodReceiver.Dynamic.Data) {
      builder.addParameter(ParameterSpec.builder(methodReceiver.typeMirror.typeName.box(), methodReceiver.name).build())
    }
    data.map { ParameterSpec.builder(it.typeName.box(), it.name).build() }.forEach { builder.addParameter(it) }
    methodParameter.mapIndexed { index, p ->
      statement {
        if (index >= 1) format += ", "
        format += if (p is MethodParameter.Data) p.name else "this.${p.name}"
      }
    }
  }
  statement { format += ")" }
  builder.addStatement(statement.build())
  return builder.build()
}


private val RXtensionTarget.bindingClassName: String
  get() = annotation.value.isNotBlankOr { element.simpleName.toString() }.let(LOWER_CAMEL to UPPER_CAMEL) + "Binding"
private val RXtensionTarget.bindingMethodName: String
  get() = annotation.value isNotBlankOr { element.simpleName.toString() }


sealed class RXtensionType() {
  class Func(val parameters: List<DataType>, val returns: TypeMirror) : RXtensionType()
  class Action(val parameters: List<DataType>) : RXtensionType()
}

private val DataType.typeName: TypeName
  get() = when (this) {
    is DataType.Parameter -> TypeName.get(parameter.asType())
    is DataType.Type -> TypeName.get(type.asType())
  }

fun RXtensionType.rxInterface(context: Context): RXInterface =
    when (this) {
      is Func -> when {
        parameters.size > 9 -> RXInterface(TypeName.get(rx.functions.FuncN::class.java), true)
        else -> ClassName.get("rx.functions", "Func${parameters.size}").let {
          ParameterizedTypeName.get(it, *(parameters.map { it.typeName } + TypeName.get(returns)).map { it.box() }.toTypedArray())
        }.let { RXInterface(it, false) }
      }
      is Action -> when {
        parameters.size > 9 -> RXInterface(TypeName.get(rx.functions.ActionN::class.java), true)
        parameters.isEmpty() -> RXInterface(TypeName.get(rx.functions.Action0::class.java), false)
        else -> ClassName.get("rx.functions", "Action${parameters.size}").let {
          ParameterizedTypeName.get(it, *(parameters.map { it.typeName.box() }).toTypedArray())
        }.let { RXInterface(it, false) }
      }
    }

class RXInterface(val typeName: TypeName, val eraseType: Boolean)

private fun TypeMirror.rxtensionType(data: List<DataType>) = when (kind) {
  TypeKind.VOID -> Action(data)
  else -> Func(data, this)
}

private val MethodReceiver.finalParameter: NamedFinalParameter?
  get() = when (this) {
    is MethodReceiver.Dynamic.Receiver -> NamedFinalParameter.Receiver.Member(type)
    is MethodReceiver.Dynamic.Partial -> NamedFinalParameter.Receiver.External(type)
    else -> null
  }

private val MethodReceiver.dataType: DataType.Type?
  get() = when (this) {
    is MethodReceiver.Dynamic.Data -> type
    else -> null
  }

private abstract class NamedFinalParameter() {
  abstract val name: String
  abstract val typeMirror: TypeMirror

  abstract class Receiver() : NamedFinalParameter() {
    class Member(val receiver: ReceiverType.Type) : Receiver() {
      override val name: String
        get() = receiver.name
      override val typeMirror: TypeMirror
        get() = receiver.type.typeMirror
    }

    class External(val receiver: PartialType.Type) : Receiver() {
      override val name: String
        get() = receiver.name
      override val typeMirror: TypeMirror
        get() = receiver.uniqueType.typeMirror
    }
  }

  sealed class Parameter() : NamedFinalParameter() {
    class Member(val receiver: ReceiverType.Parameter) : Parameter() {
      override val name: String
        get() = receiver.name
      override val typeMirror: TypeMirror
        get() = receiver.type.typeMirror
    }

    class External(val partialParameter: PartialType.Parameter) : Parameter() {
      override val name: String
        get() = partialParameter.name
      override val typeMirror: TypeMirror
        get() = partialParameter.uniqueType.typeMirror
    }
  }
}

val TypeMirror.typeName: TypeName
  get() = TypeName.get(this)

private fun NamedFinalParameter.typeName() = TypeName.get(typeMirror)

private fun NamedFinalParameter.equalsParameter(receiver: ReceiverType) =
    when (this) {
      is NamedFinalParameter.Parameter.Member -> this.receiver.equalsReceiver(receiver)
      else -> false
    }

private fun NamedFinalParameter.equalsParameter(partialParameter: PartialType) =
    when (this) {
      is NamedFinalParameter.Parameter.External -> this.partialParameter.equals(partialParameter)
      else -> false
    }


private val TypeSpec.Builder.addEntry: (Map.Entry<MethodSpec, TypeSpec>) -> Unit
  get() = { addMethod(it.key); addType(it.value) }


abstract class ReceiverType(val type: UniqueType, private val element: Element) {
  protected abstract val overrideName: String?
  val name: String by lazy(NONE) { overrideName ?: element.receiverName }

  constructor(element: Element, types: Types) : this(UniqueType(element.asType(), types), element)

  class Type(type: TypeElement, val annotation: Receiver?, types: Types) : ReceiverType(type, types) {
    override val overrideName = annotation?.value?.unless(String::isBlank)
  }

  class Parameter(parameter: VariableElement, val annotation: Receiver, types: Types, val index: Int) : ReceiverType(parameter, types) {
    override val overrideName = annotation.value unless String::isBlank
  }

  fun equalsReceiver(other: ReceiverType) = other.type.equals(type) && other.name.equals(name)
  val Element.receiverName: String
    get() = simpleName.toString().let(UPPER_CAMEL to LOWER_CAMEL)
  val debugString: String
    get() = overrideName?.let { "Receiver($it) ${type.typeMirror}" } ?: "${type.typeMirror}"
}

abstract class PartialType(val uniqueType: UniqueType, val annotation: Partial, private val element: Element) {
  constructor(element: Element, types: Types, annotation: Partial) : this(UniqueType(element.asType(), types), annotation, element)

  open val name = annotation.value.isNotBlankOr { element.constructorParameterName }
  abstract fun named(name: String): PartialType

  open class Type(val type: TypeElement, annotation: Partial, private val types: Types) : PartialType(type, types, annotation) {
    override fun named(name: String) = Named(name, this)
    class Named(override val name: String, val wrapped: Type) : Type(wrapped.type, wrapped.annotation, wrapped.types)
  }

  open class Parameter(val parameter: VariableElement, annotation: Partial, private val types: Types, val index: Int) : PartialType(parameter, types, annotation) {
    override fun named(name: String) = Named(name, this)
    class Named(override val name: String, val wrapped: Parameter) : Parameter(wrapped.parameter, wrapped.annotation, wrapped.types, wrapped.index)
  }

  fun equalsPartialParameter(other: PartialType) = other.uniqueType.equals(uniqueType) && other.name.equals(name)
  val Element.constructorParameterName: String
    get() = simpleName.toString().let(UPPER_CAMEL to LOWER_CAMEL)
}

sealed class DataType(val uniqueType: UniqueType, private val element: Element) {
  protected abstract val overrideName: String?
  open val name: String by lazy(NONE) { overrideName ?: element.dynamicParameterName }

  constructor(element: Element, types: Types) : this(UniqueType(element.asType(), types), element)

  class Type(val type: TypeElement, val annotation: Dynamic, types: Types) : DataType(type, types) {
    override val overrideName = annotation.value unless String::isEmpty
  }

  open class Parameter(val parameter: VariableElement, val annotation: Dynamic?, private val types: Types, val index: Int) : DataType(parameter, types) {
    override val overrideName = annotation?.value?.unless(String::isEmpty)
    fun named(name: String) = Named(name, this)
    class Named(override val name: String, val wrapped: Parameter) : Parameter(wrapped.parameter, wrapped.annotation, wrapped.types, wrapped.index)
  }

  fun equalsDynamicParameter(other: DataType) = other.uniqueType.equals(uniqueType) && other.name.equals(name)
  val Element.dynamicParameterName: String
    get() = simpleName.toString().let(UPPER_CAMEL to LOWER_CAMEL)

}

sealed class MethodReceiver {
  class Static(val type: TypeElement) : MethodReceiver()
  sealed class Dynamic : MethodReceiver() {
    class Receiver(val type: ReceiverType.Type) : Dynamic()
    class Partial(val type: PartialType.Type) : Dynamic()
    class Data(val type: DataType.Type) : Dynamic()
  }
}

val MethodReceiver.Dynamic.name: String
  get() = when (this) {
    is MethodReceiver.Dynamic.Receiver -> type.name
    is MethodReceiver.Dynamic.Partial -> type.name
    is MethodReceiver.Dynamic.Data -> type.name
  }

val MethodReceiver.Dynamic.typeMirror: TypeMirror
  get() = when (this) {
    is MethodReceiver.Dynamic.Receiver -> type.type.typeMirror
    is MethodReceiver.Dynamic.Partial -> type.uniqueType.typeMirror
    is MethodReceiver.Dynamic.Data -> type.uniqueType.typeMirror
  }

private fun RXtensionTarget.methodReceiver(context: Context): MethodReceiver {
  val types = context.processingEnv.typeUtils
  context.processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "types $types, element $element, receiver ${element.receiverType}")
  val methodReceiver = element.receiverType?.let { types.asElement(it) } as? TypeElement ?: element.enclosingTypeElement ?: throw RuntimeException("Cannot find receiver type of $element.")
  val scope = element.scopeAnnotation
  return when (scope) {
    null -> if (element.static) MethodReceiver.Static(methodReceiver) else MethodReceiver.Dynamic.Receiver(ReceiverType.Type(methodReceiver, null, types))
    is ScopeAnnotation._Receiver -> MethodReceiver.Dynamic.Receiver(ReceiverType.Type(methodReceiver, scope.annotation, types))
    is ScopeAnnotation._Partial -> MethodReceiver.Dynamic.Partial(PartialType.Type(methodReceiver, scope.annotation, types))
    is ScopeAnnotation._Dynamic -> MethodReceiver.Dynamic.Data(DataType.Type(methodReceiver, scope.annotation, types))
  }
}

private infix fun RXtensionTarget.parameters(context: Context): Triple<List<ReceiverType.Parameter>, List<PartialType.Parameter>, List<DataType.Parameter>> {
  val types = context.processingEnv.typeUtils
  val receiverParameters = mutableListOf<ReceiverType.Parameter>()
  val constructorParameters = mutableListOf<PartialType.Parameter>()
  val dynamicParameters = mutableListOf<DataType.Parameter>()
  element.parameters.mapIndexed { index, parameter ->
    val scope = parameter.scopeAnnotation
    when (scope) {
      null -> dynamicParameters.add(DataType.Parameter(parameter, null, types, index))
      is ScopeAnnotation._Receiver -> receiverParameters.add(ReceiverType.Parameter(parameter, scope.annotation, types, index))
      is ScopeAnnotation._Partial -> constructorParameters.add(PartialType.Parameter(parameter, scope.annotation, types, index))
      is ScopeAnnotation._Dynamic -> dynamicParameters.add(DataType.Parameter(parameter, scope.annotation, types, index))
    }
  }
  return Triple(receiverParameters, constructorParameters, dynamicParameters)
}

sealed class ScopeAnnotation {
  class _Receiver(val annotation: Receiver) : ScopeAnnotation()
  class _Partial(val annotation: Partial) : ScopeAnnotation()
  class _Dynamic(val annotation: Dynamic) : ScopeAnnotation()
}

private val Element.scopeAnnotation: ScopeAnnotation?
  get() = if ((this as? ExecutableElement)?.static.isTrue) {
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
        0, 1 -> receiver?.let { ScopeAnnotation._Receiver(it) } ?:
            partial?.let { ScopeAnnotation._Partial(it) } ?:
            dynamic?.let { ScopeAnnotation._Dynamic(it) }
        else -> throw IllegalArgumentException("$this cannot be annotated with $it at the same time.")
      }
    }
  }
