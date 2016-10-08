package com.dxm.auto.rxtension.internal

import com.dxm.auto.rxtension.Dynamic
import com.dxm.auto.rxtension.Partial
import com.dxm.auto.rxtension.Receiver
import com.dxm.auto.rxtension.internal.RXtensionType.*
import com.dxm.auto.rxtension.processor.RXtensionTarget
import com.google.common.base.CaseFormat.LOWER_CAMEL
import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.squareup.javapoet.*
import rx.functions.FuncN
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

/**
 * Created by Phoebe on 9/10/16.
 */

class JavaFileHolder(val packageName: String, private val builder: TypeSpec.Builder) {
  private val mutableMap: BiMap<MethodSpec, TypeSpec> = HashBiMap.create()
  private val mutableNameMap: BiMap<String, String> = HashBiMap.create()
  private val receivers: LinkedHashMap<String, ReceiverType> = LinkedHashMap()

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
  val typesAndMethods: Map<TypeSpec, MethodSpec>
    get() = mutableMap.inverse()
  val methods: Set<MethodSpec>
    get() = methodsAndTypes.keys
  val types: Set<TypeSpec>
    get() = mutableMap.values
  val methodNames: Set<String>
    get() = mutableNameMap.keys
  val typeNames: Set<String>
    get() = mutableNameMap.values

  fun add(target: RXtensionTarget, context: Context) {
    val (receiverParameters, partialParameters, dynamicParameters) = target.parameters(context)
    val methodReceiver = target.methodReceiver(context)
    receiverParameters.forEach { add(it) }
    val names = mutableSetOf<String>()
    val receiverFinalParameters = (listOf(methodReceiver.namedFinalParameter) + receiverParameters.map(NamedFinalParameter::ReceiverParameter)).filterNotNull()
    receiverFinalParameters.forEach { names.add(it.name) }
    val externalFinalParameters = partialParameters.map { NamedFinalParameter.External(it, names.unique(it.name)) }
    val bindingClass = target.bindingClass(methodReceiver, receiverFinalParameters + externalFinalParameters, dynamicParameters, context)
  }

  fun build() = JavaFile.builder(packageName, builder.apply { methodsAndTypes.forEach(builder.addEntry) }.build()!!).build()!!
}

