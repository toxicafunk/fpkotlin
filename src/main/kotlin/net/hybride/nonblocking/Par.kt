package net.hybride.nonblocking

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference

abstract class Future<A> {
    internal abstract fun invoke(cb: (A) -> Unit)

    fun <A> unit(a: A): Par<A> =
        { _: ExecutorService ->
            object : Future<A>() {
                override fun invoke(cb: (A) -> Unit) {
                    cb(a)
                }
            }
        }

    fun <A> runL(es: ExecutorService, pa: Par<A>): A {
        val ref = AtomicReference<A>()
        val latch = CountDownLatch(1)
        pa(es).invoke { a: A ->
            ref.set(a)
            latch.countDown()
        }
        latch.await()
        return ref.get()
    }

    fun <A> run(es: ExecutorService, pa: Par<A>): A {
        val ref = CompletableFuture<A>()
        pa(es).invoke { a: A ->
            ref.complete(a)
        }
        return ref.get()
    }

    fun eval(es: ExecutorService, r: () -> Unit) {
        es.submit(Callable { r() })
    }

    class InvokableFuture<A>(val es: ExecutorService, val a: () -> Par<A>) : Future<A>() {
        override fun invoke(cb: (A) -> Unit) =
            eval(es) { a()(es).invoke(cb) }
    }

    fun <A> fork(a: () -> Par<A>): Par<A> = { es: ExecutorService ->
        object : Future<A>() {
            override fun invoke(cb: (A) -> Unit) =
                eval(es) { a()(es).invoke(cb) }
        }
    }

    fun <A> forkI(a: () -> Par<A>): Par<A> = { es: ExecutorService ->
        InvokableFuture(es, a)
    }

    fun <A, B, C> map2(pa: Par<A>, pb: Par<B>, f: (A, B) -> C): Par<C> = TODO()
}

typealias Par<A> = (ExecutorService) -> Future<A>

