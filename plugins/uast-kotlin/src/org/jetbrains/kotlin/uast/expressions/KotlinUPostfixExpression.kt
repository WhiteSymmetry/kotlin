/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.uast

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UPostfixExpression
import org.jetbrains.uast.UastPostfixOperator
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUPostfixExpression(
        override val psi: KtPostfixExpression,
        override val parent: UElement
) : KotlinAbstractUElement(), UPostfixExpression, PsiElementBacked, KotlinTypeHelper, KotlinEvaluateHelper {
    override val operand by lz { KotlinConverter.convertOrEmpty(psi.baseExpression, this) }

    override val operator = when (psi.operationToken) {
        KtTokens.PLUSPLUS -> UastPostfixOperator.INC
        KtTokens.MINUSMINUS -> UastPostfixOperator.DEC
        KtTokens.EXCLEXCL -> KotlinPostfixOperators.EXCLEXCL
        else -> UastPostfixOperator.UNKNOWN
    }
}