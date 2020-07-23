package io.plotnik.piserver.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpResult {

    boolean success;
    String message;

    public OpResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public OpResult(boolean success) {
        this.success = success;
        this.message = null;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

}