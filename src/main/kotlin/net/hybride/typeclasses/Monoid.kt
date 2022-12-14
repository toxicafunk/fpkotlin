package net.hybride.typeclasses

import arrow.core.None
import arrow.core.Option
import arrow.core.compose
import arrow.core.extensions.list.foldable.foldLeft
import arrow.core.orElse
import chapter8.Gen
import chapter8.Passed
import chapter8.Prop.Companion.forAll
import chapter8.SimpleRNG
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import net.hybride.par.splitAt

interface Monoid<A> {
    fun combine(a1: A, a2: A): A
    val nil: A
}

val stringMonoid = object : Monoid<String> {
    override fun combine(a1: String, a2: String): String = a1 + a2

    override val nil: String = ""
}

fun <A> listMonoid(): Monoid<List<A>> = object : Monoid<List<A>> {
    override fun combine(a1: List<A>, a2: List<A>): List<A> =
        a1 + a2

    override val nil: List<A> = emptyList()
}

fun intAdditionMonoid(): Monoid<Int> = object : Monoid<Int> {
    override fun combine(a1: Int, a2: Int): Int = a1 + a2

    override val nil: Int = 0
}

fun intMultiplicationMonoid(): Monoid<Int> = object : Monoid<Int> {
    override fun combine(a1: Int, a2: Int): Int = a1 * a2

    override val nil: Int = 1
}

fun booleanOr(): Monoid<Boolean> = object : Monoid<Boolean> {
    override fun combine(a1: Boolean, a2: Boolean): Boolean =
        a1 || a2

    override val nil: Boolean = false
}

fun booleanAnd(): Monoid<Boolean> = object : Monoid<Boolean> {
    override fun combine(a1: Boolean, a2: Boolean): Boolean =
        a1 && a2

    override val nil: Boolean = true
}

/*
  _AND_ | true  | false |     _OR_ | true  | false |
  true  | true  | false |     true | true  | true  |
  false | false | false |    false | true  | false |
 */

fun <A> optionMonoid(): Monoid<Option<A>> = object : Monoid<Option<A>> {
    override fun combine(a1: Option<A>, a2: Option<A>): Option<A> =
        a1.orElse { a2 }

    override val nil: Option<A> = None
}

fun <A> dual(m: Monoid<A>): Monoid<A> = object : Monoid<A> {
    override fun combine(a1: A, a2: A): A =
        m.combine(a2, a1)

    override val nil: A = m.nil
}

fun <A> firstOptionMonoid() = optionMonoid<A>()

fun <A> lastOptionMonoid() = dual(firstOptionMonoid<A>())

fun <A> endoMonoid(): Monoid<(A) -> A> = object : Monoid<(A) -> A> {
    override fun combine(a1: (A) -> A, a2: (A) -> A): (A) -> A =
        { a -> a1(a2(a)) }

    override val nil: (A) -> A
        get() = { a -> a }
}

fun <A> endoMonoidComposed(): Monoid<(A) -> A> = object : Monoid<(A) -> A> {
    override fun combine(a1: (A) -> A, a2: (A) -> A): (A) -> A =
        a1 compose a2

    override val nil: (A) -> A
        get() = { it }
}

fun <A> monoidLaws(m: Monoid<A>, gen: Gen<A>) =
    forAll(
        gen.flatMap { a ->
            gen.flatMap { b ->
                gen.map { c ->
                    Triple(a, b, c)
                }
            }
        }
    ) { (a, b, c) ->
        m.combine(a, m.combine(b, c)) == m.combine(m.combine(a, b), c) &&
            m.combine(m.nil, a) == m.combine(a, m.nil) &&
            m.combine(m.nil, a) == a
    }
class AssociativitySpec : WordSpec({
    val max = 100
    val count = 100
    val rng = SimpleRNG(42)
    val intGen = Gen.choose(-10000, 10000)

    "law of associativity" should {
        "be upheld using existing monoids" {
            monoidLaws(intAdditionMonoid(), intGen)
                .check(max, count, rng) shouldBe Passed
            monoidLaws(intMultiplicationMonoid(), intGen)
                .check(max, count, rng) shouldBe Passed
        }
    }
})

fun <A> concatenate(la: List<A>, m: Monoid<A>): A =
    la.foldLeft(m.nil, m::combine)

fun <A, B> foldMap(la: List<A>, m: Monoid<B>, f: (A) -> B): B =
    la.foldLeft(m.nil) { b, a -> m.combine(b, f(a)) }

fun <A, B> foldMapN(la: List<A>, m: Monoid<B>, f: (A) -> B): B =
    concatenate(la.map(f), m)

fun <A, B> foldRight(la: Sequence<A>, z: B, f: (A, B) -> B): B =
    foldMap(la.toList(), endoMonoid()) { a: A -> { b: B -> f(a,b) }}(z)

fun <A, B> foldLeft(la: Sequence<A>, z: B, f: (B, A) -> B): B =
    foldMap(la.toList(), dual(endoMonoid())) { a: A -> { b: B -> f(b, a) } }(z)

fun <A, B> foldMapBF(la: List<A>, m: Monoid<B>, f: (A) -> B): B {
    val (left,right) = la.splitAt(la.size/2)
    return m.combine(
        left.foldLeft(m.nil) { b, a -> m.combine(b, f(a)) },
        right.foldLeft(m.nil) { b, a -> m.combine(b, f(a)) }
    )
}

fun <A, B> foldMapBook(la: List<A>, m: Monoid<B>, f: (A) -> B): B =
    when {
        la.size >= 2 -> {
            val (la1, la2) = la.splitAt(la.size / 2)
            m.combine(foldMap(la1, m, f), foldMap(la2, m, f))
        }
        la.size == 1 ->
            f(la.first())
        else -> m.nil
    }
