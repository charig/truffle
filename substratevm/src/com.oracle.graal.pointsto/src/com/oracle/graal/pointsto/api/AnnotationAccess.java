/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.pointsto.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Wrapper class for annotation access that defends against
 * https://bugs.openjdk.java.net/browse/JDK-7183985: when an annotation declares a Class<?> array
 * parameter and one of the referenced classes is not present on the classpath parsing the
 * annotations will result in an ArrayStoreException instead of caching of a
 * TypeNotPresentExceptionProxy. This is a problem in JDK8 but was fixed in JDK11+.
 */
public final class AnnotationAccess {

    public static <T extends Annotation> T getAnnotation(AnnotatedElement element, Class<T> annotationType) {
        try {
            return element.getAnnotation(annotationType);
        } catch (ArrayStoreException e) {
            /*
             * Returning null essentially means that the element doesn't declare the annotationType,
             * but we cannot know that since the annotation parsing failed. However, this allows us
             * to defend against crashing the image builder if the above JDK bug is encountered in
             * user code.
             */
            return null;
        }
    }

    public static Annotation[] getAnnotations(AnnotatedElement element) {
        try {
            return element.getAnnotations();
        } catch (ArrayStoreException e) {
            /*
             * Returning an empty array essentially means that the element doesn't declare any
             * annotations, but we know that it is not true since the reason the annotation parsing
             * failed is because some annotation referenced a missing class. However, this allows us
             * to defend against crashing the image builder if the above JDK bug is encountered in
             * user code.
             */
            return new Annotation[0];
        }
    }

    public static <T extends Annotation> T getDeclaredAnnotation(AnnotatedElement element, Class<T> annotationType) {
        try {
            return element.getDeclaredAnnotation(annotationType);
        } catch (ArrayStoreException e) {
            /*
             * Returning null essentially means that the element doesn't declare the annotationType,
             * but we cannot know that since the annotation parsing failed. However, this allows us
             * to defend against crashing the image builder if the above JDK bug is encountered in
             * user code.
             */
            return null;
        }
    }

    public static Annotation[] getDeclaredAnnotations(AnnotatedElement element) {
        try {
            return element.getDeclaredAnnotations();
        } catch (ArrayStoreException e) {
            /*
             * Returning an empty array essentially means that the element doesn't declare any
             * annotations, but we know that it is not true since the reason the annotation parsing
             * failed is because it at least one annotation referenced a missing class. However,
             * this allows us to defend against crashing the image builder if the above JDK bug is
             * encountered in user code.
             */
            return new Annotation[0];
        }
    }
}
