package com.dxm.auto.rxtension.internal

/**
 * Created by ants on 9/20/16.
 */

inline fun <T> T.whenTrue(predicate: T.() -> Boolean): T? = if (this.predicate()) this else null
inline fun <T> T.whenFalse(predicate: T.() -> Boolean): T? = if (this.predicate()) null else this