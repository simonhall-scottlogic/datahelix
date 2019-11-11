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
package com.scottlogic.deg.generator.profile.constraints.atomic;

import com.scottlogic.deg.common.profile.Field;
import com.scottlogic.deg.generator.fieldspecs.FieldSpec;
import com.scottlogic.deg.generator.fieldspecs.FieldSpecFactory;

import java.util.Collections;
import java.util.Objects;

public class NotEqualToConstraint implements AtomicConstraint {

    private final Field field;
    public final Object value;

    public NotEqualToConstraint(Field field, Object value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public AtomicConstraint negate() {
        return new EqualToConstraint(field, value);
    }

    @Override
    public FieldSpec toFieldSpec() {
        return FieldSpecFactory.fromType(field.getType()).withBlacklist(Collections.singleton(value));
    }

    @Override
    public String toString(){
        return String.format("`%s` = %s", field.getName(), value);
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o instanceof ViolatedAtomicConstraint) {
            return o.equals(this);
        }
        if (o == null || getClass() != o.getClass()) return false;
        NotEqualToConstraint constraint = (NotEqualToConstraint) o;
        return Objects.equals(field, constraint.field) && Objects.equals(value, constraint.value);
    }

    @Override
    public int hashCode(){
        return Objects.hash(field, value);
    }
}
