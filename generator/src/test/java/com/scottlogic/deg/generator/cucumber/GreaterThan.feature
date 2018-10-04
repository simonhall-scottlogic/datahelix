Feature: User can specify that a numeric value is higher than, but not equal to, a specified threshold
    
Scenario: User requires to create a numeric field with data values that are greater than zero
     Given there is a field foo
       And foo is greaterThan 0
       And foo is lessThan 10
       And foo is granularTo 1
     When I ask to generate all values for field foo
     Then the following data should be generated:
       | foo |
       | 1   |
       | 2   |
       | 3   |
       | 4   |
       | 5   |
       | 6   |
       | 7   |
       | 8   |
       | 9   |

Scenario: User requires to create a field with decimal values that are greater than zero, specified as an interger
     Given that there is a field foo
       And foo is granularTo 0.1
       And foo is greaterThan 0
       And foo is lessThan 2
     When I ask to generate all values for field foo
     Then the following data should be generated:
       | foo |
       | 0.1 |
       | 0.2 |
       | 0.3 |
       | 0.4 |
       | 0.5 |
       | 0.6 |
       | 0.7 |
       | 0.8 |
       | 0.9 |
       | 1   |
       | 1.1 |
       | 1.2 |
       | 1.3 |
       | 1.4 |
       | 1.5 |
       | 1.6 |
       | 1.7 |
       | 1.8 |
       | 1.9 |

Scenario: User requires to create a field with decimal values that are greater than zero, specifed as a decimal
     Given that there is a field foo
       And foo is granularTo 0.1
       And foo is greaterThan 0.0
       And foo is lessThan 2.0
     When I ask to generate all values for field foo
     Then the following data should be generated:
       | foo |
       | 0.1 |
       | 0.2 |
       | 0.3 |
       | 0.4 |
       | 0.5 |
       | 0.6 |
       | 0.7 |
       | 0.8 |
       | 0.9 |
       | 1   |
       | 1.1 |
       | 1.2 |
       | 1.3 |
       | 1.4 |
       | 1.5 |
       | 1.6 |
       | 1.7 |
       | 1.8 |
       | 1.9 |

Scenario: User requires to create a numeric field with data values that are greater than a negative number
     Given there is a field foo
       And foo is greaterThan -10
       And foo is lessThan 0
       And foo is granularTo 1
     When I ask to generate all values for field foo
     Then the following data should be generated:
       | foo |
       | -9  |
       | -8  |
       | -7  |
       | -6  |
       | -5  |
       | -4  |
       | -3  |
       | -2  |
       | -1  |

Scenario: User requires to create a numeric field with data values that are greater than zero and greater than one
     Given there is a field foo
       And foo is greaterThan 0
       And foo is greaterThan 1
       And foo is lessThan 10
       And foo is granularTo 1
     When I ask to generate all values for field foo
     Then the following data should be generated:
       | foo |
       | 2   |
       | 3   |
       | 4   |
       | 5   |
       | 6   |
       | 7   |
       | 8   |
       | 9   |

Scenario: User attempts to create a numeric field with data value that are greater than zero using an incorrect field value type of string
     Given there is a field foo
       And foo is greaterThan "Zero"
       And foo is not null
     When I ask to generate all values for field foo
     Then I am presented with an error message
        And no data is created