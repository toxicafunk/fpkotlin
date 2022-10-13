package net.hybride.lazy

import net.hybride.Cons as LCons
import net.hybride.List
import net.hybride.Nil
import net.hybride.None
import net.hybride.Option
import net.hybride.Some
import net.hybride.getOrElse

import net.hybride.lazy.Stream.Companion.cons
import net.hybride.lazy.Stream.Companion.empty
import net.hybride.map

sealed class Stream<out A> {
    companion object {
        fun <A> cons(hd: () -> A, t1: () -> Stream<A>): Stream<A> {
            val head: A by lazy(hd)
            val tail: Stream<A> by lazy(t1)
            return Cons({ head }, { tail })
        }

        fun <A> empty(): Stream<A> = Empty

        fun <A> of(vararg xs: A): Stream<A> =
            if (xs.isEmpty()) {
                empty()
            } else {
                cons(
                    { xs[0] },
                    { of(*xs.sliceArray(1 until xs.size)) }
                )
            }
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
            is Cons -> loop(st.tail(), LCons(st.head(), acc))
        }

    return loop(this, Nil)
}

fun <A> Stream<A>.take(n: Int): Stream<A> {
    fun loop(st: Stream<A>, n: Int): Stream<A> =
        when (st) {
            is Empty -> empty()
            is Cons ->
                if (n == 0) empty()
                else cons(st.head) { loop(st.tail(), n - 1) }
        }

    return loop(this, n)
}

fun <A> Stream<A>.drop(n: Int): Stream<A> {
    tailrec fun loop(st: Stream<A>, counter: Int): Stream<A> =
        when (st) {
            is Empty -> empty()
            is Cons ->
                if (counter == 0) st
                else loop(st.tail(), counter - 1)
        }

    return loop(this, n)
}

fun <A> Stream<A>.takeWhile(p: (A) -> Boolean): Stream<A>  {
    fun loop(st: Stream<A>): Stream<A> =
        when (st) {
            is Empty -> empty()
            is Cons ->
                if (p(st.head())) cons(st.head) { loop(st.tail()) }
                else empty()
        }

    return loop(this)
}

fun <A> Stream<A>.takeWhile2(p: (A) -> Boolean): Stream<A> =
    when (this) {
        is Empty -> empty()
        is Cons ->
            if (p(this.head())) cons(this.head) { this.tail().takeWhile2(p) }
            else empty()
    }

fun <A> Stream<A>.exists(p: (A) -> Boolean): Boolean =
    when (this) {
        is Cons -> p(this.head()) || this.tail().exists(p)
        else -> false
    }

fun <A, B> Stream<A>.foldRight(
    z: () -> B,
    f: (A, () -> B) -> B
): B =
    when (this) {
        is Cons -> f(this.head()) { tail().foldRight(z, f) }
        is Empty -> z()
    }

// not stack-safe
fun <A> Stream<A>.exists2(p: (A) -> Boolean): Boolean =
    foldRight({ false }, { a, b -> p(a) || b() })

fun <A> Stream<A>.forAll(p: (A) -> Boolean): Boolean =
    foldRight({ true }, { a, b -> p(a) && b() })

fun <A> Stream<A>.takeWhileF(p: (A) -> Boolean): Stream<A> =
    foldRight({ empty() }) { a, acc -> if (p(a)) cons({ a }, acc) else acc() }

fun <A> Stream<A>.headOptionF(): Option<A> =
    foldRight({ Option.empty() }) { a, _ -> Some(a) }

fun <A, B> Stream<A>.map(f: (A) -> B): Stream<B> =
    foldRight({ empty() }) { a, acc -> cons({ f(a) }, acc) }

fun <A> Stream<A>.filter(p: (A) -> Boolean): Stream<A> =
    foldRight({ empty() }) { a, acc -> if (p(a)) cons({ a }, acc) else acc() }

fun <A> Stream<A>.append(st: Stream<A>): Stream<A> =
    foldRight({ st }) { a, acc -> cons({ a }, acc) }

fun <A, B> Stream<A>.flatMap(f: (A) -> Stream<B>): Stream<B> =
    foldRight({ empty() }) { a, acc -> f(a).append(acc()) }

fun <A> Stream<A>.find(p: (A) -> Boolean): Option<A> =
    filter(p).headOption()

fun ones(): Stream<Int> = cons({ 1 }) { ones() }

fun <A> constant(a: A): Stream<A> = cons({ a }) { constant(a) }

fun from(n: Int): Stream<Int> = cons({ n }) { from( n + 1) }

fun fibs(): Stream<Int> {
    fun loop(cur: Int, nxt: Int): Stream<Int> =
        cons({ cur }) { loop(nxt, cur + nxt) }

    return loop(0, 1)
}

fun <A, S> unfold(z: S, f: (S) -> Option<Pair<A, S>>): Stream<A> =
    f(z).map { pair -> cons({ pair.first }){ unfold(pair.second, f) } }
        .getOrElse { empty() }

fun onesU(): Stream<Int> = unfold(1) { Some( 1 to 1) }

fun <A> constantU(a: A): Stream<A> = unfold(a) { Some( a to a) }

fun fromU(n: Int): Stream<Int> = unfold(n) { Some(n to (n + 1)) }

fun fibsU(): Stream<Int> = unfold(0 to 1) { (curr, next) -> Some( curr to (next to (curr + next)))}

fun <A, B> Stream<A>.mapU(f: (A) -> B): Stream<B> = unfold(this) { st: Stream<A> -> when (st) {
    is Cons -> Some( f(st.head()) to st.tail())
    else -> None
} }

fun <A> Stream<A>.takeU(n: Int): Stream<A> =
    unfold(this) { st: Stream<A> ->
        when (st) {
            is Cons ->
                if (n > 0) Some(st.head() to st.tail().takeU(n - 1))
                else None
            else -> None
        }
    }

fun <A> Stream<A>.takeWhileU(p: (A) -> Boolean): Stream<A> =
    unfold(this) { st: Stream<A> ->
        when (st) {
            is Cons ->
                if (p(st.head())) Some(st.head() to st.tail())
                else None
            else -> None
        }
    }

fun <A, B, C> Stream<A>.zipWith(
    that: Stream<B>,
    f: (A, B) -> C
): Stream<C> {
    val stPair: Pair<Stream<A>, Stream<B>> = this to that
    return unfold(stPair) { (ths: Stream<A>, tht: Stream<B>) ->
        when (ths) {
            is Cons ->
                when (tht) {
                    is Cons ->
                        Some(
                            Pair(
                                f(ths.head(), tht.head()),
                                ths.tail() to tht.tail()
                            )
                        )
                    else -> None
                }
            else -> None
        }
    }
}

fun <A, B> Stream<A>.zipAll(
    that: Stream<B>
): Stream<Pair<Option<A>, Option<B>>> =
    unfold(this to that) { (ths: Stream<A>, tht: Stream<B>) ->
        when (ths) {
            is Cons ->
                when (tht) {
                    is Cons ->
                        Some(
                            Pair(
                                Some(ths.head()) to Some(tht.head()),
                                ths.tail() to tht.tail()
                            )
                        )
                    else ->
                        Some(
                            Pair(
                                Some(ths.head()) to None,
                                ths.tail() to empty()
                            )
                        )
                }
            else ->
                when (tht) {
                    is Cons ->
                        Some(
                            Pair(
                                None to Some(tht.head()),
                                empty<A>() to tht.tail()
                            )
                        )
                    else -> None
                }
        }
    }
