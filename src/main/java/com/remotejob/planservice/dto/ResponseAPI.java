package com.remotejob.planservice.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * A generic class for API responses.
 *
 * @param <T> The type of the data contained in the response.
 */
@Setter
@Getter
public class ResponseAPI<T> {
    private String message;
    private T data;

    public ResponseAPI() {
    }

    public ResponseAPI(String message, T data) {
        this.message = message;
        this.data = data;
    }

}
