package net.hybride

import net.hybride.typeclasses.Par
import kotlin.math.pow

fun <A, B> Option<A>.map(f: (A) -> B): Option<B> =
    when (this) {
        is None -> None
        is Some -> Some(f(this.get))
    }

//fun <A, B> Option<A>.flatMap(f: (A) -> Option<B>): Option<B> =
//    this.map(f).getOrElse { None }

fun <A> Option<A>.getOrElse(default: () -> A): A =
    when (this) {
        is None -> default()
        is Some -> this.get
    }

fun <A> Option<A>.orElse1(ob: () -> Option<A>): Option<A> =
    when (this) {
        is None -> ob()
        is Some -> this
    }

fun <A> Option<A>.orElse(ob: () -> Option<A>): Option<A> =
    this.map { Some(it) }
        .getOrElse { ob() }

fun <A> Option<A>.filter(f: (A) -> Boolean): Option<A> =
    this.flatMap { a: A -> if (f(a)) Some(a) else None }

/*
TC       |    Unit      |     Generic      |       Effect
Option   |    None      |     Some         | Optionality / Nullability
Either   |    Left*     |     Right*       | Alternatibity / Duality
List     |    Nil       |     Cons         | Undeterminibility

Class: Int, String, MyADT, List<Int>, Option<MyData>, Either<String, Double>, Functor<List<String>, List<Int>>
Kinds: * -> *, List, Option, Either, Functor<List>
Higher Kinded Types (HKTs): Functor, Monad
*/
/*
class Monoid<A> (val nil: A, val combine: (A, A) -> A): A
class Functor<F, A, B>(val original: F<A>, val map: A -> B ): F<B>

class Functor<Option, A, B>(val original: Option<A>): Option<B> {
    fun map(f: A -> B): Option<B> =
        original.map(a -> f(a))
}

Functor<F<A>>.map(i -> trap { remote { someF(i) }})
*/

sealed class Option<out A> {
    companion object {
        fun <A> unit(a: A): Option<A> = TODO()

        fun <A> empty(): Option<A> = None

        fun <A, B> lift(f: (A) -> B): (Option<A>) -> Option<B> = { oa -> oa.map(f) }

        fun <A, B, C> map2(oa: Option<A>, ob: Option<B>, f: (A, B) -> C): Option<C> =
            oa.flatMap { a: A ->
                ob.map { b: B ->
                    f(a, b)
                }
            }

        fun <A, B, C> map2P(oa: Option<A>, ob: Option<B>, f: (A, B) -> C): Option<C> =
            when {
                oa is Some && ob is Some -> oa.flatMap { a: A ->
                    ob.map { b: B -> f(a, b) }
                }
                else -> None
            }

        fun <A> sequenceM(xs: List<Option<A>>): Option<List<A>> =
            xs.foldRight(Some(Nil as List<A>)) {
                    oa: Option<A>,
                    acc: Option<List<A>> ->
                map2(oa, acc) { a: A, l: List<A> -> Cons(a, l) }
            }

        fun <A> sequenceL(xs: List<Option<A>>): Option<List<A>> {
            tailrec fun loop(xs1: List<Option<A>>, acc: Option<List<A>>): Option<List<A>> =
                when (xs1) {
                    is Nil -> acc
                    is Cons -> when (xs1.head) {
                        is None -> None
                        is Some ->
                            loop(
                                xs1.tail,
                                xs1.head.map { a ->
                                    Cons(a, acc.getOrElse { Nil })
                                }
                            )
                    }
                }

            return loop(xs, None).map { l -> List.reverse(l) }
        }

        fun <A> sequenceF(xs: List<Option<A>>): Option<List<A>> {
            val zero = Some(Nil as List<A>) as Option<List<A>>
            val optList = List.foldLeft(xs, zero) { acc, a: Option<A> ->
                acc.flatMap { l: List<A> ->
                    when (a) {
                        is None -> None
                        is Some -> Some(Cons(a.get, l))
                    }
                }
            }

            return optList.map { l -> List.reverse(l) }
        }

        fun <A, B> traverseNaive(
            xa: List<A>,
            f: (A) -> Option<B>
        ): Option<List<B>> =
            sequenceM(List.map(xa, f))

        fun <A, B> traverseL(
            xa: List<A>,
            f: (A) -> Option<B>
        ): Option<List<B>> {
            fun loop(xa1: List<A>, acc: Option<List<B>>): Option<List<B>> =
                when (xa1) {
                    is Nil -> acc.map { List.reverse(it) }
                    is Cons -> loop(
                        xa1.tail,
                        acc.flatMap { l: List<B> ->
                            f(xa1.head).map { b -> Cons(b, l) }
                        }
                    )
                }

            return loop(xa, Some(Nil as List<B>) as Option<List<B>>)
        }

        fun <A, B> traverse(
            xa: List<A>,
            f: (A) -> Option<B>
        ): Option<List<B>> =
            when (xa) {
                is Nil -> Some(Nil as List<B>)
                is Cons ->
                    map2(f(xa.head), traverse(xa.tail, f)) { b, xb ->
                        Cons(b, xb)
                    }
            }

        fun <A> sequence(xs: List<Option<A>>): Option<List<A>> =
            traverse(xs) { it }
    }

    fun <B> flatMap(f: (A) -> Option<B>): Option<B> =
        this.map(f).getOrElse { None }
}
data class Some<out A>(val get: A) : Option<A>()
object None : Option<Nothing>()

fun meanF(xs: List<Double>): Option<Double> {
    val size = List.length(xs)
    return if (size == 0) None else Some(List.foldLeft(xs, 0.0) { x1, x2 -> x1 + x2 } / size)
}

fun variance(xs: List<Double>): Option<Double> =
    meanF(xs).flatMap { m ->
        meanF(
            List.map(xs) { x -> (m - x).pow(2) }
        )
    }

fun main() {
    val ds = List.of(1.0, 2.0, 3.0, 4.0, 5.0)
    println(Some("one").map { a -> a.length })
    println(Some("one").flatMap { a -> Some(a.length) })
    println(Some("twenty").getOrElse { 10 })
    println(None.getOrElse { 10 })
    println(Some("twenty").orElse { Some("ten") })
    println(None.orElse { Some(10) })
    println(variance(ds))
    println(Option.map2(Some(4), Some(6)) { a, b -> a * b })
    println(Option.map2(Some(4), None as Option<Int>) { a, b -> a * b })
    println(Option.sequence(List.of(Some(1), Some(2), Some(3))))
    println(Option.sequence(List.of(Some(1), None, Some(3))))
    println(Option.sequenceF(List.of(Some(1), Some(2), Some(3))))
    println(Option.sequenceF(List.of(Some(1), None, Some(3))))
    println(Option.traverseNaive(ds) { a -> Some(a * 2) })
    println(Option.traverse(ds) { a -> Some(a * 2) })
}
