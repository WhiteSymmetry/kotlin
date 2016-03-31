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

import com.intellij.psi.PsiKeyword
import com.intellij.psi.PsiLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.psi.PsiElementBacked

class JavaULiteralExpression(
        override val psi: PsiLiteralExpression,
        override val parent: UElement
) : JavaAbstractUElement(), ULiteralExpression, PsiElementBacked, JavaUElementWithType {
    override fun asString() = psi.text
    override fun evaluate() = psi.value
    override val value by lz { evaluate() }

    override val isNull: Boolean
        get() = asString() == PsiKeyword.NULL
}