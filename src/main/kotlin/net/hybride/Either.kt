package net.hybride

sealed class Either<out E, out A>
data class Left<out E>(val value: E) : Either<E, Nothing>()
data class Right<out A>(val value: A) : Either<Nothing, A>()

fun mean(xs: List<Double>): Either<String, Double> =
    if (xs.isEmpty())
        Left("mean of empty list!")
    else Right(xs.sum() / xs.size())

fun safeDiv(x: Int, y: Int): Either<Exception, Int> =
    try {
        Right(x / y)
    } catch (e: Exception) {
        Left(e)
    }

fun <A> catches(a: () -> A): Either<Exception, A> =
    try {
        Right(a())
    } catch (e: Exception) {
        Left(e)
    }

fun <E, A, B> Either<E, A>.map(f: (A) -> B): Either<E, B> =
    when (this) {
        is Right -> Right(f(this.value))
        is Left -> this
    }

fun <E, A, B> Either<E, A>.flatMap(f: (A) -> Either<E, B>): Either<E, B> =
    when (this) {
        is Left -> this
        is Right -> f(this.value)
    }

fun <E, A> Either<E, A>.orElse(f: () -> Either<E, A>): Either<E, A> =
    when (this) {
        is Left -> f()
        is Right -> this
    }

fun <E, A, B, C> map2(
    ae: Either<E, A>,
    be: Either<E, B>,
    f: (A, B) -> C
): Either<E, C> =
    ae .flatMap { a ->
        be.map { b ->
            f(a, b)
        }
    }

fun main() {
    val err: Either<Exception, String> = Left(RuntimeException("this string is not a String"))
    println(Right("one").map { s -> s.length })
    println( err.map { s -> s.length })
    println(Right("one").flatMap { s -> Right(s.length) })
    println( err.flatMap { s -> Left(RuntimeException("this is another exception")) })
    println( Right("one").flatMap { s -> Left(RuntimeException("this is another exception")) })
}