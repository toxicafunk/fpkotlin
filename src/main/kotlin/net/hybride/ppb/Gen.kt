package net.hybride.ppb

import arrow.core.getOrElse
import arrow.core.toOption
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
import kotlin.math.absoluteValue
import kotlin.math.min

fun <A> listOf(a: Gen<A>): List<Gen<A>> = TODO()

fun <A> listOfN(n: Int, a: Gen<A>): List<Gen<A>> = TODO()

/*fun <A> forAll(ga: Gen<A>, pred: (A) -> Boolean): Prop =
    Prop { m: MaxSize, n: TestCases, rng: RNG ->
        randomSequence(ga, rng)
            .mapIndexed { i, a ->
                try {
                    if (pred(a)) Passed
                    else Falsified(a.toString(), i)
                } catch (e: Exception) {
                    Falsified(buildMessage(a, e), i)
                }
            }.take(n)
            .find { it.isFalsified() }
            .toOption()
            .getOrElse { Passed }
    }*/

fun <A> buildMessage(a: A, e: Exception): FailedCase =
    """
        test case: $a
        generated and exception: ${e.message}
        stacktrace:
        ${e.stackTrace.joinToString("\n")}
    """.trimIndent()

private fun <A> randomSequence(ga: Gen<A>, rng: RNG): Sequence<A> =
    sequence {
        val (a: A, rng1: RNG) = ga.sample.run(rng)
        yield(a)
        yieldAll(randomSequence(ga, rng1))
    }

typealias MaxSize = Int

fun <A> forAll(g: SGen<A>, f: (A) -> Boolean): Prop = TODO()

typealias SuccessCount = Int
typealias FailedCase = String

typealias TestCases = Int
//typealias Result = Option<Pair<FailedCase, SuccessCount>>

sealed class Result {
    abstract fun isFalsified(): Boolean
}

object Passed : Result() {
    override fun isFalsified(): Boolean = false
}

data class Falsified(val failure: FailedCase, val succeses: SuccessCount) : Result() {
    override fun isFalsified(): Boolean = true
}

data class Prop(val check: (MaxSize, TestCases, RNG) -> Result) {
    companion object {
        fun <A> forAll(g: SGen<A>, f: (A) -> Boolean): Prop =
            forAll(SGen { i -> g(i) }, f)

        fun <A> forAll(g: (Int) -> Gen<A>, f: (A) -> Boolean): Prop =
            Prop { max, n, rng ->
                val casePerSize: Int = (n + (max - 1)) / max

                val props: Sequence<Prop> = generateSequence(0) { it + 1 }
                    .take(min(n, max) + 1)
                    .map { i -> forAll(SGen { g(i) }, f) }

                val prop: Prop = props.map { p ->
                    Prop { max, _, rng ->
                        p.check(max, casePerSize, rng)
                    }
                }.reduce { p1, p2 -> p1.and(p2) }

                prop.check(max, n, rng)
            }
    }

    fun and(other: Prop): Prop = Prop { m, n, rng ->
        when (val prop = check(m, n, rng)) {
            is Passed -> other.check(m, n, rng)
            is Falsified -> prop
        }
    }

    fun or(other: Prop): Prop = Prop { m, n, rng ->
        when (val prop = check(m, n, rng)) {
            is Falsified -> other.tag(prop.failure).check(m, n, rng)
            is Passed -> prop
        }
    }

    private fun tag(msg: String) = Prop { m, n, rng ->
        when (val prop = check(m, n, rng)) {
            is Falsified -> Falsified(
                "$msg: ${prop.failure}",
                prop.succeses
            )
            is Passed -> prop
        }
    }
}

data class Gen<A>(val sample: State<RNG, A>) {
    companion object {
        fun <A> listOfN(n: Int, ga: Gen<A>): Gen<List<A>> =
            Gen(State.sequence(List.fill(n) { ga.sample }))

        fun <A> listOfN(gn: Gen<Int>, ga: Gen<A>): Gen<List<A>> =
            gn.flatMap { n -> listOfN(n, ga) }

        fun <A> liftOption(ga: Gen<A>): Gen<Option<A>> =
            ga.map { a -> if (a == null) None else Some(a) }

        //fun <A> getOption(goa: Gen<Option<A>>): Gen<A> =
        //    goa.flatMap { oa -> oa. }
    }

    fun <B> flatMap(f: (A) -> Gen<B>): Gen<B> =
        Gen(this.sample.flatMap { a -> f(a).sample })

    fun <B> map(f: (A) -> B): Gen<B> =
        flatMap { a -> unit(f(a)) }

    fun <A> union(ga: Gen<A>, gb: Gen<A>): Gen<A> =
        boolean().flatMap { if (it) ga else gb }

    fun <A> weighted(
        pga: Pair<Gen<A>, Double>,
        pgb: Pair<Gen<A>, Double>
    ): Gen<A> {
        val (ga, p1) = pga
        val (gb, p2) = pgb
        val threshold = p1.absoluteValue / (p1.absoluteValue + p2.absoluteValue)
        return Gen(State { rng -> double(rng) })
            .flatMap { d -> if (d < threshold) ga else gb }
    }

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

    fun chooseString(n: Int): Gen<String> =
        listOfN(n, chooseChar())
            .map { List.foldLeft(it, "") { acc, a -> acc + a } }

    fun <A> unit(a: A): Gen<A> = Gen(State.unit(a))

    fun boolean(): Gen<Boolean> =
        Gen(State { rng -> nextBoolean(rng) })

    fun execute(rng: RNG): Pair<A, RNG> = this.sample.run(rng)

    fun unsized(): SGen<A> = SGen { _ -> this }

}

data class SGen<A>(val forSize: (Int) -> Gen<A>) {
    /*companion object {
        fun <A> forAll(g: SGen<A>, f: (A) ->)
    }*/

    operator fun invoke(i: Int): Gen<A> =
        this.forSize(i)

    fun <B> map(f: (A) -> B): SGen<B> =
        SGen { i -> forSize(i).map(f) }

    fun <B> flatMap(f: (A) -> Gen<B>): SGen<B> =
        SGen { i -> forSize(i).flatMap(f) }

    fun listOf(): SGen<List<A>> = SGen { i -> Gen.listOfN(i, forSize(i)) }
}

fun main() {
    val rng = SimpleRNG(404)
    val gen = Gen(State(intR))
    println(Gen.listOfN(5, gen).execute(rng).first)
    println(gen.choose(10, 15).execute(rng).first)
    println(gen.choosePair(20, 25).execute(rng).first)
    println(gen.chooseChar().execute(rng).first)
    println(gen.chooseString(8).execute(rng).first)
}