package com.arcadiadevs.viora.platform.shared.application.result;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.function.Function;

/**
 * Represents the result of a command or operation that can either succeed with a value or fail with an error.
 * This type encodes the possibility of failure in the type system, making error cases explicit and composable.
 *
 * @param <T> The type of the successful result value
 * @param <E> The type of the error/failure information
 */
@NullMarked
public sealed interface Result<T, E> {

    /**
     * Represents a successful result containing a value.
     *
     * @param value the successful value
     * @param <T>   the type of the successful result value
     * @param <E>   the type of the error information
     */
    record Success<T, E>(T value) implements Result<T, E> {
    }

    /**
     * Represents a failed result containing error information.
     *
     * @param error the error information
     * @param <T>   the type of the successful result value
     * @param <E>   the type of the error information
     */
    record Failure<T, E>(E error) implements Result<T, E> {
    }

    /**
     * Creates a successful result with the given value.
     *
     * @param value the success value
     * @param <T>   the type of the value
     * @param <E>   the type of the error
     * @return a Success result containing the value
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed result with the given error.
     *
     * @param error the error information
     * @param <T>   the type of the value
     * @param <E>   the type of the error
     * @return a Failure result containing the error
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * Returns true if this result is a success, false if it's a failure.
     *
     * @return {@code true} if this result represents a success, {@code false} otherwise
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Returns true if this result is a failure, false if it's a success.
     *
     * @return {@code true} if this result represents a failure, {@code false} otherwise
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Returns the success value when present.
     *
     * @return an {@link Optional} containing the success value if present, otherwise empty
     */
    default Optional<T> success() {
        return switch (this) {
            case Success<T, E> success -> Optional.of(success.value);
            case Failure<T, E> failure -> Optional.empty();
        };
    }

    /**
     * Returns the failure value when present.
     *
     * @return an {@link Optional} containing the error value if present, otherwise empty
     */
    default Optional<E> failure() {
        return switch (this) {
            case Success<T, E> success -> Optional.empty();
            case Failure<T, E> failure -> Optional.of(failure.error);
        };
    }

    /**
     * Returns an Optional containing the value if this is a success, otherwise an empty Optional.
     *
     * @return an {@link Optional} containing the value if successful, otherwise empty
     */
    default Optional<T> toOptional() {
        return success();
    }

    /**
     * Extracts the value if successful, or returns the given default if failed.
     *
     * @param defaultValue the fallback value to return if this is a failure
     * @return the success value if present, or {@code defaultValue} if a failure
     */
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T, E> s -> s.value;
            case Failure<T, E> f -> defaultValue;
        };
    }

    /**
     * Applies a function to the error if this is a failure, otherwise returns this unchanged.
     *
     * @param f   the error transformation function
     * @param <E2> the target error type
     * @return a new Result with the transformed error if failed, or the original success value
     */
    default <E2> Result<T, E2> mapError(Function<E, E2> f) {
        return switch (this) {
            case Success<T, E> s -> Result.success(s.value);
            case Failure<T, E> failure -> Result.failure(f.apply(failure.error));
        };
    }

    /**
     * Applies a function to the value if this is a success, producing a new Result.
     *
     * @param f   the function transforming the success value into a new Result
     * @param <T2> the success value type of the resulting Result
     * @return the result of applying {@code f} if successful, or a failure with the existing error
     */
    default <T2> Result<T2, E> flatMap(Function<T, Result<T2, E>> f) {
        return switch (this) {
            case Success<T, E> s -> f.apply(s.value);
            case Failure<T, E> failure -> Result.failure(failure.error);
        };
    }

    /**
     * Applies a function to the value if this is a success.
     *
     * @param f   the function transforming the success value
     * @param <T2> the target success value type
     * @return a new Result with the transformed value if successful, or the existing failure
     */
    default <T2> Result<T2, E> map(Function<T, T2> f) {
        return switch (this) {
            case Success<T, E> s -> Result.success(f.apply(s.value));
            case Failure<T, E> failure -> Result.failure(failure.error);
        };
    }

    /**
     * Transforms either branch into a single result type.
     *
     * @param onSuccess function to apply if this is a success
     * @param onFailure function to apply if this is a failure
     * @param <R>       the return type after folding
     * @return the unified transformed value
     */
    default <R> R fold(
            Function<T, R> onSuccess,
            Function<E, R> onFailure
    ) {
        return switch (this) {
            case Success<T, E> success -> onSuccess.apply(success.value);
            case Failure<T, E> failure -> onFailure.apply(failure.error);
        };
    }

    /**
     * Applies a function to the error if this is a failure.
     * Unlike mapError, this takes a Result, allowing fallback recovery.
     *
     * @param f the recovery function returning a fallback Result
     * @return this Result if successful, or the recovered Result if failed
     */
    default Result<T, E> recover(Function<E, Result<T, E>> f) {
        return switch (this) {
            case Success<T, E> s -> this;
            case Failure<T, E> failure -> f.apply(failure.error);
        };
    }
}
