package com.dxm.auto.rxtension.internal

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec

/**
 * Created by Phoebe on 9/10/16.
 */

class JavaFileHolder(val packageName: String, private val builder: TypeSpec.Builder) {
  fun build() = JavaFile.builder(packageName, builder.build()).build()
}