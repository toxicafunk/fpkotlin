package net.hybride.typeclasses

import arrow.Kind
import arrow.Kind2
import arrow.higherkind
import chapter8.RNG
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

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

@higherkind
sealed class Option<out A> : OptionOf<A> {
    companion object {
        fun <A> unit(a: A): Option<A> = TODO()

        fun <A> lazyUnit(a: () -> A): Option<A> = TODO()
    }

    fun <B> flatMap(f: (A) -> Option<B>): Option<B> = TODO()
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

    fun <B> flatMap(f: (A) -> List<B>): List<B> = TODO()

    fun <F, A1> foldRight(unit: Any, function: (A, Kind<F, List<A1>>) -> Kind<F, List<A1>>): Kind<F, List<A1>> =
        TODO()

}

class ForState private constructor() {
    companion object
}
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