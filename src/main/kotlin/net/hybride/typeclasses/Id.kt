package net.hybride.typeclasses

import arrow.higherkind

@higherkind
data class Id<A>(val a: A): IdOf<A> {
    companion object {
        fun <A> unit(a: A): Id<A> = Id(a)
    }
    fun <B> flatMap(f: (A) -> Id<B>): Id<B> = f(this.a)

    fun <B> map(f: (A) -> B): Id<B> = unit(f(this.a))
}

fun idMonad(): Monad<ForId> = object : Monad<ForId> {
    override fun <A> unit(a: A): IdOf<A> =
        Id.unit(a)

    override fun <A, B> flatMap(fa: IdOf<A>, f: (A) -> IdOf<B>): IdOf<B> =
        fa.fix().flatMap { a -> f(a).fix() }

    override fun <A, B> map(fa: IdOf<A>, f: (A) -> B): IdOf<B> =
        fa.fix().map(f)
}

fun main() {
    val iDM: Monad<ForId> = idMonad()
    val id: Id<String> = iDM.flatMap(Id("Hello, ")) {
            a: String ->
        iDM.flatMap(Id("monad!")) { b: String ->
            Id(a + b)
        } }.fix()
    println(id)

    val aId = Id("hey... ")
    val bId = Id("been trying to meet u!")
    val myId = aId.flatMap { a ->
        bId.map { b -> a + b }
    }
    println(myId)

    val oneId = Id(1)
    val tenId = Id(10)
    val exp = tenId.flatMap<String> { a ->
        if (a > 30) {
            Id.unit("Hola")
        } else {
            oneId.map { b ->
                "Hey $b"
            }
        }
    }

    println(exp)
}