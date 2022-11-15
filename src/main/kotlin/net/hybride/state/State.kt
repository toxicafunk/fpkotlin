package net.hybride.state

import net.hybride.Cons
import net.hybride.List
import net.hybride.List.Companion.foldRight

fun <S, A, B> map(
    sa: (S) -> Pair<A, S>,
    f: (A) -> B
): (S) -> Pair<B, S> = { s ->
    val (a, s1) = sa(s)
    f(a) to s1
}

// typealias State<S, A> = (S) -> Pair<A, S>

data class State<S, out A>(val run: (S) -> Pair<A, S>) {
    companion object {
        fun <S, A> unit(a: A): State<S, A> =
            State { s -> a to s }

        fun <S, A, B, C> map2(sa: State<S, A>, sb: State<S, B>, f: (A, B) -> C): State<S, C> =
            sa.flatMap { a ->
                sb.map { b ->
                    f(a, b)
                }
            }

        fun <S, A> sequence(fs: List<State<S, A>>): State<S, List<A>> =
            foldRight(fs, unit(List.empty<A>())) { f, acc ->
                map2(f, acc) { h, t -> Cons(h, t) }
            }
    }

    fun <B> flatMap(f: (A) -> State<S, B>): State<S, B> =
        State { s ->
            val (a, s1) = this.run(s)
            f(a).run(s1)
        }

    fun <B> map(f: (A) -> B): State<S, B> =
        flatMap { a -> unit(f(a)) }
}

fun <A> id(a: A): A = a
// typealias Rand<A> = State<RNG, A>
