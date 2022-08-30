package net.hybride

sealed class Option<out A>
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

fun main() {
    println(Some("one").map { a -> a.length })
    println(Some("one").flatMap { a -> Some(a.length) })
    println(Some("twenty").getOrElse { 10 })
    println(None.getOrElse { 10 })
    println(Some("twenty").orElse({ Some("ten") }))
    println(None.orElse { Some(10) })
}
