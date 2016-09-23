package com.dxm.auto.rxtension.internal

/**
 * Created by ants on 9/20/16.
 */

infix inline fun <T> T.`if`(predicate: T.() -> Boolean): T? = if (this.predicate()) this else null
infix inline fun <T> T.unless(predicate: T.() -> Boolean): T? = if (this.predicate()) null else this