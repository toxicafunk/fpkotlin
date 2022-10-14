package net.hybride.rng

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

fun ints(count: Int, rng: RNG): Pair<List<Int>, RNG> =
    if (counts > 0) {
        val (i, r1) = rng.nextInt()
        val (xs, r2) = ints(counts - 1)
        Cons(im, xs) to r2
    } else Nil to rng
