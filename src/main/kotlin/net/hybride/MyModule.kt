package net.hybride

import net.hybride.MyModule.abs
import net.hybride.MyModule.factorial
import kotlin.collections.List

object MyModule {

    fun abs(n: Int): Int =
        if (n < 0) {
            -n
        } else {
            n
        }

    fun factorial(i: Int): Int {
        tailrec fun go(n: Int, acc: Int): Int =
            if (n <= 0) {
                acc
            } else {
                go(n - 1, n * acc)
            }

        return go(i, 1)
    }

    fun fib(i: Int): Int {
        tailrec fun loop(n: Int, acc: Int): Int =
            if (n == 1) {
                acc + 1
            } else {
                loop(n - 1, acc + n)
            }

        return loop(i, 0)
    }

    fun formatAbs(x: Int): String {
        val msg = "The absolute value of %d is %d"
        return msg.format(x, abs(x))
    }

    fun formatFactorial(x: Int): String {
        val msg = "The factorial of %d is %d"
        return msg.format(x, factorial(x))
    }

    fun formatResult(name: String, n: Int, f: (Int) -> Int): String {
        val msg = "The %s of %d is %d."
        return msg.format(name, n, f(n))
    }

    fun <A> findFirst(xs: Array<A>, p: (A) -> Boolean): Int {
        tailrec fun loop(n: Int): Int =
            when {
                n >= xs.size -> -1
                p(xs[n]) -> n
                else -> loop(n + 1)
            }

        return loop(0)
    }

    fun <A, B, C> partial1(a: A, f: (A, B) -> C): (B) -> C = { b ->
        f(a, b)
    }

    fun <A, B, C> curry(f: (A, B) -> C): (A) -> (B) -> C = {
            a ->
        { b -> f(a, b) }
    }

    fun <A, B, C> uncurry(f: (A) -> (B) -> C): (A, B) -> C = {
            a, b ->
        f(a)(b)
    }

    fun <A, B, C> compose(f: (B) -> C, g: (A) -> B): (A) -> C =
        { a -> f(g(a)) }

    val <T> List<T>.tail: List<T>
        get() = drop(1)

    val <T> List<T>.head: T
        get() = first()

    fun <A> isSorted(aa: List<A>, order: (A, A) -> Boolean): Boolean {
        tailrec fun loop(n: Int, acc: Boolean): Boolean =
            when {
                n >= aa.size -> acc
                order(aa[n - 1], aa[n]) -> loop(n + 1, true)
                else -> false
            }

        return loop(1, true)
    }

    fun <A> isSorted1(aa: List<A>, order: (A, A) -> Boolean): Boolean {
        tailrec fun loop(aa1: List<A>, acc: Boolean): Boolean =
            when {
                aa1.isEmpty() || aa1.size == 1 -> acc
                order(aa1.head, aa1.tail.head) -> loop(aa1.tail, true)
                else -> false
            }

        return loop(aa, true)
    }
}

fun main() {
    println(MyModule.formatAbs(-42))
    println(MyModule.formatFactorial(7))
    println(MyModule.fib(7))
    println(MyModule.formatResult("absolute value", -42, ::abs))
    println(MyModule.formatResult("factorial", 7, ::factorial))
    println(MyModule.findFirst(arrayOf(3, 7, 9, 13)) { x -> x == 9 })
    println(MyModule.isSorted(listOf(1, 2, 3, 4, 5), { i: Int, j: Int -> i <= j }))
    println(MyModule.isSorted(listOf(1, 2, 5, 4, 3), { i: Int, j: Int -> i <= j }))
    println(MyModule.isSorted(listOf(1, 2, 4, 4, 5), { i: Int, j: Int -> i <= j }))

    println(MyModule.isSorted1(listOf(1, 2, 3, 4, 5), { i: Int, j: Int -> i <= j }))
    println(MyModule.isSorted1(listOf(1, 2, 5, 4, 3), { i: Int, j: Int -> i <= j }))
    println(MyModule.isSorted1(listOf(1, 2, 4, 4, 5), { i: Int, j: Int -> i <= j }))
}
