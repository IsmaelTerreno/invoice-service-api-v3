package com.remotejob.invoiceservice.dto;

import lombok.*;

/**
 * A generic class for API responses.
 *
 * @param <T> The type of the data contained in the response.
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseAPI<T> {
    private String status;
    private String message;
    private T data;
}
