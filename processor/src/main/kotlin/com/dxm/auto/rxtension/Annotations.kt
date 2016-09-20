package com.dxm.auto.rxtension

import com.dxm.auto.rxtension.StaticPolicy.Auto
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

/**
 * Created by Phoebe on 9/10/16.
 */

enum class StaticPolicy {
  Auto, Static
}

@Retention(BINARY)
@Target(FUNCTION)
annotation class RXtension(val name: String = "", val scope: StaticPolicy = Auto)

@Retention(BINARY)
@Target(CLASS)
annotation class RXtensionClass(val value: KClass<out Any> = Any::class, val name: String = "")

@Retention(BINARY)
@Target(VALUE_PARAMETER)
annotation class Receiver