/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.simple.pulsejob.common.util.internal;

/**
 * jupiter
 * org.jupiter.common.util.internal
 *
 * @author jiachun.fjc
 */
public class Updaters {

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> IntegerFieldUpdater<U> newIntegerFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeIntegerFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionIntegerFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> LongFieldUpdater<U> newLongFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeLongFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionLongFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U, W> ReferenceFieldUpdater<U, W> newReferenceFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeReferenceFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionReferenceFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
