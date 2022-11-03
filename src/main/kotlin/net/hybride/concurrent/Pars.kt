package net.hybride.concurrent

import net.hybride.getOrElse
import net.hybride.par.firstOption
import net.hybride.par.splitAt
import java.util.concurrent.TimeUnit
//import java.util.concurrent.ExecutorService

interface Callable<A> {
    fun call(): A
}

interface Future<A> {
    fun get(): A
    fun get(timeout: Long, timeUnit: TimeUnit): A
    fun cancel(evenIfRunning: Boolean): Boolean
    fun isDone(): Boolean
    fun isCancelled(): Boolean
}

interface ExecutorService {
    fun <A> submit(c: Callable<A>): Future<A>
}

typealias Par<A> = (ExecutorService) -> Future<A>

fun <A> run(es: ExecutorService, a: Par<A>): Future<A> = a(es)

object Pars {
    fun <A> unit(a: A): Par<A> = {
            es: ExecutorService -> UnitFuture(a)
    }

    data class UnitFuture<A>(val a: A) : Future<A> {
        override fun get(): A = a
        override fun get(timeout: Long, timeUnit: TimeUnit): A = a
        override fun cancel(evenIfRunning: Boolean): Boolean = false
        override fun isDone(): Boolean = true
        override fun isCancelled(): Boolean = false
    }

    fun <A, B, C> map2Unit(
        a: Par<A>,
        b: Par<B>,
        f: (A, B) -> C
    ): Par<C> = {
            es: ExecutorService ->
        val af: Future<A> = a(es)
        val bf: Future<B> = b(es)
        UnitFuture(f(af.get(), bf.get()))
    }

    fun <A> fork(
        a: () -> Par<A>
    ): Par<A> = {
            es: ExecutorService ->
        es.submit(object : Callable<A> {
            override fun call(): A {
                return a()(es).get()
            }
        })
    }

    fun <A> delay(pa: () -> Par<A>): Par<A> = { es -> pa()(es) }

    data class TimedMap2Future<A, B, C>(
        val pa: Future<A>,
        val pb: Future<B>,
        val f: (A, B) -> C
    ) : Future<C> {
        override fun isDone(): Boolean = TODO("Unused")
        override fun get(): C = f(pa.get(), pb.get())
        override fun get(to: Long, tu: TimeUnit): C {
            val timeoutMillis = TimeUnit.MILLISECONDS.convert(to, tu)
            val start = System.currentTimeMillis()
            val a = pa.get(to, tu)
            val duration = System.currentTimeMillis() - start
            val remainder = timeoutMillis - duration
            val b = pb.get(remainder, TimeUnit.MILLISECONDS)
            return f(a, b)
        }
        override fun cancel(b: Boolean): Boolean = TODO("Unused")
        override fun isCancelled(): Boolean = TODO("Unused")
    }

    fun <A, B, C> map2(
        a: Par<A>,
        b: Par<B>,
        f: (A, B) -> C
    ): Par<C> = {
            es: ExecutorService ->
        val fa: Future<A> = a(es)
        val fb: Future<B> = b(es)
        TimedMap2Future(fa, fb, f)
    }

    fun <A> lazyUnit(a: () -> A): Par<A> =
        Pars.fork { Pars.unit(a()) }

    fun <A, B> asyncF(f: (A) -> B): (A) -> Par<B> = { a ->
        lazyUnit { f(a) }
    }

    fun sortParNaive(parList: Par<List<Int>>): Par<List<Int>> =
        map2(parList, unit(Unit)) { a, _ -> a.sorted() }

    fun <A, B> map(pa: Par<A>, f: (A) -> B): Par<B> =
        map2(pa, unit(Unit), { a, _ -> f(a) })

    fun sortPar(parList: Par<List<Int>>): Par<List<Int>> =
        map(parList) { it.sorted() }

    fun <A> sequence1(ps: List<Par<A>>): Par<List<A>> =
        when {
            ps.isEmpty() -> unit(emptyList())
            else -> map2(
                ps.first(),
                sequence1(ps.drop(1))
            ) { a: A, b: List<A> ->
                listOf(a) + b
            }
        }

    fun <A> sequence(ps: List<Par<A>>): Par<List<A>> =
        when {
            ps.isEmpty() -> unit(emptyList())
            ps.size == 1 -> map(ps.first()) { listOf(it) }
            else -> {
                val (l,r) = ps.splitAt(ps.size/2)
                map2(sequence(l), sequence(r)) { la, lb -> la + lb }
            }
        }

    fun <A, B> parMap(
        ps: List<A>,
        f: (A) -> B
    ): Par<List<B>> = fork {
        val fbs: List<Par<B>> = ps.map(asyncF(f))
        sequence(fbs)
    }
    fun <A> parFilter(
        sa: List<A>,
        f: (A) -> Boolean
    ): Par<List<A>> {
        val fas: List<Par<A>> = sa.map { lazyUnit { it } }
        //return map(sequence(fas)) { fa -> fa.filter { f(it) }}
         return map(sequence(fas)) { fa -> fa.flatMap { a ->
             if (f(a)) listOf(a) else emptyList()
         }}
    }


    private val maxInt: (Int, Int) -> Int = { a: Int, b: Int -> if (a >= b) a else b }

}

class SimpleExecutorService: ExecutorService {
    override fun <A> submit(c: Callable<A>): Future<A> {
        return Pars.UnitFuture(c.call())
    }

}
fun main() {
    val es: ExecutorService = SimpleExecutorService()
    val l = listOf(2,4,7,2,9,4,3)

}
