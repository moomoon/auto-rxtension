package com.dxm.auto.rxtension.internal

/**
 * Created by ants on 9/23/16.
 */

fun<T> List<T>.append(other: List<T>): List<T> {
  val mutableList = other.toMutableList()
  mutableList.addAll(other)
  return mutableList
}