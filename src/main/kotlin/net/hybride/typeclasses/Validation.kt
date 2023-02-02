package net.hybride.typeclasses

import arrow.Kind
import arrow.higherkind

@higherkind
sealed class Validation<out E, out A> : ValidationOf<E, A>

data class Failure<E>(
    val head: E,
    val tail: List<E> = List.empty()
) : Validation<E, Nothing>()

data class Success<A>(val a: A) : Validation<Nothing, A>()

fun <E> validation(): Applicative2<ValidationPartialOf<E>> = object : Applicative2<ValidationPartialOf<E>> {
    override fun <A, B> apply(
        fab: ValidationOf<E, (A) -> B>,
        fa: ValidationOf<E, A>
    ): ValidationOf<E, B> =
        map2(fab, fa) { f, a -> f(a) }

    override fun <A> unit(a: A): ValidationOf<E, A> =
        Success(a)

    override fun <A, B> map(
        fa: ValidationOf<E, A>,
        f: (A) -> B
    ): ValidationOf<E, B> =
        apply(unit(f), fa)

    override fun <A, B, C> map2(
        fa: ValidationOf<E, A>,
        fb: ValidationOf<E, B>,
        f: (A, B) -> C
    ): ValidationOf<E, C> {
        val va = fa.fix()
        val vb = fb.fix()
        return when(va) {
            is Success -> when (vb) {
                is Success -> Success(f(va.a, vb.a))
                is Failure -> vb
            }
            is Failure -> when (vb) {
                is Success -> va
                is Failure -> Failure(va.head, Cons(vb.head, List.append(va.tail, vb.tail))
            }
        }
    }

}