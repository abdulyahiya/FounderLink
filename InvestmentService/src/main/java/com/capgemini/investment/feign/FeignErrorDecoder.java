package com.capgemini.investment.feign;

import com.capgemini.investment.exception.ResourceNotFoundException;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new ResourceNotFoundException("Requested resource not found from remote service");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
