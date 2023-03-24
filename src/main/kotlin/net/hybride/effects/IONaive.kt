package net.hybride.effects

import net.hybride.Option
import net.hybride.Some
import net.hybride.None

data class Player(val name: String, val score: Int)

/*fun contest(p1: Player, p2: Player): Unit =
    when {
        p1.score > p2.score ->
            println("${p1.name} is the winner!")
        p1.score < p2.score ->
            println("${p2.name} is the winner!")
        else ->
            println("It's a draw!")
    }
*/

interface IONaive<A> {
    companion object {
        fun empty(): IONaive<Unit> = object : IONaive<Unit> {
            override fun run(): Unit = Unit
        }

        fun <A> unit(a: () -> A): IONaive<A> =
            object : IONaive<A> {
                override fun run(): A =
                    a()
            }

        operator fun <A> invoke(a: () -> A): IONaive<A> = unit(a)
    }

    fun run(): A

    infix fun <B> assoc(io: IONaive<B>): IONaive<Pair<A, B>> =
        object : IONaive<Pair<A, B>> {
            override fun run(): Pair<A, B> =
                this@IONaive.run() to io.run()
        }

    fun <B> map(f: (A) -> B): IONaive<B> =
        object : IONaive<B> {
            override fun run(): B =
                f(this@IONaive.run())
        }

    fun <B> flatMap(f: (A) -> IONaive<B>): IONaive<B> =
        object : IONaive<B> {
            override fun run(): B =
                f(this@IONaive.run()).run()
        }
}

fun winner(p1: Player, p2: Player): Option<Player> =
    when {
        p1.score > p2.score -> Some(p1)
        p1.score < p2.score -> Some(p2)
        else -> None
    }

fun winnerMsg(op: Option<Player>): String =
    when (op) {
        is Some -> "${op.get.name} is the winner"
        is None -> "It's a draw"
    }

/*fun stdout(msg: String): IO<Unit> =
    object : IO<Unit> {
        override fun run(): Unit {
            println(msg)
        }
    }
*/
fun contest(p1: Player, p2: Player): IONaive<Unit> =
    stdoutNaive(winnerMsg(winner(p1, p2)))

fun fahrenheitToCelsius(f: Double): Double = (f - 32) * 5.0 / 9.0

fun stdin(): IONaive<String> = IONaive { readLine().orEmpty() }

fun stdoutNaive(msg: String): IONaive<Unit> = IONaive { println(msg) }

fun converter(): IONaive<Unit> =
    stdoutNaive("Enter a temperature in degrees Fahrenheit: ").flatMap {
        stdin().map { it.toDouble() }.flatMap { df ->
            stdoutNaive("Degrees Celsius: ${fahrenheitToCelsius(df)}")
        }
    }

val echo: IONaive<Unit> = stdin().flatMap(::stdoutNaive)
val readInt: IONaive<Int> = stdin().map { it.toInt() }
val readInts: IONaive<Pair<Int, Int>> = readInt assoc readInt

fun main() {
    stdoutNaive(winnerMsg(winner(Player("Eric", 60), Player("John", 89)))).run()
    converter().run()
    readInts
        .map { pair -> pair.first * pair.second }
        .flatMap { i -> stdoutNaive("Multiplied they yield $i") }
        .run()
}