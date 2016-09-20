package com.dxm.auto.rxtension.internal

/**
 * Created by ants on 9/20/16.
 */

inline fun <T : CharSequence> T.isNotBlankOr(defaultVal: () -> T) = if (isNotBlank()) this else defaultVal()

fun String.uniqueIn(set: Set<String>): String {
  tailrec fun dedup(surfix: Int): String = "$this$surfix".whenFalse { set.contains(this) } ?: dedup(surfix + 1)
  return whenFalse { set.contains(this) } ?: dedup(0)
}
