package net.hybride

sealed class Tree<out A> {
    companion object {
        fun <A> size(t: Tree<A>): Int {
            fun loop(t1: Tree<A>, acc: Int): Int =
                when (t1) {
                    is Leaf -> acc + 1
                    is Branch -> 1 + loop(t1.left, acc) + loop(t1.right, acc)
                }

            return loop(t, 0)
        }

        fun maximum(t: Tree<Int>): Int {
            fun loop(t1: Tree<Int>, acc: Int): Int =
                when (t1) {
                    is Leaf -> maxOf(acc, t1.value)
                    is Branch -> maxOf(loop(t1.left, acc), loop(t1.right, acc))
                }

            return loop(t, -99999)
        }

        fun <A> depth(t: Tree<A>): Int {
            fun loop(t1: Tree<A>, acc: Int): Int =
                when (t1) {
                    is Leaf -> acc + 1
                    is Branch -> maxOf(loop(t1.left, acc + 1), loop(t1.right, acc + 1))
                }

            return loop(t, 0)
        }

        fun <A, B> map(t: Tree<A>, f: (A) -> B): Tree<B> =
            when (t) {
                is Leaf -> Leaf(f(t.value))
                is Branch -> Branch(map(t.left, f), map(t.right, f))
            }

        fun <A, B> fold(ta: Tree<A>, l: (A) -> B, b: (B, B) -> B): B =
            when (ta) {
                is Leaf -> l(ta.value)
                is Branch -> b(fold(ta.left, l, b), fold(ta.right, l, b))
            }

        fun <A> sizeF(ta: Tree<A>): Int =
            fold(ta, { _ -> 1 }, { b1, b2 -> b1 + b2 })

        fun maximumF(ta: Tree<Int>): Int =
            fold(ta, { a -> a }, { b1, b2 -> maxOf(b1, b2) })

        fun <A> depthF(ta: Tree<A>): Int =
            fold(ta, { _ -> 1 }, { b1, b2 -> maxOf(b1 + 1, b2 + 1) })

        fun <A, B> mapF(ta: Tree<A>, f: (A) -> B): Tree<B> =
            fold(
                ta,
                { a -> Leaf(f(a)) as Tree<B> },
                { b1, b2 -> Branch(b1, b2) }
            )
    }
}
data class Leaf<A>(val value: A) : Tree<A>()
data class Branch<A>(val left: Tree<A>, val right: Tree<A>) : Tree<A>()

fun main() {
    val simpleTree = Branch(Leaf(1), Leaf(6))
    val tree1 = Branch(Branch(Leaf(1), Leaf(5)), Leaf(6))
    val tree2 = Branch(Branch(Leaf(1), Leaf(8)), Branch(Leaf(6), Branch(Leaf(-3), Leaf(0))))
    val stringTree = Branch(Leaf("one"), Leaf("two"))
    val stringTree1 = Branch(Branch(Leaf("one"), Leaf("twenty")), Leaf("two"))
    println(Tree.size(simpleTree))
    println(Tree.size(tree1))
    println(Tree.maximum(tree1))
    println(Tree.maximum(tree2))
    println(Tree.depth(simpleTree))
    println(Tree.depth(tree1))
    println(Tree.depth(tree2))
    println(Tree.map(simpleTree) { it + 1 })
    println(Tree.map(tree2) { it * 2 })
    println(Tree.fold(stringTree, { it.length }, { b1, b2 -> b1 + b2 }))
    println(Tree.fold(stringTree1, { it.length }, { b1, b2 -> b1 + b2 }))
    println(Tree.sizeF(tree2))
    println(Tree.maximumF(tree2))
    println(Tree.depthF(tree2))
    println(Tree.mapF(tree2) { it * 2 })
}
