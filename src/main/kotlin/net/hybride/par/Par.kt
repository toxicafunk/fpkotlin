package net.hybride.par

import net.hybride.None
import net.hybride.Option
import net.hybride.Some
import net.hybride.getOrElse

fun <A> List<A>.firstOption(): Option<A> =
    this.firstOrNull()?.let { Some(it) } ?: None

fun <A> List<A>.splitAt(n: Int): Pair<List<A>, List<A>> =
    this.foldIndexed(mutableListOf<A>() to mutableListOf<A>()) { index, acc, a ->
        if (index <= n) acc.first.add(a) else acc.second.add(a)
        acc
    }

fun sum(ints: List<Int>): Int =
    if (ints.size <= 1) ints.firstOption().getOrElse { 0 }
    else {
        val (l, r) = ints.splitAt(ints.size / 2)
        sum(l) + sum(r)
    }

class Par<A>(val get: A) {
    companion object {
        fun <A, B, C> map2(pl: Par<A>, pr: Par<B>, f: (l: A, r: B) -> C): Par<C> =
            Par(f(pl.get, pr.get))
        fun <A> fork(f: () -> Par<A>): Par<A> = f()
        fun <A> unit(a: A): Par<A> = Par(a)
        fun <A> lazyUnit(a: () -> A): Par<A> =
            fork { unit(a()) }
        fun <A> run(a: Par<A>): A = a.get
    }
}

fun <A> unit(a: () -> A): Par<A> = Par(a())

fun <A> get(a: Par<A>): A = a.get

fun sumP(ints: List<Int>): Int =
    if (ints.size <= 1) ints.firstOption().getOrElse { 0 }
    else {
        val (l, r) = ints.splitAt(ints.size / 2)
        val sumL: Par<Int> = unit { sumP(l) }
        val sumR: Par<Int> = unit { sumP(r) }
        sumL.get + sumR.get
    }

fun sum2(ints: List<Int>): Par<Int> =
    if (ints.size <= 1) unit { ints.firstOption().getOrElse { 0 } }
    else {
        val (l, r) = ints.splitAt(ints.size / 2)
        Par.map2(sum2(l), sum2(r)) { lx: Int, rx: Int -> lx + rx }
    }

fun sumF(ints: List<Int>): Par<Int> =
    if (ints.size <= 1) unit { ints.firstOption().getOrElse { 0 } }
    else {
        val (l, r) = ints.splitAt(ints.size / 2)
        Par.map2(
            Par.fork { sumF(l) },
            Par.fork { sumF(r) }
        ) { lx: Int, rx: Int -> lx + rx }
    }
