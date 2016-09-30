package com.dxm.auto.rxtension.internal

/**
 * Created by ants on 30/09/2016.
 */

val Any?.isNull: Boolean
  get() = null == this
val Any?.isNotNull: Boolean
  get() = null != this