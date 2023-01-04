package net.hybride.typeclasses

import arrow.Kind
import arrow.core.*
import arrow.core.extensions.list.foldable.foldLeft
import arrow.core.extensions.list.foldable.foldMap
import arrow.core.extensions.set.foldable.foldLeft
import chapter8.Gen
import chapter8.Passed
import chapter8.Prop.Companion.forAll
import chapter8.SimpleRNG
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import net.hybride.concurrent.ExecutorService
import net.hybride.concurrent.Par
import net.hybride.concurrent.Pars.map2
import net.hybride.concurrent.Pars.unit
import net.hybride.concurrent.SimpleExecutorService
import net.hybride.concurrent.run
import net.hybride.par.splitAt
import java.lang.Integer.min
import java.util.Locale
import java.util.concurrent.TimeUnit

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
    val (left,right) = la.splitAt(la.size/2 - 1)
    return m.combine(
        left.foldLeft(m.nil) { b, a -> m.combine(b, f(a)) },
        right.foldLeft(m.nil) { b, a -> m.combine(b, f(a)) }
    )
}

fun <A, B> foldMapBook(la: List<A>, m: Monoid<B>, f: (A) -> B): B =
    when {
        la.size >= 2 -> {
            val (la1, la2) = la.splitAt(la.size/2 - 1)
            m.combine(foldMap(la1, m, f), foldMap(la2, m, f))
        }
        la.size == 1 ->
            f(la.first())
        else -> m.nil
    }

fun <A> par(m: Monoid<A>): Monoid<Par<A>> = object : Monoid<Par<A>> {
    override fun combine(a1: Par<A>, a2: Par<A>): Par<A> =
        map2(a1, a2) { a11: A, a21: A -> m.combine(a11, a21) }

    override val nil: Par<A>
        get() = unit(m.nil)
}

fun <A, B> parFoldMap(
    la: List<A>,
    pm: Monoid<Par<B>>,
    f: (A) -> B
): Par<B> =
    when {
        la.size >= 2 -> {
            val (la1, la2) = la.splitAt(la.size/2 - 1)
            pm.combine(parFoldMap(la1, pm, f), parFoldMap(la2, pm, f))
        }
        la.size == 1 -> unit(f(la.first()))
        else -> pm.nil
    }

sealed class WC

data class Stub(val chars: String) : WC()
data class Part(val ls: String, val words: Int, val rs: String) : WC()

fun wcMonoid(): Monoid<WC> = object : Monoid<WC> {
    override fun combine(a1: WC, a2: WC): WC =
        when (a1) {
            is Stub -> when(a2) {
                is Stub -> Stub(a1.chars + a2.chars)
                is Part -> Part(a1.chars + a2.ls, a2.words, a2.rs)
            }
            is Part -> when(a2) {
                is Stub -> Part(a1.ls, a1.words, a2.chars + a2.chars)
                is Part -> Part(a1.ls,
                    a1.words + a2.words +(if ((a1.rs + a2.ls).isEmpty()) 0 else 1),
                    a2.rs
                )
            }
        }

    override val nil: WC
        get() = Stub("")
}

fun wordCount(s: String): Int {
    fun wc(c: Char): WC =
        if (c.isWhitespace()) Part("", 0, "")
        else Stub("$c")

    fun unstub(s: String): Int = min(s.length, 1)

    val WCM = wcMonoid()

    return when (val wc = foldMap(s.asSequence().toList(), WCM) { wc(it) }) {
        is Stub -> unstub(wc.chars)
        is Part -> unstub(wc.rs) + wc.words + unstub(wc.rs)
    }
}

// HKT
/*
        Type     |  Kind
        String   |   *
        List<A>  |  * -> *          | String -> List<String>
        F<A>     |  * -> * -> *     | List -> String -> List<String>, Integer -> Monoid -> Monoid<Integer>
        G<F<A>>  | * -> * -> * -> * | Monad -> List -> String -> Monad<List<String>>

        OO -> A is a X
        FP -> A has a X, A forms X under [conditions]
 */

interface Foldable<F> {
    // Kind<F, A> = F<A>
    fun <A, B> foldRight(fa: Kind<F, A>, z: B, f: (A, B) -> B): B =
        foldLeft(fa, z) { b, a -> f(a,b)}
    fun <A, B> foldLeft(fa: Kind<F, A>, z: B, f: (B, A) -> B): B =
        foldRight(fa, z) { a, b -> f(b,a) }
    fun <A, B> foldMap(fa: Kind<F, A>, m: Monoid<B>, f: (A) -> B): B =
        foldLeft(fa, m.nil) { b, a -> m.combine(f(a), b) }
    fun <A> concatenate(fa: Kind<F, A>, m: Monoid<A>): A =
        foldLeft(fa, m.nil, m::combine)
}

object ListFoldable : Foldable<ForListK> {
    override fun <A, B> foldLeft(fa: ListKOf<A>, z: B, f: (B, A) -> B): B =
        fa.fix().foldLeft(z, f)

    override fun <A, B> foldRight(fa: ListKOf<A>, z: B, f: (A, B) -> B): B =
        fa.fix().foldRight(z, f)
}

fun <A, B> productMonoid(
    ma: Monoid<A>,
    mb: Monoid<B>
): Monoid<Pair<A, B>> = object : Monoid<Pair<A, B>> {
    override fun combine(a1: Pair<A, B>, a2: Pair<A, B>): Pair<A, B> =
        ma.combine(a1.first, a2.first) to mb.combine(a1.second, a2.second)

    override val nil: Pair<A, B>
        get() = ma.nil to mb.nil
}

fun <K, V> mapMergeMonoid(v: Monoid<V>): Monoid<Map<K, V>> =
    object : Monoid<Map<K, V>> {
        override fun combine(a1: Map<K, V>, a2: Map<K, V>): Map<K, V> =
            (a1.keys + a2.keys).foldLeft(nil) { acc, k ->
                acc + mapOf(
                    k to v.combine(
                        a1.getOrDefault(k, v.nil),
                        a2.getOrDefault(k, v.nil)
                    )
                )
            }

        override val nil: Map<K, V> = emptyMap()
    }

val m: Monoid<Map<String, Map<String, Int>>> =
    mapMergeMonoid<String, Map<String, Int>>(
        mapMergeMonoid<String, Int>(
            intAdditionMonoid()
        )
    )

fun main() {
    val es: ExecutorService = SimpleExecutorService()
    val fut = run(es, parFoldMap(
        listOf("lorem", "ipsum", "dolor", "sit"),
        par(stringMonoid)
    ) { it.uppercase(Locale.getDefault()) })
    println(fut.get(500L, TimeUnit.MILLISECONDS))
    val m1 = mapOf("o1" to mapOf("i1" to 1, "i2" to 2))
    val m2 = mapOf("o1" to mapOf("i3" to 3))
    println(m.combine(m1, m2))
}