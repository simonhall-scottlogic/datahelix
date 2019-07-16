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

import java.util.Objects;

/**
 * Wrapper containing specified element with a weight.
 *
 * The weight must be non-zero and positive
 *
 * @param <E>
 */
public class WeightedElement<E> {

    private final E element;

    private final float weight;

    public WeightedElement(final E element, final float weight) {
        this.element = element;
        if (weight <= 0.0F) {
            throw new IllegalArgumentException("Cannot have a zero or negative valued weight");
        }
        this.weight = weight;
    }

    public E element() {
        return element;
    }

    public float weight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeightedElement<?> that = (WeightedElement<?>) o;
        return Float.compare(that.weight, weight) == 0 &&
            Objects.equals(element, that.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, weight);
    }

    @Override
    public String toString() {
        return "WeightedElement{" +
            "element=" + element +
            ", weight=" + weight +
            '}';
    }
}
