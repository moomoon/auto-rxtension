package com.dxm.auto.rxtension.internal

/**
 * Created by ants on 9/21/16.
 */


fun <P1, R> Function1<P1, R>.partially1(p1: P1): () -> R {
  return { this(p1) }
}
