package com.dxm.auto.rxtension.internal

/**
 * Created by ants on 9/23/16.
 */

fun <T> List<T>.append(other: Collection<T>): List<T> {
  val mutableList = toMutableList()
  mutableList.addAll(other)
  return mutableList
}

fun <A, B, C> List<Triple<A, B, C>>.rotate() = Triple(map { it.first }, map { it.second }, map { it.third })