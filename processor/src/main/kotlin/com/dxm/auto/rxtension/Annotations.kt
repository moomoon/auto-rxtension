package com.dxm.auto.rxtension

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

/**
 * Created by Phoebe on 9/10/16.
 */

@Retention(BINARY)
@Target(FUNCTION)
annotation class RXtension(val value: String = "", val exception: KClass<in RuntimeException> = RuntimeException::class)

@Retention(BINARY)
@Target(CLASS)
annotation class RXtensionClass(val value: String = "")

@Retention(BINARY)
@Target(VALUE_PARAMETER, FUNCTION)
annotation class Receiver(val value: String = "")

@Retention(BINARY)
@Target(VALUE_PARAMETER, FUNCTION)
annotation class Partial(val value: String = "")

@Retention(BINARY)
@Target(VALUE_PARAMETER, FUNCTION)
annotation class Dynamic(val value: String = "")
