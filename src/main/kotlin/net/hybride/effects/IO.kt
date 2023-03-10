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

interface IO {
    fun run(): Unit
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


fun stdout(msg: String): IO =
    object : IO {
        override fun run(): Unit {
            println(msg)
        }
    }

fun httpPost(msg: String): IO =
    object : IO {
        override fun run() {
            TODO("Send over HTTP")
        }
    }

fun contest(p1: Player, p2: Player): IO =
    stdout(winnerMsg(winner(p1, p2)))

fun httpContest(p1: Player, p2: Player): IO =
    httpPost(winnerMsg(winner(p1, p2)))

class Runtime()

fun main() {
    stdout(winnerMsg(winner(Player("Eric", 60), Player("John", 89)))).run()
    httpPost(winnerMsg(winner(Player("Eric", 60), Player("John", 89)))).run()

}