package net.hybride.parser

import arrow.core.Either
import arrow.core.Right

import net.hybride.ppb.Gen
import net.hybride.ppb.Prop
import net.hybride.ppb.Prop.Companion.forAll

interface Parser<A>

interface Parsers<PE> {

    fun char(c: Char): Parser<Char> = string(c.toString()).map { it[0] }

    fun string(s: String): Parser<String>

    fun <A> succeed(a: A): Parser<A> = string("").map { a }

    fun <A> slice(pa: Parser<A>): Parser<String>

    fun <A> run(p: Parser<A>, input: String): Either<PE, A> = TODO()

    fun <A> or(pa: Parser<A>, a2: Parser<A>): Parser<A>

    infix fun String.or(other: String): Parser<String> =
        or(string(this), string(other))

    fun <A> listOfN(i: Int, p: Parser<A>): Parser<List<A>>

    fun zeroPlusChars(c: Char, s: String): Parser<Int>

    fun onePlusChars(c: Char, s: String): Either<PE, Parser<Int>> /*{
        val p = run(zeroOrMoreChars(s), s)
        p.fold({ e -> e}, {i -> if (i == 0) PE else i})
    }*/

    fun zeroPlusOnePlusChars(a: Char, b: Char, s: String): Either<PE, Parser<Pair<Int, Int>>>

    fun <A> Parser<A>.many(): Parser<List<A>>

    fun <A,B> Parser<A>.map(f: (A) -> B): Parser<B>

    fun <A> many1(p: Parser<A>): Parser<List<A>>

    //fun <A, B> product(pa: Parser<A>, pb: Parser<B>): Parser<Pair<A, B>>

    infix fun <A, B> Parser<A>.product(pb: Parser<B>): Parser<Pair<A, B>>

    fun <A, B, C> map2(
        pa: Parser<A>,
        pb: () -> Parser<B>,
        f: (A, B) -> C
    ): Parser<C>


    fun charLaws(c: Char): Boolean =
     run(char(c), c.toString()) == Right(c)

    fun stringLaws(s: String): Boolean =
        run(string(s), s) == Right(s) &&
        run("abra" or "cadabra", "abra") == Right("abra")

    fun listLaws(): Boolean =
        run(listOfN(3, "ab" or "cad"), "ababab") == Right("ababab")
                && run(listOfN(3, "ab" or "cad"), "cadcadcad") == Right("cadcadcad")
                && run(listOfN(3, "ab" or "cad"), "ababcad") == Right("ababcad")
                && run(listOfN(3, "ab" or "cad"), "cadabab") == Right("cadabab")

    fun countBeginLaws(): Boolean =
        run(zeroPlusChars('a',"aa"), "aa") == Right(2)
                && run(zeroPlusChars('a',"b123"), "b123") == Right(0)
                && run(zeroPlusChars('a',""), "b123") == Right(0)


}

//tag::init3[]
object ParseError

abstract class Laws: Parsers<ParseError> {
    private fun <A> equal(
        p1: Parser<A>,
        p2: Parser<A>,
        i: Gen<String>
    ): Prop =
        forAll(i) { s: String -> run(p1, s) == run(p2, s) }

    fun <A> mapLaw(p: Parser<A>, i: Gen<String>): Prop =
        equal(p, p.map { a: A -> a }, i)

}
//end::init3[]

abstract class Example : Parsers<ParseError> {
    val numA: Parser<Int> = char('a').many().map { it.size }

    fun numALaws(): Boolean = run(numA, "aaa") == Right(3)
            && run(numA, "b") == Right(0)

    fun sliceLaws(): Boolean =
        run(slice(("a" or "b").many()), "aaba") == Right("aaba")

    override fun <A, B, C> map2(
        pa: Parser<A>,
        pb: () -> Parser<B>,
        f: (A, B) -> C
    ): Parser<C> =
        (pa product pb()).map { pair -> f(pair.first, pair.second) }

    override fun <A> many1(p: Parser<A>): Parser<List<A>> =
        map2(p, { p.many() }) { a, b -> listOf(a) + b }

    fun <A> many2(pa: Parser<A>): Parser<List<A>> =
        or(
            map2(pa, { pa.many() }) { a, la -> listOf(a) + la },
            succeed(emptyList())
        )

    fun <A> listOfN1(n: Int, pa: Parser<A>): Parser<List<A>> =
        if (n > 0)
            map2(pa, { listOfN1(n - 1, pa) }) { a, la ->
                listOf(a) + la
            }
        else succeed(emptyList())
}