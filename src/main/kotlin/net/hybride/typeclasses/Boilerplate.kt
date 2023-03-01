package net.hybride.typeclasses

import arrow.Kind
import arrow.Kind2
import arrow.higherkind
import chapter8.RNG
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

//interface Kind<out F, out A>
//typealias Kind2<F, A, B> = arrow.Kind<arrow.Kind<F, A>, B>

/*class ForGen private constructor() { companion object }
typealias GenOf<A> = Kind<ForGen, A>
//typealias GenKindedJ<A> = io.kindedj.Hk<ForGen, A>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <A> GenOf<A>.fix(): Gen<A> =
    this as Gen<A>*/


@higherkind
data class Gen<A>(val sample: State<RNG, A>) : GenOf<A> {
    companion object {
        fun <A> unit(a: A): Gen<A> = Gen(State.unit(a))
        fun string(): Gen<String> = TODO()
        fun double(rng: IntRange): Gen<Double> = TODO()
        fun choose(start: Int, end: Int): Gen<Int> = TODO()
    }

    fun <B> flatMap(f: (A) -> Gen<B>): Gen<B> = TODO()
    fun <B> map(f: (A) -> B): Gen<B> = TODO()
}

/*class ForPar private constructor() { companion object }
typealias ParOf<A> = Kind<ForPar, A>
//typealias ParKindedJ<A> = io.kindedj.Hk<ForPar, A>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <A> ParOf<A>.fix(): Par<A> =
    this as Par<A>*/

@higherkind
class Par<A>(val run: (ExecutorService) -> Future<A>) : ParOf<A> {
    companion object {
        fun <A> unit(a: A): Par<A> = TODO()

        fun <A> lazyUnit(a: () -> A): Par<A> = TODO()
    }

    fun <B> flatMap(f: (A) -> Par<B>): Par<B> = TODO()
}

/*class ForOption private constructor() { companion object }
typealias OptionOf<A> = Kind<ForOption, A>
//typealias GenKindedJ<A> = io.kindedj.Hk<ForGen, A>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <A> OptionOf<A>.fix(): Option<A> =
    this as Option<A>*/

fun <A, B> Option<A>.map(f: (A) -> B): Option<B> =
    when (this) {
        is None -> None
        is Some -> Some(f(this.get))
    }

fun <A> Option<A>.getOrElse(default: () -> A): A =
    when (this) {
        is None -> default()
        is Some -> this.get
    }

@higherkind
sealed class Option<out A> : OptionOf<A> {
    companion object {
        fun <A> unit(a: A): Option<A> = Some(a)
    }

    fun <B> flatMap(f: (A) -> Option<B>): Option<B> =
        this.map(f).getOrElse { None }

}

data class Some<out A>(val get: A) : Option<A>()
object None : Option<Nothing>()

/*class ForList private constructor() { companion object }
typealias ListOf<A> = Kind<ForList, A>
//typealias GenKindedJ<A> = io.kindedj.Hk<ForGen, A>
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <A> ListOf<A>.fix(): List<A> =
    this as List<A>*/

object Nil : List<Nothing>()
data class Cons<out A>(val head: A, val tail: List<A>) : List<A>()

@higherkind
sealed class List<out A> : ListOf<A> {
    companion object {
        fun <A> of(vararg aa: A): List<A> {
            val tail = aa.sliceArray(1 until aa.size)
            return if (aa.isEmpty()) Nil else Cons(aa.first(), of(*tail))
        }

        fun <A> unit(a: A): List<A> = TODO()

        fun <A> lazyUnit(a: () -> A): List<A> = TODO()

        fun <A> empty(): List<A> = Nil as List<A>

        fun <A> fill(n: Int, a: A): List<A> = TODO()

        fun <A> append(a1: List<A>, a2: List<A>): List<A> =
            when (a1) {
                is Nil -> a2
                is Cons -> Cons(a1.head, append(a1.tail, a2))
            }
    }

    fun <B> map(f: (A) -> B): List<B> =
        when (this) {
            is Nil -> this
            is Cons -> Cons(f(this.head), this.tail.map(f))
        }

    fun <B> flatMap(f: (A) -> List<B>): List<B> = TODO()

    //fun <F, A1> foldRight(unit: Any, function: (A, Kind<F, List<A1>>) -> Kind<F, List<A1>>): Kind<F, List<A1>> =
    fun <B> foldRight(z: B, f: (A, B) -> B): B =
        when (this) {
            is Nil -> z
            is Cons -> f(this.head, this.tail.foldRight(z, f))
        }

}

class ForState private constructor() { companion object }
typealias StateOf<S, A> = Kind2<ForState, S, A>
typealias StatePartialOf<S> = Kind<ForState, S>
inline fun <S, A> StateOf<S, A>.fix(): State<S, A> =
    this as State<S, A>

data class State<S, out A>(val run: (S) -> Pair<A, S>) : StateOf<S, A> {

    companion object {
        fun <S, A> unit(a: A): State<S, A> =
            State { s: S -> a to s }

        fun <S> get(): State<S, S> =
            State { s -> s to s }

        fun <S> set(s: S): State<S, Unit> =
            State { Unit to s }
    }

    fun <B> map(f: (A) -> B): State<S, B> =
        flatMap { a: A -> unit<S, B>(f(a)) }

    fun <B> flatMap(f: (A) -> State<S, B>): State<S, B> =
        State { s1: S ->
            val (a: A, s2: S) = this.run(s1)
            f(a).run(s2)
        }
}

class ForEither private constructor() { companion object }
typealias EitherOf<E, A> = Kind2<ForEither, E, A>
typealias EitherPartialOf<E> = Kind<ForEither, E>
inline fun <E, A> EitherOf<E, A>.fix(): Either<E, A> =
    this as Either<E, A>

sealed class Either<out E, out A> : EitherOf<E, A> {
    companion object {
        fun <E, A> unit(a: A): Either<E, A> = Right(a)
    }

    fun <B> flatMap(f: (A) -> Either<@UnsafeVariance E, B>): Either<E, B> =
        when (this) {
            is Left -> this
            is Right -> f(this.value)
        }

    fun <B> map(f: (A) -> B): Either<E, B> =
        flatMap { a -> unit<E,B>(f(a)) }
}
data class Left<out E>(val value: E) : Either<E, Nothing>()
data class Right<out A>(val value: A) : Either<Nothing, A>()