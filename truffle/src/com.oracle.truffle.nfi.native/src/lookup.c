/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#define _GNU_SOURCE
#include <dlfcn.h>

#include "native.h"

#include <string.h>
#include "internal.h"

JNIEXPORT jlong JNICALL Java_com_oracle_truffle_nfi_impl_NFIContext_loadLibrary(JNIEnv *env, jclass self, jlong context, jstring name, jint flags) {
    const char *utfName = (*env)->GetStringUTFChars(env, name, NULL);
    void *ret = dlopen(utfName, flags);
    if (ret == NULL) {
        const char *error = dlerror();
        struct __TruffleContextInternal *ctx = (struct __TruffleContextInternal *) context;
        (*env)->ThrowNew(env, ctx->UnsatisfiedLinkError, error);
    }
    (*env)->ReleaseStringUTFChars(env, name, utfName);
    return (jlong) ret;
}

JNIEXPORT void JNICALL Java_com_oracle_truffle_nfi_impl_NFIContext_freeLibrary(JNIEnv *env, jclass self, jlong handle) {
    dlclose((void*) handle);
}

static jlong lookup(JNIEnv *env, jlong context, void *handle, jstring name) {
    struct __TruffleContextInternal *ctx = (struct __TruffleContextInternal *) context;
    const char *utfName = (*env)->GetStringUTFChars(env, name, NULL);
    // clear previous errors
    dlerror();
    void *ret = dlsym(handle, utfName);
    if (ret == NULL) {
        const char *error = dlerror();
        // if error == NULL, the symbol was found, but really points to NULL
        if (error != NULL) {
            (*env)->ThrowNew(env, ctx->UnsatisfiedLinkError, error);
        }
    }
    (*env)->ReleaseStringUTFChars(env, name, utfName);
    return (jlong) check_intrinsify(ctx, ret);
}

JNIEXPORT jlong JNICALL Java_com_oracle_truffle_nfi_impl_NFIContext_lookup(JNIEnv *env, jclass self, jlong context, jlong library, jstring name) {
    if (library == 0) {
        return lookup(env, context, RTLD_DEFAULT, name);
    } else {
        return lookup(env, context, (void *) library, name);
    }
}
