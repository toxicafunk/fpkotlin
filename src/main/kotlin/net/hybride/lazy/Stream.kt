package net.hybride.lazy

import net.hybride.List
import net.hybride.Cons as LCons
import net.hybride.Nil
import net.hybride.None
import net.hybride.Option
import net.hybride.Some

sealed class Stream<out A> {
    companion object {
        fun <A> cons(hd: () -> A, t1: () -> Stream<A>): Stream<A> {
            val head: A by lazy(hd)
            val tail: Stream<A> by lazy(t1)
            return Cons({ head }, { tail })
        }

        fun <A> empty(): Stream<A> = Empty

        fun <A> of(vararg xs: A): Stream<A> =
            if (xs.isEmpty()) empty()
            else cons(
                { xs[0] },
                { of(*xs.sliceArray(1 until xs.size)) }
            )
    }
}

data class Cons<out A>(
    val head: () -> A,
    val tail: () -> Stream<A>
) : Stream<A>()

object Empty : Stream<Nothing>()

fun <A> Stream<A>.headOption(): Option<A> =
    when (this) {
        is Empty -> None
        is Cons -> Some(head())
    }

fun <A> Stream<A>.toList(): List<A> {
    fun loop(st: Stream<A>, acc: List<A>): List<A> =
        when (st) {
            is Empty -> List.reverse(acc)
            is Cons -> LCons(st.head(), st.tail().toList())
        }

    return loop(this, Nil)
}

fun <A> Stream<A>.drop(n: Int): Stream<A> =
    if (n == 0) this
    else when (this) {
        is Empty -> this
        is Cons -> tail().drop(n - 1)
    }

fun <A> Stream<A>.take(n: Int): Stream<A> {
    fun loop(st: Stream<A>, acc: Stream<A>, counter: Int): Stream<A> =
        if (counter == n) acc
        else when (this) {
            is Empty -> acc
            is Cons -> loop(tail(), Stream.cons(head, () -> acc), counter + 1)
        }
}