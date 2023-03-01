package net.hybride.typeclasses

import kotlin.random.Random

interface EitherMonad<E> : Monad<EitherPartialOf<E>> {
    override fun <A> unit(a: A): EitherOf<E, A> =
        Either.unit(a)

    override fun <A, B> flatMap(
        fa: EitherOf<E,  A>,
        f: (A) -> EitherOf<E, B>
    ): EitherOf<E, B> =
        fa.fix().flatMap { a -> f(a).fix() }

    fun <A, B> bimap(fa: EitherOf<E, A>, f: (E) -> B, g: (A) -> B): EitherOf<E, B> {
        return when (val eit = fa.fix()) {
            is Left -> eit.map(f)
            is Right -> eit.map(g)
        }
    }
}

val eitherMonadString: EitherMonad<String> = object : EitherMonad<String> {
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

typealias MyIntError = MyError<Int>

val eitherMonadMyError: EitherMonad<MyError<Int>> = object : EitherMonad<MyIntError> {
    override fun <A> unit(a: A): EitherOf<MyIntError, A> =
        Either.unit(a)

    override fun <A, B> flatMap(
        fa: EitherOf<MyIntError, A>,
        f: (A) -> EitherOf<MyIntError, B>
    ): EitherOf<MyIntError,B> =
        fa.fix().flatMap { a -> f(a).fix() }
}


object SomeService {
    val rnd = Random(123)
    
    fun calculateLength(word: String): Int = word.length

    fun  callMayFail(max: Int): Either<MyError<Int>, Int> {
        val result = rnd.nextInt(max)
        val threshold = max / 2
        return if (result <= threshold) Left(ValidationError("Result is not big enough", result))
        else Right(result)
    }

}

fun main() {
    val words = List.of("The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
    val listInt: List<Int> = words.map { word -> SomeService.calculateLength(word) }

    val listEither1: List<Either<MyError<Int>, Int>> = listInt.map { i -> SomeService.callMayFail(i) }
    println(listEither1)
    val res = eitherMonadMyError.sequence(listEither1)
    println(res)

    val listEither2 = eitherMonadMyError.traverse(listInt) { i -> SomeService.callMayFail(i) }
    println(listEither2)

    /*eitherMonadMyError.bimap( { e ->
        when (e) {
            is ValidationError -> e.message
            else -> "This shouldn't happen"
        }
    },
        { a -> }
    )*/

}