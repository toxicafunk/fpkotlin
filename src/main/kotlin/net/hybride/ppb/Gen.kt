package net.hybride.ppb

import net.hybride.Either
import net.hybride.List
import net.hybride.rng.nonNegativeInt
import net.hybride.rng.nextBoolean
import net.hybride.rng.RNG
import net.hybride.rng.SimpleRNG
import net.hybride.rng.double
import net.hybride.rng.intR
import net.hybride.state.State

fun <A> listOf(a: Gen<A>): List<Gen<A>> = TODO()

fun <A> listOfN(n: Int, a: Gen<A>): List<Gen<A>> = TODO()

fun <A> forAll(a: Gen<A>, pred: (A) -> Boolean): Prop = TODO()

typealias SuccessCount = Int
typealias FailedCase = String

interface Prop {
    // fun check(): Unit unit throws away info
    fun check(): Boolean

    fun and(p: Prop): Prop =
        object : Prop {
            val checked: Either<Pair<FailedCase, SuccessCount>, SuccessCount> = TODO() // this.check() && p.check()
            override fun check(): Boolean = TODO() // checked
        }
}

data class Gen<A>(val sample: State<RNG, A>) {
    companion object {
        fun <A> listOfN(n: Int, ga: Gen<A>): Gen<List<A>> =
            Gen(State.sequence(List.fill(n) { ga.sample }))
    }
    fun choose(start: Int, stopExclusive: Int): Gen<Int> =
        Gen(
            State { rng: RNG -> nonNegativeInt(rng) }
                .map { start + (it % (stopExclusive - start)) }
        )

    fun chooseUnbiased(start: Int, stopExclusive: Int): Gen<Int> =
        Gen(
            State { rng: RNG -> double(rng) }
                .map { start + (it * (stopExclusive - start)) }
                .map { it.toInt() }
        )

    fun <A> unit(a: A): Gen<A> = Gen( State.unit(a) )

    fun boolean(): Gen<Boolean> =
        Gen(State { rng -> nextBoolean(rng) })


}

fun main() {
    val rng = SimpleRNG(404)
    val gen = Gen(State(intR))
    println(Gen.listOfN(5, gen).sample.run(rng).first)
}