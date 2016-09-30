package com.dxm.auto.rxtension.internal

import com.dxm.auto.rxtension.processor.ConstructorParameter
import com.dxm.auto.rxtension.processor.DynamicParameter
import com.dxm.auto.rxtension.processor.ReceiverType
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.util.*
import javax.lang.model.type.TypeMirror

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
    if (!receiver.equals(it)) throw IllegalArgumentException("${receiver.debugString} collides with ${it.debugString}")
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

  fun add(receivers: List<ReceiverType>, constructorParameters: List<ConstructorParameter>, dynamicParameters: List<DynamicParameter>, returnType: TypeMirror) {

  }

  fun build() = JavaFile.builder(packageName, builder.apply { methodsAndTypes.forEach(builder.addEntry) }.build()!!).build()!!
}

private val TypeSpec.Builder.addEntry: (Map.Entry<MethodSpec, TypeSpec>) -> Unit
  get() = { addMethod(it.key); addType(it.value) }

