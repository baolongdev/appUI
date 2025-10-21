// utils/Either.kt
package com.example.appui.utils

/**
 * Represents a value of one of two possible types (a disjoint union).
 * Either is used for error handling, where Left represents failure and Right represents success.
 */
sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    val isLeft: Boolean get() = this is Left
    val isRight: Boolean get() = this is Right

    companion object {
        fun <L> left(value: L) = Left(value)
        fun <R> right(value: R) = Right(value)
    }
}

// Extension functions
inline fun <L, R, T> Either<L, R>.fold(
    crossinline onLeft: (L) -> T,
    crossinline onRight: (R) -> T
): T = when (this) {
    is Either.Left -> onLeft(value)
    is Either.Right -> onRight(value)
}

inline fun <L, R, T> Either<L, R>.map(
    crossinline transform: (R) -> T
): Either<L, T> = when (this) {
    is Either.Left -> this
    is Either.Right -> Either.Right(transform(value))
}

inline fun <L, R, T> Either<L, R>.flatMap(
    crossinline transform: (R) -> Either<L, T>
): Either<L, T> = when (this) {
    is Either.Left -> this
    is Either.Right -> transform(value)
}

fun <L, R> Either<L, R>.getOrNull(): R? = when (this) {
    is Either.Left -> null
    is Either.Right -> value
}

fun <L, R> Either<L, R>.leftOrNull(): L? = when (this) {
    is Either.Left -> value
    is Either.Right -> null
}

fun <L, R> Either<L, R>.getOrElse(default: R): R = when (this) {
    is Either.Left -> default
    is Either.Right -> value
}

inline fun <L, R> Either<L, R>.getOrElse(crossinline default: () -> R): R = when (this) {
    is Either.Left -> default()
    is Either.Right -> value
}

inline fun <L, R> Either<L, R>.onLeft(crossinline action: (L) -> Unit): Either<L, R> {
    if (this is Either.Left) action(value)
    return this
}

inline fun <L, R> Either<L, R>.onRight(crossinline action: (R) -> Unit): Either<L, R> {
    if (this is Either.Right) action(value)
    return this
}

// Utility function for try-catch
inline fun <R> runCatching(block: () -> R): Either<Throwable, R> {
    return try {
        Either.Right(block())
    } catch (e: Throwable) {
        Either.Left(e)
    }
}

// Utility for catching specific exceptions
inline fun <reified E : Throwable, R> runCatchingEither(block: () -> R): Either<E, R> {
    return try {
        Either.Right(block())
    } catch (e: Throwable) {
        if (e is E) {
            Either.Left(e)
        } else {
            throw e
        }
    }
}
