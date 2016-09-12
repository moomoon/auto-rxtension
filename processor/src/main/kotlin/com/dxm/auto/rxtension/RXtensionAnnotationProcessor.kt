package com.dxm.auto.rxtension

import com.dxm.auto.rxtension.internal.Context
import com.dxm.auto.rxtension.internal.JavaFileHolder
import com.dxm.auto.rxtension.internal.Type
import com.google.auto.service.AutoService
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR

/**
 * Created by Phoebe on 9/10/16.
 */

@SupportedAnnotationTypes("com.dxm.auto.rxtension.*")
@AutoService(Processor::class)
class RXtensionAnnotationProcessor : AbstractProcessor() {
  override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
    val processingEnv = processingEnv ?: return false
    val roundEnv = roundEnv ?: return false
    val context = Context(roundEnv = roundEnv, processingEnv = processingEnv)
    val builders: Map<Type, JavaFileHolder> = hashMapOf()
    try {
      Processor.allProcessors.forEach { it.process(builders, context) }
      builders.values.forEach { it.build().writeTo(processingEnv.filer) }
    } catch(e: Exception) {
      processingEnv.messager.printMessage(ERROR, e.message)
    }
    return true
  }
}