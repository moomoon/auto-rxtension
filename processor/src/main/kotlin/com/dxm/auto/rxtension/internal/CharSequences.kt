package com.dxm.auto.rxtension.internal

import com.google.common.base.CaseFormat
import com.google.common.base.Converter

/**
 * Created by ants on 9/20/16.
 */

inline fun <T : CharSequence> T.isNotBlankOr(defaultVal: () -> T) = if (isNotBlank()) this else defaultVal()

fun String.uniqueIn(set: Set<String>): String {
  tailrec fun dedup(surfix: Int): String = "$this$surfix" unless { set.contains(this) } ?: dedup(surfix + 1)
  return unless { set.contains(this) } ?: dedup(0)
}

infix fun CaseFormat.to(format: CaseFormat): (String) -> String = converterTo(format).lambda
private val Converter<String, String>.lambda: (String) -> String
    get() = { this.convert(it)!! }