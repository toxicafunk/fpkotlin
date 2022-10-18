package net.hybride.rng

import net.hybride.Cons
import net.hybride.List
import net.hybride.List.Companion.foldRight
import net.hybride.Nil

interface RNG {
    fun nextInt(): Pair<Int, RNG>
}

data class SimpleRNG(val seed: Long) : RNG {
    override fun nextInt(): Pair<Int, RNG> {
        val newSeed = (seed * 0x5DEECE66DL + 0xBL) and 0xFFFFFFFFFFFFL
        val nextRNG = SimpleRNG(newSeed)
        val n = (newSeed ushr 16).toInt()
        return n to nextRNG
    }
}

fun nonNegativeInt(rng: RNG): Pair<Int, RNG> {
    val (rNum, rng1) = rng.nextInt()
    return if (rNum < 0) -(rNum + 1) to rng1 else rNum to rng1
}

fun double(rng: RNG): Pair<Double, RNG> {
    val (rInt, rng1) = nonNegativeInt(rng)
    return rInt / Int.MAX_VALUE.toDouble() to rng1
}

fun intDouble(rng: RNG): Pair<Pair<Int, Double>, RNG> {
    val (rInt, rng1) = rng.nextInt()
    val (rDbl, rng2) = double(rng1)
    return (rInt to rDbl) to rng2
}

fun doubleInt(rng: RNG): Pair<Pair<Double, Int>, RNG> {
    val (id, rng1) = intDouble(rng)
    val (rInt, rDbl) = id
    return (rDbl to rInt) to rng1
}

fun double3(rng: RNG): Pair<Triple<Double, Double, Double>, RNG> {
    val (d1, rng1) = double(rng)
    val (d2, rng2) = double(rng1)
    val (d3, rng3) = double(rng2)
    return Triple(d1, d2, d3) to rng3
}

fun ints(count: Int, rng: RNG): Pair<List<Int>, RNG> {
    val pInts = if (count > 0) {
        val (i, r1) = rng.nextInt()
        val (xs, r2) = ints(count - 1, r1)
        Cons(i, xs) to r2
    } else {
        Nil to rng
    }

    return pInts
}

typealias Rand<A> = (RNG) -> Pair<A, RNG>

val intR: Rand<Int> = { rng -> rng.nextInt() }

fun <A> unit(a: A): Rand<A> = { rng -> a to rng }

fun <A, B> map(s: Rand<A>, f: (A) -> B): Rand<B> = { rng ->
    val (a, rng1) = s(rng)
    f(a) to rng1
}

fun doubleR(): Rand<Double> =
    map(::nonNegativeInt) { i ->
        i / Int.MAX_VALUE.toDouble() + 1
    }

fun <A, B, C> map2(ra: Rand<A>, rb: Rand<B>, f: (A, B) -> C): Rand<C> = { rng ->
    val (a, ra1) = ra(rng)
    val (b, rb1) = rb(ra1)
    f(a, b) to rb1
}

fun <A, B> both(ra: Rand<A>, rb: Rand<B>): Rand<Pair<A, B>> =
    map2(ra, rb) { a, b -> a to b }

val intDoubleR: Rand<Pair<Int, Double>> = both(intR, doubleR())
val doubleIntR: Rand<Pair<Double, Int>> = both(doubleR(), intR)

// fun <A> sequence(fs: List<Rand<A>>): (RNG) -> Pair<List<A>, RNG> = { rng ->
fun <A> sequence(fs: List<Rand<A>>): Rand<List<A>> = { rng ->
    when (fs) {
        is Nil -> unit(List.empty<A>())(rng)
        is Cons -> {
            val (a, rng1) = fs.head(rng)
            val (xs, rng2) = sequence(fs.tail)(rng1)
            Cons(a, xs) to rng2
        }
    }
}

fun <A> sequenceF(fs: List<Rand<A>>): Rand<List<A>> =
    foldRight(fs, unit(List.empty())) { f, acc ->
        map2(f, acc) { h, t -> Cons(h, t) }
    }

fun intsS(count: Int, rng: RNG): Pair<List<Int>, RNG> {
    fun loop(c: Int): List<Rand<Int>> =
        if (c == 0) Nil else Cons({ rng1 -> 1 to rng1 }, loop(c - 1))

    return sequenceF(loop(count))(rng)
}

fun <A, B> flatMap(f: Rand<A>, g: (A) -> Rand<B>): Rand<B> = { rng ->
    val (a, rng1) = f(rng)
    g(a)(rng1)
}

fun nonNegativeIntLessThan(n: Int): Rand<Int> =
    flatMap(::nonNegativeInt) { i ->
        val mod = i % n
        if (i + (n - 1) - mod >= 0) unit(mod) else nonNegativeIntLessThan(n)
    }

fun <A, B> mapF(s: Rand<A>, f: (A) -> B): Rand<B> = flatMap(s) { a -> unit(f(a)) }

fun <A, B, C> map2F(ra: Rand<A>, rb: Rand<B>, f: (A, B) -> C): Rand<C> =
    flatMap(ra) { a ->
        mapF(rb) { b ->
            f(a, b)
        }
    }

fun rollDie(): Rand<Int> = nonNegativeIntLessThan(6)

fun rollDieFix(): Rand<Int> = map(nonNegativeIntLessThan(6)) { it + 1 }
