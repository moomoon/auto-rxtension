package com.dxm.auto.rxtension.internal

import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.type.TypeMirror

/**
 * Created by Phoebe on 9/10/16.
 */

class Context(val roundEnv: RoundEnvironment, val processingEnv: ProcessingEnvironment) {
  val uniqueType: (typeMirror: TypeMirror) -> UniqueType = { UniqueType(it, processingEnv.typeUtils) }
}
