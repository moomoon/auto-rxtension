package com.dxm.auto.rxtension

import com.dxm.auto.rxtension.internal.Context
import com.dxm.auto.rxtension.internal.JavaFileHolder
import com.dxm.auto.rxtension.internal.UniqueType
import com.google.auto.service.AutoService
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.NOTE

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
    val builders: MutableMap<UniqueType, JavaFileHolder> = mutableMapOf()
    try {
      Processor.allProcessors.forEach { it.process(builders, context) }
      builders.values.forEach { it.build().writeTo(processingEnv.filer) }
      processingEnv.messager.printMessage(NOTE, "messaging")

    } catch(e: Exception) {
      processingEnv.messager.printMessage(ERROR, e.message)
    }
    return true
  }
}