package com.dxm.auto.rxtension.processor

import com.dxm.auto.rxtension.Processor
import com.dxm.auto.rxtension.RXtension
import com.dxm.auto.rxtension.internal.Context
import com.dxm.auto.rxtension.internal.JavaFileHolder
import com.dxm.auto.rxtension.internal.Type
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeKind

/**
 * Created by Phoebe on 9/11/16.
 */
class RXtensionProcessor : Processor {
    override fun process(builders: Map<Type, JavaFileHolder>, context: Context)
            = context.roundEnv.getElementsAnnotatedWith(RXtension::class.java).forEach { target(it as ExecutableElement).build(builders, context) }

}

data class RXtensionTarget(val element: ExecutableElement, val annotation: RXtension)

private fun target(element: ExecutableElement) = RXtensionTarget(element, element.getAnnotation(RXtension::class.java))
enum class RXtensionType { Func, Action }

private fun RXtensionType.builder(target: RXtensionTarget) =
        when (this) {
            RXtensionType.Func -> RXtensionFuncBuilder(target)
            RXtensionType.Action -> RXtensionActionBuilder(target)
        }

private val RXtensionTarget.type: RXtensionType
    get() = if (element.returnType.kind.equals(TypeKind.VOID)) RXtensionType.Action else RXtensionType.Func;

private fun RXtensionTarget.build(builders: Map<Type, JavaFileHolder>, context: Context) = type.builder(this).build(builders, context)

abstract class RXtensionBuilder(val target: RXtensionTarget) {
    fun build(builders: Map<Type, JavaFileHolder>, context: Context) {
    }
}

class RXtensionFuncBuilder(target: RXtensionTarget) : RXtensionBuilder(target) {}
class RXtensionActionBuilder(target: RXtensionTarget) : RXtensionBuilder(target) {}
