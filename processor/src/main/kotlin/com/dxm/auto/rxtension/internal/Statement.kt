package com.dxm.auto.rxtension.internal

import com.squareup.javapoet.MethodSpec

/**
 * Created by ants on 08/10/2016.
 */

class Statement(val format: String, val args: List<Any>) {
  companion object {
    fun builder() = Builder()
  }

  class Builder() {
    var format = ""
    var args = listOf<Any>()

    fun build() = Statement(format, args)
    operator fun invoke(block: Statement.Builder.() -> Unit) = apply(block)
  }

  fun addTo(methodBuilder: MethodSpec.Builder) {
    methodBuilder.addStatement(this)
  }

}

fun MethodSpec.Builder.addStatement(statement: Statement) = addStatement(statement.format, *(statement.args.toTypedArray()))!!