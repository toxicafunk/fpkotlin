package net.hybride.concurrent

import java.util.concurrent.Executors as JUExecutors
import java.util.concurrent.ExecutorService as JUExecutorService

import net.hybride.nonblocking.Future
import net.hybride.Either
import net.hybride.Left
import net.hybride.Option
import net.hybride.Right

val es: JUExecutorService = JUExecutors.newFixedThreadPool(4)
val s = Strategy.from(es)
val echoer = Actor<String>(s) {
    println("got message: $it")
}

fun <A, B, C> Par.map2(pa: Par<A>, pb: Par<B>, f: (A, B) -> C): Par<C> =
    { es: ExecutorService ->
        object : Future<C>() {
            override fun invoke(cb: (C) -> Unit) {
                val ar = AtomicReference<Option<A>>(None)
                val br = AtomicReference<Option<B>>(None)
                val combiner =
                    Actor<Either<A, B>>(Strategy.from(es)) { eab ->
                        when (eab) {
                            is Left<A> ->
                                br.get().fold(
                                    { ar.set(Some(eab.a)) },
                                    { b -> eval(es) { cb(f(eab.a, b)) } }
                                )

                            is Right<B> ->
                                ar.get().fold(
                                    { br.set(Some(eab.b)) },
                                    { a -> eval(es) { cb(f(a, eab.b)) } }
                                )
                        }
                    }
                pa(es).invoke { a: A -> combiner.send(Left(a)) }
                pb(es).invoke { b: B -> combiner.send(Right(b)) }
            }
        }
    }

fun main() {
    echoer.send("hello")
    echoer.send("goodbye")
    echoer.send("You're just repeating everything I say, aren't you?")
}

