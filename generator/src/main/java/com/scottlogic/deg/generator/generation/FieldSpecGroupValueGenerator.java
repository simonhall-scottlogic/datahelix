package com.scottlogic.deg.generator.generation;

import com.scottlogic.deg.common.profile.Field;
import com.scottlogic.deg.generator.fieldspecs.FieldSpec;
import com.scottlogic.deg.generator.fieldspecs.FieldSpecGroup;
import com.scottlogic.deg.generator.generation.databags.DataBag;
import com.scottlogic.deg.generator.generation.databags.DataBagValue;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldSpecGroupValueGenerator {

    private final FieldSpecValueGenerator underlyingGenerator;

    public FieldSpecGroupValueGenerator(FieldSpecValueGenerator underlyingGenerator) {
        this.underlyingGenerator = underlyingGenerator;
    }

    public Stream<DataBag> generate(FieldSpecGroup group) {
        Field first = group.fieldSpecs().keySet().iterator().next();

        FieldSpecGroup groupRespectingFirstField = initialAdjustments(first, group);
        FieldSpec firstSpec = groupRespectingFirstField.fieldSpecs().get(first);

        Stream<DataBag> firstDataBagValues = underlyingGenerator.generate(firstSpec)
            .map(value -> toDataBag(first, value));

        return createRemainingDataBags(firstDataBagValues, first, groupRespectingFirstField);
    }

    private static DataBag toDataBag(Field field, DataBagValue value) {
        Map<Field, DataBagValue> map = new HashMap<>();
        map.put(field, value);
        return new DataBag(map);
    }

    private static FieldSpecGroup initialAdjustments(Field field, FieldSpecGroup group) {
        return group;
    }

    private static FieldSpecGroup adjustBounds(Field field, DataBagValue value, FieldSpecGroup group) {
        return group;
    }

    private static final class DataBagGroupWrapper {

        private final DataBag dataBag;
        private final FieldSpecGroup group;
        private final FieldSpecValueGenerator generator;

        private DataBagGroupWrapper(DataBag databag,
                                    FieldSpecGroup group,
                                    FieldSpecValueGenerator generator) {
            this.dataBag = databag;
            this.group = group;
            this.generator = generator;
        }

        public DataBag dataBag() {
            return dataBag;
        }

        public FieldSpecValueGenerator generator() {
            return generator;
        }

    }

    private Stream<DataBag> createRemainingDataBags(Stream<DataBag> stream, Field first, FieldSpecGroup group) {
        Stream<DataBagGroupWrapper> initial = stream
            .map(dataBag -> new DataBagGroupWrapper(dataBag, group, underlyingGenerator))
            .map(wrapper -> adjustWrapperBounds(wrapper, first));
        Set<Field> toProcess = filterFromSet(group.fieldSpecs().keySet(), first);

        return recursiveMap(initial, toProcess).map(DataBagGroupWrapper::dataBag);
    }

    private static DataBagGroupWrapper adjustWrapperBounds(DataBagGroupWrapper wrapper, Field field) {
        DataBagValue value = wrapper.dataBag.getUnformattedValue(field);
        FieldSpecGroup newGroup = adjustBounds(field, value, wrapper.group);
        return new DataBagGroupWrapper(wrapper.dataBag, newGroup, wrapper.generator);

    }

    private static Stream<DataBagGroupWrapper> recursiveMap(Stream<DataBagGroupWrapper> wrapperStream,
                                                            Set<Field> fieldsToProcess) {
        if (fieldsToProcess.isEmpty()) {
            return wrapperStream;
        }

        Field field = fieldsToProcess.iterator().next();

        Stream<DataBagGroupWrapper> mappedStream = wrapperStream.flatMap(wrapper -> acceptNextValue(wrapper, field));
        Set<Field> remainingFields = filterFromSet(fieldsToProcess, field);

        return recursiveMap(mappedStream, remainingFields);
    }

    private static <T> Set<T> filterFromSet(Set<T> original, T element) {
        return original.stream()
            .filter(f -> !f.equals(element))
            .collect(Collectors.toSet());
    }

    private static Stream<DataBagGroupWrapper> acceptNextValue(DataBagGroupWrapper wrapper, Field field) {
        if (wrapper.generator.isRandom()) {
            return Stream.of(acceptNextRandomValue(wrapper, field));
        } else {
            return acceptNextNonRandomValue(wrapper, field);
        }
    }

    private static DataBagGroupWrapper acceptNextRandomValue(DataBagGroupWrapper wrapper, Field field) {
        FieldSpecGroup group = wrapper.group;

        DataBagValue nextValue = wrapper.generator().generateOne(group.fieldSpecs().get(field));

        DataBag combined = DataBag.merge(toDataBag(field, nextValue), wrapper.dataBag);

        FieldSpecGroup newGroup = adjustBounds(field, nextValue, group);

        return new DataBagGroupWrapper(combined, newGroup, wrapper.generator);
    }

    private static final class WrappedDataBag<T> {
        private final DataBag dataBag;
        private final T other;

        public WrappedDataBag(DataBag dataBag, T other) {
            this.dataBag = dataBag;
            this.other = other;
        }
    }

    private static Stream<DataBagGroupWrapper> acceptNextNonRandomValue(DataBagGroupWrapper wrapper, Field field) {
        FieldSpecGroup group = wrapper.group;
        return wrapper.generator().generate(group.fieldSpecs().get(field))
            .map(value -> new WrappedDataBag<>(toDataBag(field, value), value))
            .map(wrapped -> new WrappedDataBag<>(DataBag.merge(wrapped.dataBag, wrapper.dataBag), wrapped.other))
            .map(combined -> new DataBagGroupWrapper(
                combined.dataBag, adjustBounds(field, combined.other, wrapper.group), wrapper.generator)
            );
    }

}
