package net.hybride.typeclasses

import arrow.Kind
import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.ForListK
import arrow.core.ListKOf
import arrow.core.extensions.listk.traverse.map
import arrow.core.fix

interface Functor<F> {
    fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B>

    fun <A, B> distribute(
        fab: Kind<F, Pair<A, B>>
    ): Pair<Kind<F, A>, Kind<F, B>> =
        map(fab) { it.first } to map(fab) { it.second }

    fun <A, B> codistribute(
        e: Either<Kind<F, A>, Kind<F, B>>
    ): Kind<F, Either<A, B>> =
        when (e) {
            is Left -> map(e.a) { Left(it) }
            is Right -> map(e.b) { Right(it) }
        }
}

val listFunctor: Functor<ForListK> = object : Functor<ForListK> {
    override fun <A, B> map(fa: ListKOf<A>, f: (A) -> B): ListKOf<B> =
        fa.fix().map(f)
}

