package com.dxm.auto.rxtension.processor

import com.dxm.auto.rxtension.Processor
import com.dxm.auto.rxtension.internal.Context
import com.dxm.auto.rxtension.internal.JavaFileHolder
import com.dxm.auto.rxtension.internal.Type

/**
 * Created by Phoebe on 9/11/16.
 */
class RXtensionProcessor: Processor {
  override fun process(builders: Map<Type, JavaFileHolder>, context: Context) {
    context.roundEnv
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
