package net.hybride

sealed class List<out A> {
    companion object {
        fun <A> of(vararg aa: A): List<A> {
            val tail = aa.sliceArray(1 until aa.size)
            return if (aa.isEmpty()) Nil else Cons(aa.first(), of(*tail))
        }

        fun sum(ints: List<Int>): Int =
            when (ints) {
                is Nil -> 0
                is Cons -> ints.head + sum(ints.tail)
            }

        fun product(doubles: List<Double>): Double =
            when (doubles) {
                is Nil -> 1.0
                is Cons ->
                    if (doubles.head == 0.0) 0.0
                    else doubles.head * product(doubles.tail)
            }

        fun <A> tail(xs: List<A>): List<A> =
            when (xs) {
                is Nil -> Nil
                is Cons -> xs.tail
            }

        fun <A> setHead(xs: List<A>, x: A): List<A> =
            Cons(x, xs)

        tailrec fun <A> drop(l: List<A>, n: Int): List<A> =
            when(l) {
                is Nil -> Nil
                is Cons -> if (n == 0) l else drop(l.tail, n-1)
            }

        fun <A> drop1(l: List<A>, n: Int): List<A> {
            tailrec fun loop(n1: Int, acc: List<A>): List<A> =
                when (l) {
                    is Nil -> Nil
                    is Cons ->
                        if (n1 == n) acc
                        else when(acc) {
                            is Nil -> Nil
                            is Cons -> loop(n1 + 1, acc.tail)
                        }
                }

            return if (n <= 0) Nil else loop(0, l)
        }

        tailrec fun <A> dropWhile(l: List<A>, f: (A) -> Boolean): List<A>  =
            when(l) {
                is Nil -> Nil
                is Cons ->
                    if (f(l.head)) dropWhile(l.tail, f)
                    else l
            }

        fun <A> dropWhile1(l: List<A>, f: (A) -> Boolean): List<A>  =
            when(l) {
                is Nil -> l
                is Cons ->
                    if (f(l.head)) dropWhile1(l.tail, f)
                    else Cons(l.head, dropWhile1(l.tail, f))
            }

        fun <A> append(a1: List<A>, a2: List<A>): List<A> =
            when (a1) {
                is Nil -> a2
                is Cons -> Cons(a1.head, append(a1.tail, a2))
            }

        fun <A> init(l: List<A>): List<A> =
            when(l) {
                is Nil -> l
                is Cons ->
                    if (l.tail == Nil) Nil
                    else Cons(l.head, init(l.tail))
            }

        fun <A, B> foldRight(xs: List<A>, z: B, f: (A, B) -> B): B =
            when (xs) {
                is Nil -> z
                is Cons -> f(xs.head, foldRight(xs.tail, z, f))
            }

        fun sum2(ints: List<Int>): Int =
            foldRight(ints, 0, { a, b -> a + b })

        fun product2(dbs: List<Double>): Double =
            foldRight(dbs, 1.0, { a, b -> a * b })

        fun <A> empty(): List<A> = Nil

        fun <A> length(xs: List<A>): Int =
            foldRight(xs, 0, { _, b -> b + 1})

        fun <A, B> foldLeft(xs: List<A>, z: B, f: (B, A) -> B): B {
            tailrec fun loop(xs1: List<A>, acc: B): B =
                when (xs1) {
                    is Nil -> acc
                    is Cons -> loop(xs1.tail, f(acc, xs1.head))
                }

            return loop(xs, z)
        }

        fun sum3(ints: List<Int>): Int =
            foldLeft(ints, 0, {b, a -> b + a})

        fun product3(doubles: List<Double>): Double =
            foldLeft(doubles, 1.0, { b, a -> b * a})

        fun <A> length2(xs: List<A>): Int =
            foldLeft(xs, 0, { b, _ -> b + 1})

        fun <A> reverse(xs: List<A>): List<A> =
            foldLeft(xs, Nil as List<A>, { b, a -> Cons(a, b) })

        fun <A, B> foldRight2(xs: List<A>, z: B, f: (A, B) -> B): B =
            foldLeft(xs, z, { b, a -> f(a,b) })

        fun <A, B> foldLeft2(xs: List<A>, z: B, f: (B, A) -> B): B =
            foldRight(xs, z, { a, b -> f(b,a) })

        fun product4(doubles: List<Double>): Double =
            foldLeft2(doubles, 1.0, { b, a -> b * a})

        fun product5(doubles: List<Double>): Double =
            foldRight2(doubles, 1.0, { a, b -> b * a})

        fun <A> append2(a1: List<A>, a2: List<A>): List<A> =
            foldRight(a1, a2, { a, b -> Cons(a, b) })

        fun <A> flatten(ll: List<List<A>>): List<A> =
            foldLeft(ll, Nil as List<A>, { b, a -> append(a, b) })

        fun addOne(xs: List<Int>): List<Int> =
            reverse(foldLeft(xs, Nil as List<Int>, { b, a -> Cons(a + 1, b) }))

        fun addOne2(xs: List<Int>): List<Int> =
            when(xs) {
                is Nil -> xs
                is Cons -> Cons(xs.head + 1, addOne2(xs.tail))
            }

        fun doubles2String(xs: List<Double>): List<String> =
            reverse(foldLeft(xs, Nil as List<String>, { b, a -> Cons(a.toString(), b) }))

        fun <A, B> map(xs: List<A>, f: (A) -> B): List<B> =
            when(xs) {
                is Nil -> xs
                is Cons -> Cons(f(xs.head), map(xs.tail, f))
            }

        fun <A, B> map2(xs: List<A>, f: (A) -> B): List<B> {
            tailrec fun loop(xs1: List<A>, acc: List<B>): List<B> =
                when(xs1) {
                    is Nil -> acc
                    is Cons -> loop(xs1.tail, Cons(f(xs1.head), acc))
                }

            return loop(xs, Nil)
        }

        fun <A, B> mapFL(xs: List<A>, f: (A) -> B): List<B> =
            foldLeft(xs, Nil as List<A>, { b, a -> Cons(f(a), b) } )
        //fun <A> filter(xs: List<A>, f: (A) -> Boolean): List<A> =

    }
}
object Nil: List<Nothing>()
data class Cons<out A>(val head: A, val tail: List<A>): List<A>()