private fun RXtensionTarget.bindingClass(methodReceiver: MethodReceiver, finalParameters: List<NamedFinalParameter>, dataParameters: List<DataType>, context: Context): TypeSpec {
  val builder = TypeSpec.classBuilder(bindingClassName)
  val fieldAndConstructorParam = finalParameters.map {
    FieldSpec.builder(it.typeName(), it.name, Modifier.FINAL, Modifier.PRIVATE).build() to
        ParameterSpec.builder(it.typeName(), it.name).build()
  }
  fieldAndConstructorParam.forEach { builder.addField(it.first) }
  val rxInterface = element.returnType.rxtensionType(dataParameters).rxInterface(context)
  builder.addSuperinterface(rxInterface.typeName)
  fieldAndConstructorParam.isNotEmpty().whenTrue {
    builder.addMethod(fieldAndConstructorParam.constructor())
  }
  builder.addMethod(fieldAndConstructorParam.map { it.first }.callMethod(methodReceiver, dataParameters, element.returnType.asReturnType, rxInterface.eraseType))
  return builder.build()
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
  get() = when(kind) {
    TypeKind.VOID -> TypeName.VOID
    else -> TypeName.get(this)
  }

private fun List<FieldSpec>.callMethod(methodReceiver: MethodReceiver, data: List<DataType>, returnType: TypeName, eraseType: Boolean): MethodSpec {
  val builder = MethodSpec.methodBuilder("call")
  builder.returns(returnType)
  return builder.build()
}
//
//private fun callMethod(finalParameters: List<NamedFinalParameter>, dynamicParameters: List<DynamicType>, eraseType: Boolean): MethodSpec {
//
//}

private val RXtensionTarget.bindingClassName: String
  get() = annotation.value.isNotBlankOr { element.simpleName.toString() }.let(LOWER_CAMEL to UPPER_CAMEL) + "Binding"
private val RXtensionTarget.bindingMethodName: String
  get() = annotation.value isNotBlankOr { element.simpleName.toString() }


sealed class RXtensionType() {
  class Func(val parameters: List<DataType>, val returns: TypeMirror) : RXtensionType()
  class Action(val parameters: List<DataType>) : RXtensionType()
}

private val DataType.typeName: TypeName
  get() = when(this) {
    is DataType.Parameter -> TypeName.get(parameter.asType())
    is DataType.Type -> TypeName.get(type.asType())
  }

fun RXtensionType.rxInterface(context: Context): RXInterface =
    when (this) {
      is Func -> if (parameters.size > 9) RXInterface(TypeName.get(rx.functions.FuncN::class.java), true)
      else ClassName.get("rx.functions", "Func${parameters.size}").let {
        ParameterizedTypeName.get(it, *(parameters.map { it.typeName.box() } + TypeName.get(returns)).toTypedArray())
      }.let { RXInterface(it, false) }
      is Action -> if (parameters.size > 9) RXInterface(TypeName.get(rx.functions.ActionN::class.java), true)
      else ClassName.get("rx.functions", "Action${parameters.size}").let {
        ParameterizedTypeName.get(it, *(parameters.map { it.typeName.box() }).toTypedArray())
      }.let { RXInterface(it, false) }
    }

class RXInterface(val typeName: TypeName, val eraseType: Boolean)
//
//private fun RXtensionType.builder(target: RXtensionTarget) =
//    when (this) {
//      Func -> RXtensionFuncBuilder(target)
//      Action -> RXtensionActionBuilder(target)
//    }

private fun TypeMirror.rxtensionType(data: List<DataType>): RXtensionType =
    when (kind) {
      TypeKind.VOID -> Action(data)
      else -> Func(data, this)
    }

//private val ExecutableElement.type: RXtensionType
//  get() = if (returnsKind(TypeKind.VOID)) Action(parameters) else Func(parameters, returnType)

private val MethodReceiver.namedFinalParameter: NamedFinalParameter.DynamicReceiver?
  get() = when (this) {
    is MethodReceiver.Dynamic -> NamedFinalParameter.DynamicReceiver(this)
    else -> null
  }

private sealed class NamedFinalParameter() {
  abstract val name: String
  abstract val typeMirror: TypeMirror

  class DynamicReceiver(val receiver: MethodReceiver.Dynamic) : NamedFinalParameter() {
    override val name: String
      get() = receiver.name
    override val typeMirror: TypeMirror
      get() = receiver.typeMirror
  }

  class ReceiverParameter(val receiver: ReceiverType.Parameter) : NamedFinalParameter() {
    override val name: String
      get() = receiver.name
    override val typeMirror: TypeMirror
      get() = receiver.type.typeMirror
  }

  class External(val partialParameter: PartialType, override val name: String) : NamedFinalParameter() {
    override val typeMirror: TypeMirror
      get() = partialParameter.uniqueType.typeMirror
  }
}

private fun NamedFinalParameter.typeName() = TypeName.get(typeMirror)

private fun NamedFinalParameter.equalsParameter(receiver: ReceiverType) =
    when (this) {
      is NamedFinalParameter.ReceiverParameter -> this.receiver.equalsReceiver(receiver)
      else -> false
    }

private fun NamedFinalParameter.equalsParameter(partialParameter: PartialType) =
    when (this) {
      is NamedFinalParameter.External -> this.partialParameter.equals(partialParameter)
      else -> false
    }


private val TypeSpec.Builder.addEntry: (Map.Entry<MethodSpec, TypeSpec>) -> Unit
  get() = { addMethod(it.key); addType(it.value) }


sealed class ReceiverType(val type: UniqueType, private val element: Element) {
  protected abstract val overrideName: String?
  val name: String by lazy(LazyThreadSafetyMode.NONE) { overrideName ?: element.receiverName }

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

sealed class PartialType(val uniqueType: UniqueType, val annotation: Partial, private val element: Element) {
  constructor(element: Element, types: Types, annotation: Partial) : this(UniqueType(element.asType(), types), annotation, element)

  val name = annotation.value.isNotBlankOr { element.constructorParameterName }

  class Type(val type: TypeElement, annotation: Partial, types: Types) : PartialType(type, types, annotation)
  class Parameter(val parameter: VariableElement, annotation: Partial, types: Types) : PartialType(parameter, types, annotation)

  fun equalsPartialParameter(other: PartialType) = other.uniqueType.equals(uniqueType) && other.name.equals(name)
  val Element.constructorParameterName: String
    get() = simpleName.toString().let(UPPER_CAMEL to LOWER_CAMEL)
}

sealed class DataType(val uniqueType: UniqueType, private val element: Element) {
  protected abstract val overrideName: String?
  val name: String by lazy(LazyThreadSafetyMode.NONE) { overrideName ?: element.dynamicParameterName }

  constructor(element: Element, types: Types) : this(UniqueType(element.asType(), types), element)

  class Type(val type: TypeElement, val annotation: Dynamic, types: Types) : DataType(type, types) {
    override val overrideName = annotation.value unless String::isEmpty
  }

  class Parameter(val parameter: VariableElement, val annotation: Dynamic?, types: Types) : DataType(parameter, types) {
    override val overrideName = annotation?.value?.unless(String::isEmpty)
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
  val methodReceiver = types.asElement(element.receiverType) as? TypeElement ?: throw RuntimeException("Cannot find receiver type of $element.")
  val scope = element.scopeAnnotation
  return when (scope) {
    null -> MethodReceiver.Static(methodReceiver)
    is ScopeAnnotation._Receiver -> MethodReceiver.Dynamic.Receiver(ReceiverType.Type(methodReceiver, scope.annotation, types))
    is ScopeAnnotation._Partial -> MethodReceiver.Dynamic.Partial(PartialType.Type(methodReceiver, scope.annotation, types))
    is ScopeAnnotation._Dynamic -> MethodReceiver.Dynamic.Data(DataType.Type(methodReceiver, scope.annatation, types))
  }
}

private infix fun RXtensionTarget.parameters(context: Context): Triple<List<ReceiverType.Parameter>, List<PartialType.Parameter>, List<DataType.Parameter>> {
  val types = context.processingEnv.typeUtils
  val receiverParameters = mutableListOf<ReceiverType.Parameter>()
  val constructorParameters = mutableListOf<PartialType.Parameter>()
  val dynamicParameters = mutableListOf<DataType.Parameter>()
  element.parameters.forEach { parameter ->
    val scope = parameter.scopeAnnotation
    when (scope) {
      null -> dynamicParameters.add(DataType.Parameter(parameter, null, types))
      is ScopeAnnotation._Receiver -> receiverParameters.add(ReceiverType.Parameter(parameter, scope.annotation, types))
      is ScopeAnnotation._Partial -> constructorParameters.add(PartialType.Parameter(parameter, scope.annotation, types))
      is ScopeAnnotation._Dynamic -> dynamicParameters.add(DataType.Parameter(parameter, scope.annatation, types))
    }
  }
  return Triple(receiverParameters, constructorParameters, dynamicParameters)
}

sealed class ScopeAnnotation {
  class _Receiver(val annotation: Receiver) : ScopeAnnotation()
  class _Partial(val annotation: Partial) : ScopeAnnotation()
  class _Dynamic(val annatation: Dynamic) : ScopeAnnotation()
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
        0, 1 -> receiver?.let(ScopeAnnotation::_Receiver) ?:
            partial?.let(ScopeAnnotation::_Partial) ?:
            dynamic?.let(ScopeAnnotation::_Dynamic)
        else -> throw IllegalArgumentException("$this cannot be annotated with $it at the same time.")
      }
    }
  }
