package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unified API response wrapper.
 */
@Schema(description = "Unified API response")
public class ApiResponse<T> {

    @Schema(description = "HTTP status code", example = "200")
    private int code;

    @Schema(description = "Message", example = "success")
    private String message;

    @Schema(description = "Response payload")
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
