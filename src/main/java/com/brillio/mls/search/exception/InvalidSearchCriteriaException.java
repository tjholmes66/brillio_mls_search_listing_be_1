package com.brillio.mls.search.exception;

import java.util.List;

public class InvalidSearchCriteriaException extends RuntimeException
{
    private final List<String> errors;

    public InvalidSearchCriteriaException(List<String> errors)
    {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors()
    {
        return errors;
    }
}
