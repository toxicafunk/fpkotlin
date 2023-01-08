package net.hybride.typeclasses

import arrow.Kind
import arrow.core.fix

interface Mon<F> {
    fun <A, B> flatMap(fa: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B>

    fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B>

    fun <A, B, C> map2(
        fa: Kind<F, A>,
        fb: Kind<F, B>,
        f: (A, B) -> C
    ): Kind<F, C> =
        flatMap(fa) { a -> map(fb) { b -> f(a, b) } }
}