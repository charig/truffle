/*
 * Copyright (c) 2018, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.nodes.intrinsics.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMIntrinsic;
import com.oracle.truffle.llvm.runtime.LLVMBoxedPrimitive;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMNativePointer;

@NodeChild(type = LLVMExpressionNode.class)
public abstract class LLVMTruffleIsHandleToManaged extends LLVMIntrinsic {

    @CompilationFinal private ContextReference<LLVMContext> contextRef;
    @CompilationFinal private LLVMMemory memory;

    @Specialization
    protected boolean doLongCase(long a,
                    @Cached("getContextReference()") ContextReference<LLVMContext> context) {
        return doPointerCase(LLVMNativePointer.create(a), context);
    }

    @Specialization
    protected boolean doPointerCase(LLVMNativePointer a,
                    @Cached("getContextReference()") ContextReference<LLVMContext> context) {
        if (canBeHandle(a)) {
            return context.get().isHandle(a);
        }
        return false;
    }

    @Specialization
    protected boolean doLLVMBoxedPrimitive(LLVMBoxedPrimitive from,
                    @Cached("getContextReference()") ContextReference<LLVMContext> context) {
        if (from.getValue() instanceof Long) {
            return doLongCase((long) from.getValue(), context);
        } else {
            return false;
        }
    }

    private boolean canBeHandle(LLVMNativePointer a) {
        if (memory == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            memory = getLLVMMemory();
        }
        return memory.isHandleMemory(a.asNative());
    }

    @Fallback
    protected boolean doGeneric(@SuppressWarnings("unused") Object object) {
        return false;
    }
}
