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
package com.simple.pulsejob.common.util;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.simple.pulsejob.common.concurrent.collection.NonBlockingHashMap;
import com.simple.pulsejob.common.concurrent.collection.NonBlockingHashMapLong;
import com.simple.pulsejob.common.util.internal.UnsafeUtil;

/**
 * Static utility methods pertaining to {@link Map} instances.
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class Maps {

    private static final boolean USE_NON_BLOCKING_HASH = SystemPropertyUtil.getBoolean("jupiter.use.non_blocking_hash", false);

    /**
     * Creates a mutable, empty {@code HashMap} instance.
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * Creates a {@code HashMap} instance, with a high enough "initial capacity"
     * that it should hold {@code expectedSize} elements without growth.
     */
    public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(int expectedSize) {
        return new HashMap<>(capacity(expectedSize));
    }

    /**
     * Creates an {@code IdentityHashMap} instance.
     */
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
        return new IdentityHashMap<>();
    }

    /**
     * Creates an {@code IdentityHashMap} instance, with a high enough "initial capacity"
     * that it should hold {@code expectedSize} elements without growth.
     */
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMapWithExpectedSize(int expectedSize) {
        return new IdentityHashMap<>(capacity(expectedSize));
    }

    /**
     * Creates a mutable, empty, insertion-ordered {@code LinkedHashMap} instance.
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    /**
     * Creates a mutable, empty {@code TreeMap} instance using the natural ordering of its elements.
     */
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<>();
    }

    /**
     * Creates a mutable, empty {@code ConcurrentMap} instance.
     */
    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        if (USE_NON_BLOCKING_HASH && UnsafeUtil.hasUnsafe()) {
            return new NonBlockingHashMap<>();
        }
        return new ConcurrentHashMap<>();
    }

    /**
     * Creates a {@code ConcurrentMap} instance, with a high enough "initial capacity"
     * that it should hold {@code expectedSize} elements without growth.
     */
    public static <K, V> ConcurrentMap<K, V> newConcurrentMap(int initialCapacity) {
        if (USE_NON_BLOCKING_HASH && UnsafeUtil.hasUnsafe()) {
            return new NonBlockingHashMap<>(initialCapacity);
        }
        return new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     * Creates a mutable, empty {@code NonBlockingHashMapLong} instance.
     */
    public static <V> ConcurrentMap<Long, V> newConcurrentMapLong() {
        return new NonBlockingHashMapLong<>();
    }

    /**
     * Creates a {@code NonBlockingHashMapLong} instance, with a high enough "initial capacity"
     * that it should hold {@code expectedSize} elements without growth.
     */
    public static <V> ConcurrentMap<Long, V> newConcurrentMapLong(int initialCapacity) {
        return new NonBlockingHashMapLong<>(initialCapacity);
    }

    /**
     * Returns a capacity that is sufficient to keep the map from being resized as
     * long as it grows no larger than expectedSize and the load factor is >= its
     * default (0.75).
     */
    private static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            Requires.requireTrue(expectedSize >= 0, "expectedSize cannot be negative but was: " + expectedSize);
            return expectedSize + 1;
        }
        if (expectedSize < Ints.MAX_POWER_OF_TWO) {
            return expectedSize + expectedSize / 3;
        }
        return Integer.MAX_VALUE; // any large value
    }

    private Maps() {}
}
