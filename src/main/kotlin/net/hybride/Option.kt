package net.hybride

import kotlin.math.pow

sealed class Option<out A> {
    companion object {
        fun <A> sequence(xs: List<Option<A>>): Option<List<A>> {
            tailrec fun loop(xs1: List<Option<A>>, acc: Option<List<A>>): Option<List<A>> =
                when (xs1) {
                    is Nil -> acc
                    is Cons -> when (xs1.head) {
                        is None -> None
                        is Some ->
                            loop(
                                xs1.tail,
                                xs1.head.map {
                                        a ->
                                    Cons(a, acc.getOrElse { Nil })
                                }
                            )
                    }
                }

            return loop(xs, None).map { l -> List.reverse(l) }
        }

        fun <A> sequenceF(xs: List<Option<A>>): Option<List<A>> {
            val zero = Some(Nil as List<A>) as Option<List<A>>
            val optList = List.foldLeft(xs, zero) { acc, a ->
                acc.flatMap { l ->
                    when (a) {
                        is None -> None
                        is Some -> Some(Cons(a.get, l))
                    }
                }
            }

            return optList.map { l -> List.reverse(l) }
        }
    }
}
data class Some<out A>(val get: A) : Option<A>()
object None : Option<Nothing>()

fun <A, B> Option<A>.map(f: (A) -> B): Option<B> =
    when (this) {
        is None -> None
        is Some -> Some(f(this.get))
    }

fun <A> Option<A>.getOrElse(default: () -> A): A =
    when (this) {
        is None -> default()
        is Some -> this.get
    }

fun <A, B> Option<A>.flatMap(f: (A) -> Option<B>): Option<B> =
    this.map(f).getOrElse { None }

fun <A> Option<A>.orElse1(ob: () -> Option<A>): Option<A> =
    when (this) {
        is None -> ob()
        is Some -> Some(this.get)
    }

fun <A> Option<A>.orElse(ob: () -> Option<A>): Option<A> =
    this.map { Some(it) }
        .getOrElse { ob() }

fun mean(xs: List<Double>): Option<Double> {
    val size = List.length(xs)
    return if (size == 0) None else Some(List.foldLeft(xs, 0.0) { x1, x2 -> x1 + x2 } / size)
}

fun variance(xs: List<Double>): Option<Double> =
    mean(xs).flatMap { m ->
        mean(
            List.map(xs, { x -> (m - x).pow(2) })
        )
    }

fun <A, B> lift(f: (A) -> B): (Option<A>) -> Option<B> = { oa -> oa.map(f) }

fun <A, B, C> map2(a: Option<A>, b: Option<B>, f: (A, B) -> C): Option<C> =
    when {
        a is Some && b is Some -> a.flatMap { i ->
            b.map { j -> f(i, j) }
        }
        else -> None
    }

fun main() {
    val ds = List.of(1.0, 2.0, 3.0, 4.0, 5.0)
    println(Some("one").map { a -> a.length })
    println(Some("one").flatMap { a -> Some(a.length) })
    println(Some("twenty").getOrElse { 10 })
    println(None.getOrElse { 10 })
    println(Some("twenty").orElse({ Some("ten") }))
    println(None.orElse { Some(10) })
    println(variance(ds))
    println(map2(Some(4), Some(6)) { a, b -> a * b })
    println(Option.sequence(List.of(Some(1), Some(2), Some(3))))
    println(Option.sequence(List.of(Some(1), None, Some(3))))
    println(Option.sequenceF(List.of(Some(1), Some(2), Some(3))))
    println(Option.sequenceF(List.of(Some(1), None, Some(3))))
}
