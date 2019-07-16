/*
 * Copyright 2019 Scott Logic Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scottlogic.deg.generator.fieldspecs.whitelist;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FrequencyDistributedSet<T> implements DistributedSet<T> {

    private static final FrequencyDistributedSet<?> EMPTY = new FrequencyDistributedSet<>(Collections.emptySet());

    private final Set<WeightedElement<T>> underlyingSet;

    public FrequencyDistributedSet(final Set<WeightedElement<T>> underlyingSet) {
        if (underlyingSet.isEmpty()) {
            this.underlyingSet = underlyingSet;
        } else {
            if (underlyingSet.contains(null)) {
                throw new IllegalArgumentException("DistributedSet should not contain null elements");
            }

            float total = underlyingSet.stream()
                .map(WeightedElement::weight)
                .reduce(0.0F, Float::sum);

            this.underlyingSet = underlyingSet.stream()
                .map(holder -> new WeightedElement<>(holder.element(), holder.weight() / total))
                .collect(Collectors.toSet());
        }
    }

    public static <T> FrequencyDistributedSet<T> uniform(final Set<T> underlyingSet) {
        return new FrequencyDistributedSet<>(
            underlyingSet.stream()
                .map(e -> new WeightedElement<T>(e, 1.0F))
                .collect(Collectors.toSet()));
    }

    @SuppressWarnings("unchecked")
    public static <T> FrequencyDistributedSet<T> empty() {
        return (FrequencyDistributedSet<T>) EMPTY;
    }

    @Override
    public Set<T> set() {
        return underlyingSet.stream().map(WeightedElement::element).collect(Collectors.toSet());
    }

    @Override
    public Set<WeightedElement<T>> distributedSet() {
        return underlyingSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrequencyDistributedSet<?> that = (FrequencyDistributedSet<?>) o;
        return Objects.equals(underlyingSet, that.underlyingSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(underlyingSet);
    }

    @Override
    public String toString() {
        return "FrequencyDistributedSet{" +
            "underlyingSet=" + underlyingSet +
            '}';
    }
}
