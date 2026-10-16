package com.brillio.mls.search.exception;

import java.util.List;

public class ListingNotFoundException extends RuntimeException
{
    private final List<String> availableOptions;

    public ListingNotFoundException(String message, List<String> availableOptions)
    {
        super(message);
        this.availableOptions = List.copyOf(availableOptions);
    }

    public List<String> getAvailableOptions()
    {
        return availableOptions;
    }
}
