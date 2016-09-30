package com.dxm.auto.rxtension.internal

/**
 * Created by ants on 30/09/2016.
 */

//operator fun <A, B, C> Triple<A, B, C>?.component1() = this?.component1()
//operator fun <A, B, C> Triple<A, B, C>?.component2() = this?.component2()
//operator fun <A, B, C> Triple<A, B, C>?.component3() = this?.component3()

fun <A, B, C> Triple<A, B, C>?.nullableElements() = this ?: Triple(null, null, null)