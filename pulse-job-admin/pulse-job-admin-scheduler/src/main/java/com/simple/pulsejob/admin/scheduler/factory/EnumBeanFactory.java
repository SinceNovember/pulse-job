package com.simple.pulsejob.admin.scheduler.factory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class EnumBeanFactory<K, V> {
    private final Map<K, V> beanMap;

    protected EnumBeanFactory(List<V> beans,
                              Function<V, K> keyExtractor,
                              String duplicateMsg) {
        this.beanMap = beans.stream()
            .collect(Collectors.toUnmodifiableMap(
                keyExtractor,
                Function.identity(),
                (left, right) -> { throw new IllegalStateException(duplicateMsg + left); }
            ));
    }

    public V get(K key) {
        V value = beanMap.get(key);
        if (value == null) {
            throw new IllegalArgumentException("not found enum type:" + key);
        }
        return value;
    }
}