package net.hybride.ppb

import net.hybride.Either
import net.hybride.List
import net.hybride.None
import net.hybride.Option
import net.hybride.Some
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

        fun <A> liftOption(ga: Gen<A>): Gen<Option<A>> =
            ga.map { a -> if (a == null) None else Some(a) }

        //fun <A> getOption(goa: Gen<Option<A>>): Gen<A> =
        //    goa.flatMap { oa -> oa. }
    }

    fun <B> flatMap(f: (A) -> Gen<B>): Gen<B> =
        Gen(this.sample.flatMap { a -> f(a).sample })

    fun <B> map(f: (A) -> B): Gen<B> =
        flatMap { a -> unit(f(a)) }

    fun choose(start: Int, stopExclusive: Int): Gen<Int> =
        Gen(
            State { rng: RNG -> nonNegativeInt(rng) }
                .map { start + (it % (stopExclusive - start)) }
        )

    fun choosePair(start: Int, stopExclusive: Int): Gen<Pair<Int, Int>> =
        choose(start, stopExclusive).flatMap { i1 ->
            choose(start, stopExclusive).map { i2 -> i1 to i2 }
        }

    fun chooseUnbiased(start: Int, stopExclusive: Int): Gen<Int> =
        Gen(
            State { rng: RNG -> double(rng) }
                .map { start + (it * (stopExclusive - start)) }
                .map { it.toInt() }
        )

    fun chooseChar(): Gen<Char> = choose(32, 123).map { it.toChar() }

    fun chooseString(n: Int): Gen<String> = listOfN(n, chooseChar()).map { it. }

    fun <A> unit(a: A): Gen<A> = Gen( State.unit(a) )

    fun boolean(): Gen<Boolean> =
        Gen(State { rng -> nextBoolean(rng) })

    fun execute(rng: RNG): Pair<A, RNG> = this.sample.run(rng)

}

fun main() {
    val rng = SimpleRNG(404)
    val gen = Gen(State(intR))
    println(Gen.listOfN(5, gen).execute(rng).first)
    println(gen.choose(10, 15).execute(rng).first)
    println(gen.choosePair(20, 25).execute(rng).first)
    println(gen.chooseChar().execute(rng).first)
    println(gen.chooseString(6).execute(rng).first)
}