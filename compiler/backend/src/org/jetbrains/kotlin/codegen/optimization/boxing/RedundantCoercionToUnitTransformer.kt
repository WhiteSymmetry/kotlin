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

package org.jetbrains.kotlin.codegen.optimization.boxing

import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.fixStack.peek
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.removeNodeGetNext
import org.jetbrains.kotlin.codegen.optimization.replaceNodeGetNext
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue
import org.jetbrains.org.objectweb.asm.util.Printer

class RedundantCoercionToUnitTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        Transformer(methodNode).transform()
    }

    private class Transformer(val methodNode: MethodNode) {
        private val insnList = methodNode.instructions

        private val frames: Array<Frame<SourceValue>?> = Analyzer<SourceValue>(SourceInterpreter()).analyze("fake", methodNode)
        private val insns = insnList.toArray()

        private val dontTouchInsns = hashSetOf<AbstractInsnNode>()
        private val transformations = hashMapOf<AbstractInsnNode, () -> Unit>()
        private val insertedNops = hashSetOf<AbstractInsnNode>()

        fun transform() {
            computeResultMovedInsns()
            computeTransformations()
            applyTransformations()
            removeUnneededNops()
        }

        private fun computeResultMovedInsns() {
            for (i in 0..insns.lastIndex) {
                val frame = frames[i] ?: continue
                val insn = insns[i]

                when (insn.opcode) {
                    Opcodes.DUP ->
                        dontTouchWordsOnTop(i, frame, 1)
                    Opcodes.DUP_X1 ->
                        dontTouchWordsOnTop(i, frame, 2)
                    Opcodes.DUP_X2 ->
                        dontTouchWordsOnTop(i, frame, 3)
                    Opcodes.DUP2 ->
                        dontTouchWordsOnTop(i, frame, 2)
                    Opcodes.DUP2_X1 ->
                        dontTouchWordsOnTop(i, frame, 3)
                    Opcodes.DUP2_X2 ->
                        dontTouchWordsOnTop(i, frame, 4)
                    Opcodes.SWAP ->
                        dontTouchWordsOnTop(i, frame, 2)
                }
            }
        }

        private fun dontTouchWordsOnTop(at: Int, frame: Frame<SourceValue>, expectedWords: Int) {
            var words = 0
            var offset = 0
            while (words < expectedWords) {
                val value = frame.peek(offset) ?: throwIllegalStackInsn(at)
                offset++
                words += value.size
                dontTouchInsns.addAll(value.insns)
            }
            if (words != expectedWords) throwIllegalStackInsn(at)
        }

        private fun computeTransformations() {
            transformations.clear()

            for (i in 0..insns.lastIndex) {
                if (frames[i] == null) continue
                val insn = insns[i]

                if (insn.opcode == Opcodes.POP) {
                    propagatePopBackwards(insn, 0)
                }
            }
        }

        private fun applyTransformations() {
            for (transformation in transformations.values) {
                transformation()
            }
        }

        private fun removeUnneededNops() {
            var node: AbstractInsnNode? = insnList.first
            while (node != null) {
                node = node.next
                val begin = node ?: break
                while (node != null && node !is LabelNode) {
                    node = node.next
                }
                val end = node
                removeUnneededNopsInRange(begin, end)
            }
        }

        private fun removeUnneededNopsInRange(begin: AbstractInsnNode, end: AbstractInsnNode?) {
            val insnSeq = InsnSequence(begin, end)
            val hasNops = insnSeq.any { it.opcode == Opcodes.NOP }
            val hasInsns = insnSeq.any { it.opcode != -1 && it.opcode != Opcodes.NOP }

            if (!hasNops) return

            var node: AbstractInsnNode? = begin
            var keepNop = !hasInsns
            while (node != null && node != end) {
                if (insertedNops.contains(node) && !keepNop) {
                    keepNop = false
                    node = insnList.removeNodeGetNext(node)
                }
                else {
                    if (node.opcode == Opcodes.NOP) {
                        keepNop = false
                    }
                    node = node.next
                }
            }
        }

        private fun propagatePopBackwards(insn: AbstractInsnNode, poppedValueSize: Int) {
            if (transformations.containsKey(insn)) return

            when {
                insn.opcode == Opcodes.POP -> {
                    val inputTop = getInputTop(insn)
                    val sources = inputTop.insns
                    if (sources.all { !isDontTouch(it) } && sources.any { isTransformablePopOperand(it) }) {
                        transformations[insn] = replaceWithNopTransformation(insn)
                        sources.forEach { propagatePopBackwards(it, inputTop.size) }
                    }
                }

                insn.opcode == Opcodes.CHECKCAST -> {
                    val inputTop = getInputTop(insn)
                    val sources = inputTop.insns
                    val resultType = (insn as TypeInsnNode).desc
                    if (sources.all { !isDontTouch(it) } && sources.any { isTransformableCheckcastOperand(it, resultType) }) {
                        transformations[insn] = replaceWithNopTransformation(insn)
                        sources.forEach { propagatePopBackwards(it, inputTop.size) }
                    }
                    else {
                        transformations[insn] = insertPopAfterTransformation(insn, poppedValueSize)
                    }
                }

                insn.isPrimitiveBoxing() -> {
                    val boxedValueSize = getInputTop(insn).size
                    transformations[insn] = replaceWithPopTransformation(insn, boxedValueSize)
                }

                insn.isUnitOrNull() -> {
                    transformations[insn] = replaceWithNopTransformation(insn)
                }

                else -> {
                    transformations[insn] = insertPopAfterTransformation(insn, poppedValueSize)
                }
            }
        }

        private fun replaceWithPopTransformation(insn: AbstractInsnNode, size: Int): () -> Unit =
                { insnList.replaceNodeGetNext(insn, createPopInsn(size)) }

        private fun insertPopAfterTransformation(insn: AbstractInsnNode, size: Int) =
                { insnList.insert(insn, createPopInsn(size)) }

        private fun replaceWithNopTransformation(insn: AbstractInsnNode): () -> Unit =
                {
                    val nop = InsnNode(Opcodes.NOP)
                    insertedNops.add(nop)
                    insnList.replaceNodeGetNext(insn, nop)
                }

        private fun createPopInsn(size: Int) =
                when (size) {
                    1 -> InsnNode(Opcodes.POP)
                    2 -> InsnNode(Opcodes.POP2)
                    else -> throw AssertionError("Unexpected popped value size: $size")
                }

        private fun getInputTop(insn: AbstractInsnNode): SourceValue {
            val i = insnList.indexOf(insn)
            val frame = frames[i] ?: throw AssertionError("Unexpected dead instruction #$i")
            return frame.top() ?: throw AssertionError("Instruction #$i has empty stack on input")
        }

        private fun isTransformableCheckcastOperand(it: AbstractInsnNode, resultType: String) =
                it.isPrimitiveBoxing() && (it as MethodInsnNode).owner == resultType

        private fun isTransformablePopOperand(insn: AbstractInsnNode) =
                insn.opcode == Opcodes.CHECKCAST || insn.isPrimitiveBoxing() || insn.isUnitOrNull()

        private fun isDontTouch(insn: AbstractInsnNode) =
                dontTouchInsns.contains(insn)

        private fun throwIllegalStackInsn(i: Int): Nothing =
                throw AssertionError("#$i: illegal use of ${Printer.OPCODES[insns[i].opcode]}, input stack: ${formatInputStack(frames[i])}")

        private fun formatInputStack(frame: Frame<SourceValue>?): String =
                if (frame == null)
                    "unknown (dead code)"
                else
                    (0..frame.stackSize - 1).map { frame.getStack(it).size }.joinToString(prefix =  "[", postfix = "]")
    }

}

fun AbstractInsnNode.isUnitOrNull() =
        opcode == Opcodes.ACONST_NULL ||
        opcode == Opcodes.GETSTATIC && this is FieldInsnNode && owner == "kotlin/Unit" && name == "INSTANCE"
