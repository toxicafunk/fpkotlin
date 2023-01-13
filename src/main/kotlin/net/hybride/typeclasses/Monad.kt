package net.hybride.typeclasses

import arrow.Kind
import arrow.core.ForListK
import arrow.core.ForSequenceK
import arrow.core.ListKOf
import arrow.core.fix
import arrow.core.ListK
import arrow.core.SequenceK
import arrow.core.SequenceKOf

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
            map2(fa, fla) { a, la -> Cons<A>(a, la) }
        }

    fun <A, B> traverse(
        la: List<A>,
        f: (A) -> Kind<F, B>
    ): Kind<F, List<B>> =
        la.foldRight(unit(List.empty<B>())) { a: A, acc: Kind<F, List<B>> ->
            map2(f(a), acc) { b: B, lb: List<B> -> Cons(b, lb) }
        }

    fun <A> replicateM(n: Int, ma: Kind<F, A>): Kind<F, List<A>> =
        when (n) {
            0 -> unit(List.empty())
            else ->
                map2(ma, replicateM(n-1, ma)) { m: A, ml: List<A> -> Cons(m, ml) }
        }

    fun <A> _replicateM(n: Int, ma: Kind<F, A>): Kind<F, List<A>> =
        sequence(List.fill(n, ma))

    fun <A, B> product(
        ma: Kind<F, A>,
        mb: Kind<F, B>
    ): Kind<F, Pair<A, B>> =
        map2(ma, mb) { a, b -> a to b }

    fun <A> filterM(
        ms: List<A>,
        f: (A) -> Kind<F, Boolean>
    ): Kind<F, List<A>> =
        when (ms) {
            is Nil -> unit(Nil)
            is Cons ->
                flatMap(f(ms.head)) { succeed ->
                    if (succeed) map(filterM(ms.tail, f)) { tail ->
                        Cons(ms.head, tail)
                    } else (filterM(ms.tail, f))
                }
        }

    fun <A, B, C> compose(
        f: (A) -> Kind<F, B>,
        g: (B) -> Kind<F, C>
    ): (A) -> Kind<F, C> = { a: A ->
        flatMap(f(a)) { b -> g(b) }
    }
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

    val intStateMonad: StateMonad<Int> = object : StateMonad<Int> {
        override fun <A> unit(a: A): StateOf<Int, A> =
            State { s -> a to s }
        override fun <A, B> flatMap(
            fa: StateOf<Int, A>,
            f: (A) -> StateOf<Int, B>
        ): StateOf<Int, B> =
            fa.fix().flatMap { a -> f(a).fix() }
    }

    interface EitherMonad<E> : Monad<EitherPartialOf<E>> {
        override fun <A> unit(a: A): EitherOf<E, A> =
            Either.unit(a)

        override fun <A, B> flatMap(
            fa: EitherOf<E,  A>,
            f: (A) -> EitherOf<E, B>
        ): EitherOf<E, B> =
            fa.fix().flatMap { a -> f(a).fix() }

    }

    val eitherMonad: EitherMonad<String> = object : EitherMonad<String> {
        override fun <A> unit(a: A): EitherOf<String, A> =
            Either.unit(a)

        override fun <A, B> flatMap(
            fa: EitherOf<String, A>,
            f: (A) -> EitherOf<String, B>
        ): EitherOf<String,B> =
            fa.fix().flatMap { a -> f(a).fix() }
    }
}