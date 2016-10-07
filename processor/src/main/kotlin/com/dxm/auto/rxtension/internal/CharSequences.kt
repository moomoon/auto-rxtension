package com.dxm.auto.rxtension.internal

import com.google.common.base.CaseFormat
import com.google.common.base.Converter

/**
 * Created by ants on 9/20/16.
 */

infix inline fun <T : CharSequence> T.isNotBlankOr(defaultVal: () -> T) = if (isNotBlank()) this else defaultVal()

fun String.uniqueIn(set: Set<String>): String {
  tailrec fun dedup(surfix: Int): String = "$this$surfix" unless { set.contains(this) } ?: dedup(surfix + 1)
  return unless { set.contains(this) } ?: dedup(0)
}

fun MutableSet<String>.unique(name: String): String = name.uniqueIn(this).apply { add(this) }

infix fun CaseFormat.to(format: CaseFormat): (String) -> String = { this.to(format, it) }