package net.hybride.typeclasses

import arrow.Kind
import arrow.higherkind

interface Traversable<F> : Functor<F> {
    // foreach
    fun <G, A, B> traverse(
        fa: Kind<F, A>,
        AG: Applicative<G>,
        f: (A) -> Kind<G, B>
    ): Kind<G, Kind<F, B>> =
        sequence(map(fa, f), AG)

    // collect
    fun <G, A> sequence(
        fga: Kind<F, Kind<G, A>>,
        AG: Applicative<G>
    ): Kind<G, Kind<F, A>> =
        traverse(fga, AG) { it }
}

@higherkind
data class Tree<out A>(val head: A, val tail: List<Tree<A>>) : TreeOf<A>
fun <A> optionTraversable(): Traversable<ForOption> = object : Traversable<ForOption> {
    override fun <A, B> map(fa: Kind<ForOption, A>, f: (A) -> B): Kind<ForOption, B> =
        fa.fix().map(f)
}

fun <A> listTraversable(): Traversable<ForList> = object : Traversable<ForList> {
    override fun <A, B> map(fa: Kind<ForList, A>, f: (A) -> B): Kind<ForList, B> =
        fa.fix().map(f)
}

fun <A> treeTraversable(): Traversable<ForTree> = object : Traversable<ForTree> {
    override fun <A, B> map(fa: Kind<ForTree, A>, f: (A) -> B): Kind<ForTree, B> =
        TODO()
}