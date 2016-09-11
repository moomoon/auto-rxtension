package com.dxm.auto.rxtension.internal

import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

/**
 * Created by Phoebe on 9/10/16.
 */

class Type (val typeMirror: TypeMirror, private val types: Types) {
  val a: String by lazy {
    ""
  }
  override fun equals(other: Any?) = (other as? Type)?.equalsTypeMirror(typeMirror) ?: super.equals(other)
  private fun equalsTypeMirror(other: TypeMirror) = typeMirror.equalsTypeMirror(other, types)
  override fun hashCode(): Int{
    var result = typeMirror.hashCode()
    result = 31 * result + types.hashCode()
    return result
  }
}

fun TypeMirror.equalsTypeMirror(other: TypeMirror, types: Types) = types.isSameType(this, other)
fun Types.type(typeMirror: TypeMirror) = Type(typeMirror, this)