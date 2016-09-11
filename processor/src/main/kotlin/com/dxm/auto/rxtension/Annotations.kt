package com.dxm.auto.rxtension

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

/**
 * Created by Phoebe on 9/10/16.
 */
@Retention(BINARY)
@Target(FUNCTION)
annotation class RXtension

@Retention(BINARY)
@Target(CLASS)
annotation class RXtensionClass(val value: KClass<out Any> = Any::class, val name: String = "")