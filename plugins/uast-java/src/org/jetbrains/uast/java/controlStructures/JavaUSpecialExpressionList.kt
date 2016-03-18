/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.uast.java

import com.intellij.psi.PsiElement
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

open class JavaUSpecialExpressionList(
        override val psi: PsiElement,
        override val kind: UastSpecialExpressionKind, // original element
        override val parent: UElement
) : JavaAbstractUElement(), USpecialExpressionList, PsiElementBacked {
    class Empty(psi: PsiElement, expressionType: UastSpecialExpressionKind, parent: UElement) :
            JavaUSpecialExpressionList(psi, expressionType, parent) {
        init { expressions = emptyList() }
    }

    override lateinit var expressions: List<UExpression>
}