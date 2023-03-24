package net.hybride.effects

import chapter13.boilerplate.io.fix
import chapter13.boilerplate.io.IOOf
import chapter13.boilerplate.io.IOMonad

sealed class IO<A> : IOOf<A> {
    companion object {
        fun <A> unit(a: A) = Suspend { a }

        fun monad(): IOMonad = object : IOMonad{}
    }
    fun <B> flatMap(f: (A) -> IO<B>): IO<B> = FlatMap(this, f)
    fun <B> map(f: (A) -> B): IO<B> = flatMap { a -> Return(f(a)) }
}

data class Return<A>(val a: A) : IO<A>()
data class Suspend<A>(val resume: () -> A) : IO<A>()
data class FlatMap<A, B>(
    val sub: IO<A>,
    val f: (A) -> IO<B>
) : IO<B>()

fun stdout(s: String): IO<Unit> = Suspend { println(s) }
val p = IO.monad()
    .forever<Unit, Unit>(stdout("To infinity and beyond!"))
    .fix()

@Suppress("UNCHECKED_CAST")
tailrec fun <A> run(io: IO<A>): A =
    when (io) {
        is Return -> io.a
        is Suspend -> io.resume()
        is FlatMap<*, *> -> {
            val x = io.sub as IO<A>
            val f = io.f as (A) -> IO<A>
            when (x) {
                is Return ->
                    run(f(x.a))
                is Suspend ->
                    run(f(x.resume()))
                is FlatMap<*, *> -> {
                    val g = x.f as (A) -> IO<A>
                    val y = x.sub as IO<A>
                    run(y.flatMap { a: A -> g(a).flatMap(f) })
                }
            }
        }
    }