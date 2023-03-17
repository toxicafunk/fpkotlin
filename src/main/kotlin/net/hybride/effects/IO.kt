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

interface IO<A> {
    companion object {
        fun empty(): IO<Unit> = object : IO<Unit> {
            override fun run(): Unit = Unit
        }

        fun <A> unit(a: () -> A): IO<A> =
            object : IO<A> {
                override fun run(): A =
                    a()
            }

        operator fun <A> invoke(a: () -> A): IO<A> = unit(a)
    }

    fun run(): A

    infix fun <B> assoc(io: IO<B>): IO<Pair<A, B>> =
        object : IO<Pair<A, B>> {
            override fun run(): Pair<A, B> =
                this@IO.run() to io.run()
        }

    fun <B> map(f: (A) -> B): IO<B> =
        object : IO<B> {
            override fun run(): B =
                f(this@IO.run())
        }

    fun <B> flatMap(f: (A) -> IO<B>): IO<B> =
        object : IO<B> {
            override fun run(): B =
                f(this@IO.run()).run()
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
fun contest(p1: Player, p2: Player): IO<Unit> =
    stdout(winnerMsg(winner(p1, p2)))

fun fahrenheitToCelsius(f: Double): Double = (f - 32) * 5.0 / 9.0

fun stdin(): IO<String> = IO { readLine().orEmpty() }

fun stdout(msg: String): IO<Unit> = IO { println(msg) }

fun converter(): IO<Unit> =
    stdout("Enter a temperature in degrees Fahrenheit: ").flatMap {
        stdin().map { it.toDouble() }.flatMap { df ->
            stdout("Degrees Celsius: ${fahrenheitToCelsius(df)}")
        }
    }

val echo: IO<Unit> = stdin().flatMap(::stdout)
val readInt: IO<Int> = stdin().map { it.toInt() }
val readInts: IO<Pair<Int, Int>> = readInt assoc readInt

fun main() {
    stdout(winnerMsg(winner(Player("Eric", 60), Player("John", 89)))).run()
    converter().run()
    readInts
        .map { pair -> pair.first * pair.second }
        .flatMap { i -> stdout("Multiplied they yield $i") }
        .run()
}