package com.scottlogic.deg.generator.inputs.validation.messages;

import com.scottlogic.deg.generator.inputs.validation.StandardValidationMessages;

import java.math.BigDecimal;

public class GranularityConstraintValidationMessages implements StandardValidationMessages {


    private BigDecimal validValue;
    private BigDecimal invalidValue;

    public GranularityConstraintValidationMessages(BigDecimal validValue, BigDecimal invalidValue){

        this.validValue = validValue;
        this.invalidValue = invalidValue;
    }

    @Override
    public String getVerboseMessage() {
        return String.format("Having granularity of %s is not valid. Granularity is currently set to: %s", invalidValue, validValue);
    }
}

