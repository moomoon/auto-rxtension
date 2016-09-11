package com.dxm.auto.rxtension

import com.dxm.auto.rxtension.internal.Context
import com.dxm.auto.rxtension.internal.JavaFileHolder
import com.dxm.auto.rxtension.internal.Type
import com.dxm.auto.rxtension.processor.RXtensionProcessor

/**
 * Created by Phoebe on 9/10/16.
 */

interface Processor {
  fun process(builders: Map<Type, JavaFileHolder>, context: Context)
  companion object AllProcessors {
    val allProcessors: Array<Processor> = arrayOf(RXtensionProcessor())
  }
}