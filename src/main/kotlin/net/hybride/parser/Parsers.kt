package net.hybride.parser

import arrow.core.Either
import arrow.core.Right

interface Parsers<PE> {

    interface Parser<A>

    fun char(c: Char): Parser<Char> = TODO()

    fun string(s: String): Parser<String> = TODO()

    fun <A> run(p: Parser<A>, input: String): Either<PE, A>

    //fun orString(s1: String, s2: String): Parser<String>

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