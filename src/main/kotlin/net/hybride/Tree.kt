package net.hybride

sealed class Tree<out A> {
    companion object {
        fun <A> size(t: Tree<A>): Int {
            tailrec fun loop(t1: Tree<A>, acc: Int): Int =
                when (t1) {
                    is Leaf -> acc + 1
                    is Branch -> 1 + loop(t1.left, acc) + loop(t1.right, acc)
                }

            return loop(t, 0)
        }

        fun <A> maximum(t: Tree<A>): A {
            tailrec fun loop(t1: Tree<A>, acc: A): A =
                when (t1) {
                    is Leaf -> maxOf(acc, t1.leaf)
                    is Branch -> maxOf(loop(t1.left, acc), loop(t1.right, acc))
                }

            return loop(t, )
        }
    }
}
data class Leaf<A>(val value: A) : Tree<A>()
data class Branch<A>(val left: Tree<A>, val right: Tree<A>) : Tree<A>()

fun main() {
    val simpleTree = Branch(Leaf(1), Leaf(6))
    val tree1 = Branch(Branch(Leaf(1), Leaf(5)), Leaf(6))
    println(Tree.size(simpleTree))
    println(Tree.size(tree1))
}
