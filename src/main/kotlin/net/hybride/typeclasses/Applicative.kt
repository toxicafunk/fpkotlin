package net.hybride.typeclasses

import arrow.Kind
import arrow.core.extensions.set.foldable.foldLeft
import arrow.core.foldRight

interface Applicative<F> : Functor<F> {

    fun <A, B, C> map2(
        fa: Kind<F, A>,
        fb: Kind<F, B>,
        f: (A, B) -> C
    ): Kind<F, C>

    fun <A> unit(a: A): Kind<F, A>

    override fun <A, B> map(
        fa: Kind<F, A>,
        f: (A) -> B
    ): Kind<F, B> =
        map2(fa, unit(Unit)) { a, _ -> f(a) }

    fun <A, B> traverse(
        la: List<A>,
        f: (A) -> Kind<F, B>
    ): Kind<F, List<B>> =
        la.foldRight(
            unit(List.empty<B>())
        ) { a: A, acc: Kind<F, List<B>> ->
            map2(f(a), acc) { b: B, lb: List<B> -> Cons(b, lb) }
        }

    fun <A> sequence(lfa: List<Kind<F, A>>): Kind<F, List<A>> =
        traverse(lfa) { it }

    fun <K, V> sequence(mkv: Map<K, Kind<F, V>>): Kind<F, Map<K, V>> =
        mkv.entries.foldLeft(unit(emptyMap())) { facc: Kind<F, Map<K, V>>, (k: K, fv: Kind<F, V>) ->
            map2(facc, fv) { acc, v -> acc + (k to v)}
        }

    fun <A> replicateM(n: Int, ma: Kind<F, A>): Kind<F, List<A>> =
        sequence(List.fill(n, ma))

    fun <A, B> product(
        ma: Kind<F, A>,
        mb: Kind<F, B>
    ): Kind<F, Pair<A, B>> =
        map2(ma, mb) { a, b -> a to b }
}