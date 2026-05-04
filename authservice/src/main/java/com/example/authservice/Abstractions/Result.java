package com.example.authservice.Abstractions;

public class Result<T> {

    private final boolean isSuccess;
    private final Error error;
    private T value;

    private Result(boolean isSuccess, T value, Error error) {
        if ((isSuccess && error != Error.NONE) || (!isSuccess && error == Error.NONE)) {
            throw new IllegalArgumentException("A result cannot be successful and contain an error");
        }

        this.isSuccess = isSuccess;
        this.error = error;
        this.value = value;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean isFailure() {
        return !isSuccess;
    }

    public Error getError() {
        return error;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(true, value, Error.NONE);
    }

    public static <T> Result<T> failure(Error error) {
        return new Result<>(false, null, error);
    }
}