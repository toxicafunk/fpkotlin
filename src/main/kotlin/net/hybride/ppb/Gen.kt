package net.hybride.ppb

fun <A> listOf(a: Gen<A>): List<Gen<A>> = TODO()

fun <A> listOfN(n: Int, a: Gen<A>): List<Gen<A>> = TODO()

fun <A> forAll(a: Gen<A>, pred: (A) -> Boolean): Prop = TODO()

interface Prop {
    // fun check(): Unit unit throws away onfo
    fun check(): Boolean

    fun and(p: Prop): Prop {
        return object : Prop {
            val checked = this.check() && p.check()
            override fun check(): Boolean = checked

        }
    }
}
