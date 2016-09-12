package com.dxm.auto.rxtension.processor

import com.dxm.auto.rxtension.Processor
import com.dxm.auto.rxtension.RXtension
import com.dxm.auto.rxtension.RXtensionClass
import com.dxm.auto.rxtension.internal.*
import com.dxm.auto.rxtension.processor.RXtensionType.Action
import com.dxm.auto.rxtension.processor.RXtensionType.Func
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeSpec.classBuilder
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind.VOID

/**
 * Created by Phoebe on 9/11/16.
 */
class RXtensionProcessor : Processor {
  override fun process(builders: MutableMap<UniqueType, JavaFileHolder>, context: Context)
      = context.roundEnv.getElementsAnnotatedWith(RXtension::class.java).forEach { target(it as ExecutableElement).build(builders, context) }

}

class RXtensionTarget(val element: ExecutableElement, val annotation: RXtension) {
  val packageName = element.packageElement.simpleName.toString()
  fun containerClassName(): String = element.topLevelTypeElement!!.run { getAnnotation(RXtensionClass::class.java)?.name ?: simpleName.toString() + "_Extensions" }
  val emptyJavaFileHolder: () -> JavaFileHolder = { JavaFileHolder(packageName, classBuilder(containerClassName())) }
}

private fun target(element: ExecutableElement) = RXtensionTarget(element, element.getAnnotation(RXtension::class.java))
enum class RXtensionType { Func, Action }

private fun RXtensionType.builder(target: RXtensionTarget) =
    when (this) {
      Func -> RXtensionFuncBuilder(target)
      Action -> RXtensionActionBuilder(target)
    }

private val RXtensionTarget.type: RXtensionType
  get() = if (element.returnsKind(VOID)) Action else Func

private fun RXtensionTarget.build(builders: MutableMap<UniqueType, JavaFileHolder>, context: Context) = type.builder(this).build(builders, context)

abstract class RXtensionBuilder(val target: RXtensionTarget) {
  fun build(builders: MutableMap<UniqueType, JavaFileHolder>, context: Context) {
    val type = uniqueType(context) ?: return
    val holder = builders.getOrPut(type, target.emptyJavaFileHolder)
    typeSpec(context)?.let {
      if (null != methodSpec(it, context)?.let { holder.builder.addMethod(it) })
        holder.builder.addType(it)
    }
  }

  abstract fun methodSpec(type: TypeSpec, context: Context): MethodSpec?
  abstract fun typeSpec(context: Context): TypeSpec?
}

private fun RXtensionBuilder.uniqueType(context: Context) = context.uniqueType mapOver target.element.enclosingTypeElement?.asType()

class RXtensionFuncBuilder(target: RXtensionTarget) : RXtensionBuilder(target) {
  override fun methodSpec(type: TypeSpec, context: Context): MethodSpec? {

    return null
  }

  override fun typeSpec(context: Context): TypeSpec? {
    return null
  }
}

class RXtensionActionBuilder(target: RXtensionTarget) : RXtensionBuilder(target) {
  override fun methodSpec(type: TypeSpec, context: Context): MethodSpec {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun typeSpec(context: Context): TypeSpec {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}

private sealed class ReceiverType {
  class Type(val type: TypeElement): ReceiverType()
  class Instance(val type: TypeElement): ReceiverType()
  class Parameter(val parameter: VariableElement): ReceiverType()
}

private class Fields()

