package net.hybride.par

import arrow.core.split
import net.hybride.getOrElse
import net.hybride.None
import net.hybride.Option
import net.hybride.Some

fun <A> List<A>.firstOption(): Option<A> =
    this.firstOrNull()?.let { Some(it) } ?: None

fun <A> List<A>.splitAt(n: Int): Pair<List<A>, List<A>> =
    this.foldIndexed(mutableListOf<A>() to mutableListOf<A>()) { index, acc, a ->
        if (index <= n) acc.first.add(a)
        else acc.second.add(a)
        return acc
    }

fun sum(ints: List<Int>): Int =
    if (ints.size <= 1) ints.firstOption().getOrElse { 0 }
    else {
        val (l, r) = ints.splitAt(ints.size / 2)
        sum(l) + sum(r)
    }

class Par<A>(val get: A)

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
