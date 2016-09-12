package com.dxm.auto.rxtension.internal

/**
 * Created by ants on 9/12/16.
 */

infix fun <A, B> ((A) -> B).mapOver(a: A?) = a?.let (this)