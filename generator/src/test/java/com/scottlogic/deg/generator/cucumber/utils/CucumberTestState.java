package com.scottlogic.deg.generator.cucumber.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scottlogic.deg.generator.Field;
import com.scottlogic.deg.generator.generation.GenerationConfig;
import com.scottlogic.deg.schemas.v3.ConstraintDTO;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to represent the state during cucumber test running and execution
 */
public class CucumberTestState {

    public GenerationConfig.DataGenerationType dataGenerationType;
    public GenerationConfig.CombinationStrategyType combinationStrategyType = GenerationConfig.CombinationStrategyType.PINNING;
    public GenerationConfig.TreeWalkerType walkerType = GenerationConfig.TreeWalkerType.CARTESIAN_PRODUCT;

    /**
     * Boolean to represent if the generation mode is validating or violating.
     * If true, generation is in violate mode.
     */
    public Boolean shouldViolate = false;

    /**
     * Boolean to represent if a reporting monitor is used.
     * If true, no monitor is implemented
     */
    public boolean quiet = true;

    //Default value of 10 million rows is set here.
    public long maxRows = GenerationConfig.Constants.DEFAULT_MAX_ROWS;

    public List<List<Object>> generatedObjects;

    final List<Field> profileFields = new ArrayList<>();
    final List<ConstraintDTO> constraints = new ArrayList<>();
    final List<Exception> testExceptions = new ArrayList<>();

    public void addConstraint(String fieldName, String constraintName, List<Object> value) {
        if (value == null)
            addConstraint(fieldName, constraintName, (Object)value);
        else
            addConstraint(fieldName, constraintName, getSetValues(value));
    }

    public void addNotConstraint(String fieldName, String constraintName, List<Object> value) {
        if (value == null)
            addNotConstraint(fieldName, constraintName, (Object)value);
        else
            addNotConstraint(fieldName, constraintName, getSetValues(value));
    }

    public void addConstraint(String fieldName, String constraintName, Object value) {
        ConstraintDTO dto = this.createConstraint(fieldName, constraintName, value);
        this.addConstraintToList(dto);
    }

    public void addNotConstraint(String fieldName, String constraintName, Object value) {
        ConstraintDTO notDto = new ConstraintDTO();
        notDto.not = this.createConstraint(fieldName, constraintName, value);
        this.addConstraintToList(notDto);
    }

    public void addConstraintsFromJson(String constraintProfile) throws IOException {
        ConstraintHolder holder = this.deserialise(constraintProfile);
        this.constraints.addAll(holder.constraints);
    }

    private Collection<Object> getSetValues(List<Object> values) {
        if (values == null){
            throw new IllegalArgumentException("Values cannot be null");
        }

        values.stream()
            .filter(value -> value instanceof Exception)
            .map(value -> (Exception)value)
            .forEach(this::addException);

        return values.stream()
            .filter(value -> !(value instanceof Exception))
            .collect(Collectors.toSet());
    }

    public void clearState(){
        this.profileFields.clear();
        this.constraints.clear();
        this.testExceptions.clear();
        this.generatedObjects = null;
    }

    public void addField(String fieldName) {
        this.profileFields.add(new Field(fieldName));
    }

    public void addException(Exception e){
        this.testExceptions.add(e);
    }

    private ConstraintDTO createConstraint(String fieldName, String constraintName, Object value) {
        ConstraintDTO dto = new ConstraintDTO();
        dto.field = fieldName;
        dto.is = this.extractConstraint(constraintName);
        if (value != null){
            if (value instanceof Collection){
                dto.values = (Collection<Object>) value;
            } else {
                dto.value = value;
            }
        }
        return dto;
    }

    private String extractConstraint(String gherkinConstraint) {
        List<String> allConstraints = Arrays.asList(gherkinConstraint.split(" "));
        return allConstraints.get(0) + allConstraints
            .stream()
            .skip(1)
            .map(value -> value.substring(0, 1).toUpperCase() + value.substring(1))
            .collect(Collectors.joining());
    }

    private void addConstraintToList(ConstraintDTO constraintDTO) {
        this.constraints.add(constraintDTO);
    }

    private ConstraintHolder deserialise(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        return mapper.readerFor(ConstraintHolder.class).readValue(json);
    }
}

class ConstraintHolder {
    public Collection<ConstraintDTO> constraints;
    public ConstraintHolder(){}
}
