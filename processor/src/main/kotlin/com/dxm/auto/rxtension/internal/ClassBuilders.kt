package com.dxm.auto.rxtension.internal

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec

/**
 * Created by Phoebe on 9/10/16.
 */

class JavaFileHolder(val packageName: String, private val builder: TypeSpec.Builder) {
  private val mutableMap: BiMap<MethodSpec, TypeSpec> = HashBiMap.create()
  private val mutableNameMap: BiMap<String, String> = HashBiMap.create()
  fun add(method: MethodSpec, type: TypeSpec) {
    mutableMap.put(method, type)
    mutableNameMap.put(method.name, type.name)
  }
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
  fun build() = JavaFile.builder(packageName, builder.apply { methodsAndTypes.forEach(builder.addEntry) }.build()!!).build()!!
}

private val TypeSpec.Builder.addEntry: (Map.Entry<MethodSpec, TypeSpec>) -> Unit
    get() = { addMethod(it.key); addType(it.value) }