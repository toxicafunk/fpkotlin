package net.hybride.typeclasses

interface EitherMonad<E> : Monad<EitherPartialOf<E>> {
    override fun <A> unit(a: A): EitherOf<E, A> =
        Either.unit(a)

    override fun <A, B> flatMap(
        fa: EitherOf<E,  A>,
        f: (A) -> EitherOf<E, B>
    ): EitherOf<E, B> =
        fa.fix().flatMap { a -> f(a).fix() }

}

val eitherMonad: EitherMonad<String> = object : EitherMonad<String> {
    override fun <A> unit(a: A): EitherOf<String, A> =
        Either.unit(a)

    override fun <A, B> flatMap(
        fa: EitherOf<String, A>,
        f: (A) -> EitherOf<String, B>
    ): EitherOf<String,B> =
        fa.fix().flatMap { a -> f(a).fix() }
}

sealed class MyError<out A>(message: String)
data class ValidationError<out A>(val message: String, val field: A): MyError<A>(message)
data class IrrecoverableError<A>(val message: String): MyError<A>(message)


object SomeService {
    fun calculateLength(word: String): Int = word.length

}

fun main() {
    val words = List.of("The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
    words.map { word -> SomeService.calculateLength(word) }
}