fun main() {
    val ls = List.of(1,2,3,4,5)
    val ls1 = List.of(6,7,8,9,10)
    val ds = List.of(1.0,2.0,3.0,4.0,5.0)
    println(ls)
    println(List.sum(ls))
    println(List.product(ds))
    println(List.setHead(ls,8))
    println(List.drop(ls,3))
    println(List.drop(ls,8))
    println(List.drop1(ls,3))
    println(List.drop1(ls,8))

    val filterEven = { i: Int -> i % 2 == 0 }
    val filterOdd = { i: Int -> i % 2 == 1 }
    val filterLess3 = { i: Int -> i <= 3 }
    println(List.dropWhile(ls, filterOdd))
    println(List.dropWhile(ls, filterLess3))
    println(List.dropWhile1(ls, filterEven))
    println(List.dropWhile1(ls, filterLess3))
    println(List.init(ls))

    println(List.foldRight(ls, Nil as List<Int>, { x, y -> Cons(x, y) }))
    println(List.sum2(ls))
    println(List.product2(ds))
    println(List.length(ls))
    println(List.sum3(ls))
    println(List.product3(ds))
    println(List.length2(ls))

    println(List.reverse(ls))
    println(List.product4(ds))
    println(List.product5(ds))

    println(List.append2(ls, ls1))
    println(List.flatten(List.of(ls, ls1, ls)))
    println(List.addOne(ls))
    println(List.addOne2(ls))
    println(List.doubles2String(ds))

    println(List.map(ls, { it + 2 }))
    println(List.map2(ls, { it * 2 }))
}
