package net.hybride.typeclasses

import arrow.Kind
import arrow.core.ForListK
import arrow.core.ForSequenceK
import arrow.core.ListKOf
import arrow.core.fix
import arrow.core.ListK
import arrow.core.SequenceK
import arrow.core.SequenceKOf
import net.hybride.typeclasses.Option

interface Monad<F>: Functor<F> {
    fun <A> unit(a: A): Kind<F, A>

    fun <A, B> flatMap(fa: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B>

    override fun <A, B> map(
        fa: Kind<F, A>,
        f: (A) -> B
    ): Kind<F, B> =
        flatMap(fa) { a -> unit(f(a)) }

    fun <A, B, C> map2(
        fa: Kind<F, A>,
        fb: Kind<F, B>,
        f: (A, B) -> C
    ): Kind<F, C> =
        flatMap(fa) { a -> map(fb) { b -> f(a, b) } }

    fun <A> sequence(lfa: List<Kind<F, A>>): Kind<F, List<A>> =
        lfa.foldRight(
            unit(List.empty<A>())
        ) { fa: Kind<F, A>, fla: Kind<F, List<A>> ->
            map2(fa, fla) { a, la -> Cons<A>(a, la) as List<A> }
        }

    fun <A, B> traverse(
        la: List<A>,
        f: (A) -> Kind<F, B>
    ): Kind<F, List<B>> =
        TODO()
}

object Monads {
    val genMonad = object : Monad<ForGen> {
        override fun <A> unit(a: A): Kind<ForGen, A> = Gen.unit(a)

        override fun <A, B> flatMap(fa: Kind<ForGen, A>, f: (A) -> Kind<ForGen, B>): Kind<ForGen, B> =
            fa.fix().flatMap { a -> f(a).fix() }
    }

    val parMonad = object : Monad<ForPar> {
        override fun <A> unit(a: A): ParOf<A> = Par.unit(a)

        override fun <A, B> flatMap(fa: ParOf<A>, f: (A) -> ParOf<B>): ParOf<B> =
            fa.fix().flatMap { a -> f(a).fix() }
    }

    val optionMonad = object : Monad<ForOption> {
        override fun <A> unit(a: A): OptionOf<A> = Some(a)

        override fun <A, B> flatMap(fa: OptionOf<A>, f: (A) -> OptionOf<B>): OptionOf<B> =
            fa.fix().flatMap { a -> f(a).fix() }
    }

    val listMonad = object : Monad<ForList> {
        override fun <A> unit(a: A): ListOf<A> = List.unit(a)

        override fun <A, B> flatMap(fa: ListOf<A>, f: (A) -> ListOf<B>): ListOf<B> =
            fa.fix().flatMap { a -> f(a).fix() }
    }

    val listKMonad: Monad<ForListK> = object : Monad<ForListK> {
        override fun <A> unit(a: A): ListKOf<A> = ListK.just(a)

        override fun <A, B> flatMap(fa: ListKOf<A>, f: (A) -> ListKOf<B>): ListKOf<B> =
            fa.fix().flatMap(f)
    }

    val sequenceKMonad: Monad<ForSequenceK> = object : Monad<ForSequenceK> {
        override fun <A> unit(a: A): SequenceKOf<A> = SequenceK.just(a)

        override fun <A, B> flatMap(fa: SequenceKOf<A>, f: (A) -> SequenceKOf<B>): SequenceKOf<B> =
            fa.fix().flatMap(f)
    }

    interface StateMonad<S> : Monad<StatePartialOf<S>> {
        override fun <A> unit(a: A): StateOf<S, A> =
            State { s -> a to s}

        override fun <A, B> flatMap(
            fa: StateOf<S,  A>,
            f: (A) -> StateOf<S, B>
        ): StateOf<S, B> =
            fa.fix().flatMap { a -> f(a).fix() }
    }
}