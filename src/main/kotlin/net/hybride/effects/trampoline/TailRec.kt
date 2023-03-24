package net.hybride.effects.trampoline

import arrow.higherkind

@higherkind
sealed class Tailrec<A> : TailrecOf<A> {
    fun <B> flatMap(f: (A) -> Tailrec<B>): Tailrec<B> = FlatMap(this, f)
    fun <B> map(f: (A) -> B): Tailrec<B> = flatMap { a -> Return(f(a)) }
}
data class Return<A>(val a: A) : Tailrec<A>()
data class Suspend<A>(val resume: () -> A) : Tailrec<A>()
data class FlatMap<A, B>(
    val sub: Tailrec<A>,
    val f: (A) -> Tailrec<B>
) : Tailrec<B>()

val f = { x: Int -> Return(x) }
val g = List(100000) { idx -> f }
    .fold(f) { a: (Int) -> Tailrec<Int>, b: (Int) -> Tailrec<Int> ->
        { x: Int ->
            Suspend { Unit }.flatMap { _ -> a(x).flatMap(b) }
        }
    }

@Suppress("UNCHECKED_CAST")
tailrec fun <A> run(io: Tailrec<A>): A =
    when (io) {
        is Return -> io.a
        is Suspend -> io.resume()
        is FlatMap<*, *> -> {
            val x = io.sub as Tailrec<A>
            val f = io.f as (A) -> Tailrec<A>
            when (x) {
                is Return ->
                    run(f(x.a))
                is Suspend ->
                    run(f(x.resume()))
                is FlatMap<*, *> -> {
                    val g = x.f as (A) -> Tailrec<A>
                    val y = x.sub as Tailrec<A>
                    run(y.flatMap { a: A -> g(a).flatMap(f) })
                }
            }
        }
    }

fun main() {
    println(run(g(42)))
}