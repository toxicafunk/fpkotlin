package net.hybride.concurrent

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ExecutorService as JUExecutorService
import java.util.concurrent.Executors as JUExecutors
import net.hybride.Either
import net.hybride.getOrElse
import net.hybride.Left
import net.hybride.map
import net.hybride.nonblocking.Future
import net.hybride.None
import net.hybride.Option
import net.hybride.Right
import net.hybride.Some

val es: JUExecutorService = JUExecutors.newFixedThreadPool(4)
val s = Strategy.from(es)
val echoer = Actor<String>(s) {
    println("got message: $it")
}

typealias JPar<A> = (JUExecutorService) -> Future<A>

fun <A, B> Option<A>.fold(b: () -> B, f: (A) -> B): B =
    this.map(f).getOrElse(b)

fun <A, B, C> JPar<A>.map2(pa: JPar<A>, pb: JPar<B>, f: (A, B) -> C): JPar<C> =
    { es: JUExecutorService ->
        object : Future<C>() {
            override fun invoke(cb: (C) -> Unit) {
                val ar = AtomicReference<Option<A>>(None)
                val br = AtomicReference<Option<B>>(None)
                val combiner =
                    Actor<Either<A, B>>(Strategy.from(es)) { eab ->
                        when (eab) {
                            is Left<A> ->
                                br.get().fold(
                                    { ar.set(Some(eab.value)) },
                                    { b -> eval(es) { cb(f(eab.value, b)) } }
                                )

                            is Right<B> ->
                                ar.get().fold(
                                    { br.set(Some(eab.value)) },
                                    { a -> eval(es) { cb(f(a, eab.value)) } }
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
