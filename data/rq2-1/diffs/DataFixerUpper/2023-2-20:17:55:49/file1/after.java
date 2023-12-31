// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.
package com.mojang.datafixers.functions;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.Optic;
import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.types.Func;
import com.mojang.datafixers.types.constant.EmptyPart;
import com.mojang.datafixers.types.families.Algebra;
import com.mojang.datafixers.types.families.ListAlgebra;
import com.mojang.datafixers.types.families.RecursiveTypeFamily;
import com.mojang.datafixers.types.templates.Product;
import com.mojang.datafixers.types.templates.Sum;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PointFreeRule {
    <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr);

    default <A> PointFree<A> rewriteOrNop(final PointFree<A> expr) {
        return DataFixUtils.orElse(rewrite(expr), expr);
    }

    static PointFreeRule nop() {
        return Nop.INSTANCE;
    }

    enum Nop implements PointFreeRule, Supplier<PointFreeRule> {
        INSTANCE;

        @Override
        public <A> Optional<PointFree<A>> rewrite(final PointFree<A> expr) {
            return Optional.of(expr);
        }

        @Override
        public PointFreeRule get() {
            return this;
        }
    }

    enum BangEta implements PointFreeRule {
        INSTANCE;

        @SuppressWarnings("unchecked")
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            if (expr instanceof Bang) {
                return Optional.empty();
            }
            if (expr.type() instanceof Func<?, ?> func) {
                if (func.second() instanceof EmptyPart) {
                    return Optional.of((PointFree<A>) Functions.bang(func.first()));
                }
            }
            return Optional.empty();
        }
    }

    enum CompAssocLeft implements PointFreeRule {
        INSTANCE;

        // f ◦ (g ◦ h) -> (f ◦ g) ◦ h
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            if (expr instanceof Comp<?, ?, ?>) {
                final Comp<?, ?, ?> comp2 = (Comp<?, ?, ?>) expr;
                final PointFree<? extends Function<?, ?>> second = comp2.second;
                if (second instanceof Comp<?, ?, ?>) {
                    final Comp<?, ?, ?> comp1 = (Comp<?, ?, ?>) second;
                    return swap(comp1, comp2);
                }
            }
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        private static <A, B, C, D, E> Optional<PointFree<E>> swap(final Comp<A, B, C> comp1, final Comp<?, ?, D> comp2raw) {
            final Comp<A, C, D> comp2 = (Comp<A, C, D>) comp2raw;
            return Optional.of((PointFree<E>) new Comp<>(new Comp<>(comp2.first, comp1.first), comp1.second));
        }
    }

    enum CompAssocRight implements PointFreeRule {
        INSTANCE;

        // (f ◦ g) ◦ h -> f ◦ (g ◦ h)
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            if (expr instanceof Comp<?, ?, ?>) {
                final Comp<?, ?, ?> comp1 = (Comp<?, ?, ?>) expr;
                final PointFree<? extends Function<?, ?>> first = comp1.first;
                if (first instanceof Comp<?, ?, ?>) {
                    final Comp<?, ?, ?> comp2 = (Comp<?, ?, ?>) first;
                    return swap(comp1, comp2);
                }
            }
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        private static <A, B, C, D, E> Optional<PointFree<E>> swap(final Comp<A, B, D> comp1, final Comp<?, C, ?> comp2raw) {
            final Comp<B, C, D> comp2 = (Comp<B, C, D>) comp2raw;
            return Optional.of((PointFree<E>) new Comp<>(comp2.first, new Comp<>(comp2.second, comp1.second)));
        }
    }

    enum LensAppId implements PointFreeRule {
        INSTANCE;

        // (ap lens id) -> id
        @SuppressWarnings("unchecked")
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            if (expr instanceof Apply<?, A> apply) {
                final PointFree<? extends Function<?, A>> func = apply.func;
                if (func instanceof ProfunctorTransformer<?, ?, ?, ?> && Functions.isId(apply.arg)) {
                    return Optional.of((PointFree<A>) Functions.id(((Func<?, ?>) apply.type()).first()));
                }
            }
            return Optional.empty();
        }
    }

    enum AppNest implements PointFreeRule {
        INSTANCE;

        // (ap f1 (ap f2 arg)) -> (ap (f1 ◦ f2) arg)
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            if (expr instanceof final Apply<?, ?> applyFirst) {
                if (applyFirst.arg instanceof final Apply<?, ?> applySecond) {
                    return Optional.of(Functions.app(compose(applyFirst.func, applySecond.func), applySecond.arg));
                }
            }
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        private <A, B, C> PointFree<Function<A, C>> compose(final PointFree<? extends Function<?, ?>> first, final PointFree<? extends Function<?, ?>> second) {
            // Optic[o1] ◦ Optic[o2] -> Optic[o1 ◦ o2]
            if (first instanceof ProfunctorTransformer<?, ?, ?, ?> firstOptic && second instanceof ProfunctorTransformer<?, ?, ?, ?> secondOptic) {
                return cap(firstOptic, secondOptic);
            }
            return Functions.comp((PointFree<Function<B, C>>) first, (PointFree<Function<A, B>>) second);
        }

        @SuppressWarnings("unchecked")
        private <R, X, Y, S, T, A, B> R cap(final ProfunctorTransformer<X, Y, ?, ?> first, final ProfunctorTransformer<S, T, A, B> second) {
            final ProfunctorTransformer<X, Y, S, T> firstCasted = (ProfunctorTransformer<X, Y, S, T>) first;
            final Func<Function<S, T>, Function<X, Y>> firstType = (Func<Function<S, T>, Function<X, Y>>) firstCasted.type;
            final Func<Function<A, B>, Function<S, T>> secondType = (Func<Function<A, B>, Function<S, T>>) second.type;
            return (R) Functions.profunctorTransformer(firstCasted.optic.compose(second.optic), DSL.func(secondType.first(), firstType.second()));
        }
    }

    interface CompRewrite extends PointFreeRule {
        static CompRewrite choice(final CompRewrite... rules) {
            return (first, second) -> {
                for (final CompRewrite rule : rules) {
                    final Optional<? extends PointFree<? extends Function<?, ?>>> view = rule.doRewrite(first, second);
                    if (view.isPresent()) {
                        return view;
                    }
                }
                return Optional.empty();
            };
        }

        @Override
        default <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            if (expr instanceof Comp<?, ?, ?>) {
                final Comp<?, ?, ?> comp = (Comp<?, ?, ?>) expr;
                final PointFree<? extends Function<?, ?>> first = comp.first;
                final PointFree<? extends Function<?, ?>> second = comp.second;
                // Rewrite f ◦ g in (_ ◦ f) ◦ g
                if (first instanceof Comp<?, ?, ?>) {
                    final Comp<?, ?, ?> firstComp = (Comp<?, ?, ?>) first;
                    return doRewrite(firstComp.second, comp.second).map(result -> {
                        if (result instanceof Comp<?, ?, ?>) {
                            final Comp<?, ?, ?> resultComp = (Comp<?, ?, ?>) result;
                            return buildLeftNested(resultComp, firstComp);
                        }
                        return buildRight(firstComp, result);
                    });
                }
                // Rewrite f ◦ g in f ◦ (g ◦ _)
                if (second instanceof Comp<?, ?, ?>) {
                    final Comp<?, ?, ?> secondComp = (Comp<?, ?, ?>) second;
                    return doRewrite(comp.first, secondComp.first).map(result -> {
                        if (result instanceof Comp<?, ?, ?>) {
                            final Comp<?, ?, ?> resultComp = (Comp<?, ?, ?>) result;
                            return buildRightNested(secondComp, resultComp);
                        }
                        return buildLeft(result, secondComp);
                    });
                }
                // Rewrite f ◦ g
                return (Optional<? extends PointFree<A>>) doRewrite(comp.first, comp.second);
            }
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        static <A, B, C, D> PointFree<D> buildLeft(final PointFree<?> result, final Comp<A, B, C> comp) {
            return (PointFree<D>) new Comp<>((PointFree<Function<B, C>>) result, comp.second);
        }

        @SuppressWarnings("unchecked")
        static <A, B, C, D> PointFree<D> buildRight(final Comp<A, B, C> comp, final PointFree<?> result) {
            return (PointFree<D>) new Comp<>(comp.first, (PointFree<Function<A, B>>) result);
        }

        @SuppressWarnings("unchecked")
        static <A, B, C, D, E> PointFree<E> buildLeftNested(final Comp<A, B, C> comp1, final Comp<?, ?, D> comp2raw) {
            final Comp<A, C, D> comp2 = (Comp<A, C, D>) comp2raw;
            return (PointFree<E>) new Comp<>(new Comp<>(comp2.first, comp1.first), comp1.second);
        }

        @SuppressWarnings("unchecked")
        static <A, B, C, D, E> PointFree<E> buildRightNested(final Comp<A, B, D> comp1, final Comp<?, C, ?> comp2raw) {
            final Comp<B, C, D> comp2 = (Comp<B, C, D>) comp2raw;
            return (PointFree<E>) new Comp<>(comp2.first, new Comp<>(comp2.second, comp1.second));
        }

        Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(PointFree<? extends Function<?, ?>> first, PointFree<? extends Function<?, ?>> second);
    }

    enum SortProj implements CompRewrite {
        INSTANCE;

        // (ap π1 f)◦(ap π2 g) -> (ap π2 g)◦(ap π1 f)
        @Override
        public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(final PointFree<? extends Function<?, ?>> first, final PointFree<? extends Function<?, ?>> second) {
            if (first instanceof Apply<?, ?> && second instanceof Apply<?, ?>) {
                final Apply<?, ?> applyFirst = (Apply<?, ?>) first;
                final Apply<?, ?> applySecond = (Apply<?, ?>) second;
                final PointFree<? extends Function<?, ?>> firstFunc = applyFirst.func;
                final PointFree<? extends Function<?, ?>> secondFunc = applySecond.func;
                if (firstFunc instanceof ProfunctorTransformer<?, ?, ?, ?> && secondFunc instanceof ProfunctorTransformer<?, ?, ?, ?>) {
                    final ProfunctorTransformer<?, ?, ?, ?> firstOptic = (ProfunctorTransformer<?, ?, ?, ?>) firstFunc;
                    final ProfunctorTransformer<?, ?, ?, ?> secondOptic = (ProfunctorTransformer<?, ?, ?, ?>) secondFunc;

                    Optic<?, ?, ?, ?, ?> fo = firstOptic.optic;
                    while (fo instanceof Optic.CompositionOptic<?, ?, ?, ?, ?, ?, ?>) {
                        fo = ((Optic.CompositionOptic<?, ?, ?, ?, ?, ?, ?>) fo).outer();
                    }
                    if (!Optics.isProj2(fo)) {
                        return Optional.empty();
                    }

                    Optic<?, ?, ?, ?, ?> so = secondOptic.optic;
                    while (so instanceof Optic.CompositionOptic<?, ?, ?, ?, ?, ?, ?>) {
                        so = ((Optic.CompositionOptic<?, ?, ?, ?, ?, ?, ?>) so).outer();
                    }
                    if (!Optics.isProj1(so)) {
                        return Optional.empty();
                    }

                    return Optional.of(cap(applyFirst, applySecond));
                }
            }
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        private <R, A, A2, B, B2> R cap(final Apply<?, ?> first, final Apply<?, ?> second) {
            final ProfunctorTransformer<Pair<A, B2>, Pair<A2, B2>, A, A2> firstFunc = (ProfunctorTransformer<Pair<A, B2>, Pair<A2, B2>, A, A2>) (Object) first.func;
            final ProfunctorTransformer<Pair<A, B>, Pair<A, B2>, B, B2> secondFunc = (ProfunctorTransformer<Pair<A, B>, Pair<A, B2>, B, B2>) (Object) second.func;
            final PointFree<Function<A, A2>> firstArg = (PointFree<Function<A, A2>>) first.arg;
            final PointFree<Function<B, B2>> secondArg = (PointFree<Function<B, B2>>) second.arg;

            final Func<Pair<A, B2>, Pair<A2, B2>> firstType = (Func<Pair<A, B2>, Pair<A2, B2>>) first.type;
            final Func<Pair<A, B>, Pair<A, B2>> secondType = (Func<Pair<A, B>, Pair<A, B2>>) second.type;
            final Product.ProductType<A, B> input = (Product.ProductType<A, B>) secondType.first();
            final Product.ProductType<A2, B2> output = (Product.ProductType<A2, B2>) firstType.second();

            return (R) new Comp<>(
                new Apply<>(secondFunc.castOuterUnchecked(DSL.func(
                    DSL.func(input.second(), output.second()),
                    DSL.func(DSL.and(output.first(), input.second()), output)
                )), secondArg),
                new Apply<>(firstFunc.castOuterUnchecked(DSL.func(
                    DSL.func(input.first(), output.first()),
                    DSL.func(input, DSL.and(output.first(), input.second()))
                )), firstArg)
            );
        }
    }

    enum SortInj implements CompRewrite {
        INSTANCE;

        // (ap i1 f)◦(ap i2 g) -> (ap i2 g)◦(ap i1 f)
        @Override
        public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(final PointFree<? extends Function<?, ?>> first, final PointFree<? extends Function<?, ?>> second) {
            if (first instanceof Apply<?, ?> && second instanceof Apply<?, ?>) {
                final Apply<?, ?> applyFirst = (Apply<?, ?>) first;
                final Apply<?, ?> applySecond = (Apply<?, ?>) second;
                final PointFree<? extends Function<?, ?>> firstFunc = applyFirst.func;
                final PointFree<? extends Function<?, ?>> secondFunc = applySecond.func;
                if (firstFunc instanceof ProfunctorTransformer<?, ?, ?, ?> && secondFunc instanceof ProfunctorTransformer<?, ?, ?, ?>) {
                    final ProfunctorTransformer<?, ?, ?, ?> firstOptic = (ProfunctorTransformer<?, ?, ?, ?>) firstFunc;
                    final ProfunctorTransformer<?, ?, ?, ?> secondOptic = (ProfunctorTransformer<?, ?, ?, ?>) secondFunc;

                    Optic<?, ?, ?, ?, ?> fo = firstOptic.optic;
                    while (fo instanceof Optic.CompositionOptic<?, ?, ?, ?, ?, ?, ?>) {
                        fo = ((Optic.CompositionOptic<?, ?, ?, ?, ?, ?, ?>) fo).outer();
                    }
                    if (!Optics.isInj2(fo)) {
                        return Optional.empty();
                    }

                    Optic<?, ?, ?, ?, ?> so = secondOptic.optic;
                    while (so instanceof Optic.CompositionOptic<?, ?, ?, ?, ?, ?, ?>) {
                        so = ((Optic.CompositionOptic<?, ?, ?, ?, ?, ?, ?>) so).outer();
                    }
                    if (!Optics.isInj1(so)) {
                        return Optional.empty();
                    }

                    return Optional.of(cap(applyFirst, applySecond));
                }
            }
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        private <R, A, A2, B, B2> R cap(final Apply<?, ?> first, final Apply<?, ?> second) {
            final ProfunctorTransformer<Either<A, B2>, Either<A2, B2>, A, A2> firstFunc = (ProfunctorTransformer<Either<A, B2>, Either<A2, B2>, A, A2>) (Object) first.func;
            final ProfunctorTransformer<Either<A, B>, Either<A, B2>, B, B2> secondFunc = (ProfunctorTransformer<Either<A, B>, Either<A, B2>, B, B2>) (Object) second.func;
            final PointFree<Function<A, A2>> firstArg = (PointFree<Function<A, A2>>) first.arg;
            final PointFree<Function<B, B2>> secondArg = (PointFree<Function<B, B2>>) second.arg;

            final Func<Either<A, B2>, Either<A2, B2>> firstType = (Func<Either<A, B2>, Either<A2, B2>>) first.type;
            final Func<Either<A, B>, Either<A, B2>> secondType = (Func<Either<A, B>, Either<A, B2>>) second.type;
            final Sum.SumType<A, B> input = (Sum.SumType<A, B>) secondType.first();
            final Sum.SumType<A2, B2> output = (Sum.SumType<A2, B2>) firstType.second();

            return (R) new Comp<>(
                new Apply<>(secondFunc.castOuterUnchecked(DSL.func(
                    DSL.func(input.second(), output.second()),
                    DSL.func(DSL.or(output.first(), input.second()), output)
                )), secondArg),
                new Apply<>(firstFunc.castOuterUnchecked(DSL.func(
                    DSL.func(input.first(), output.first()),
                    DSL.func(input, DSL.or(output.first(), input.second()))
                )), firstArg)
            );
        }
    }

    enum LensComp implements CompRewrite {
        INSTANCE;

        // (ap lens f)◦(ap lens g) -> (ap lens (f ◦ g))
        @SuppressWarnings("unchecked")
        @Override
        public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(final PointFree<? extends Function<?, ?>> first, final PointFree<? extends Function<?, ?>> second) {
          if (first instanceof Apply<?, ?> && second instanceof Apply<?, ?>) {
                final Apply<?, ?> applyFirst = (Apply<?, ?>) first;
                final Apply<?, ?> applySecond = (Apply<?, ?>) second;
                final PointFree<? extends Function<?, ?>> firstFunc = applyFirst.func;
                final PointFree<? extends Function<?, ?>> secondFunc = applySecond.func;
                if (firstFunc instanceof ProfunctorTransformer<?, ?, ?, ?> && secondFunc instanceof ProfunctorTransformer<?, ?, ?, ?>) {
                    final ProfunctorTransformer<?, ?, ?, ?> lensPFFirst = (ProfunctorTransformer<?, ?, ?, ?>) firstFunc;
                    final ProfunctorTransformer<?, ?, ?, ?> lensPFSecond = (ProfunctorTransformer<?, ?, ?, ?>) secondFunc;
                    // TODO: better equality - has to be the same lens; find out more about lens profunctor composition
                    if (Objects.equals(lensPFFirst.optic, lensPFSecond.optic)) {
                        return cap(lensPFFirst, lensPFSecond, applyFirst.arg, applySecond.arg);
                    }
                }
            }
            return Optional.empty();
        }

        private <R, A, B, C, S, T, U> Optional<? extends PointFree<R>> cap(final ProfunctorTransformer<S, T, A, B> l1, final ProfunctorTransformer<?, U, ?, C> l2, final PointFree<?> f1, final PointFree<?> f2) {
            return cap2(l1, (ProfunctorTransformer<T, U, B, C>) l2, (PointFree<Function<B, C>>) f1, (PointFree<Function<A, B>>) f2);
        }

        private <R, P extends K2, Proof extends K1, A, B, C, S, T, U> Optional<? extends PointFree<R>> cap2(final ProfunctorTransformer<S, T, A, B> l1, final ProfunctorTransformer<T, U, B, C> l2, final PointFree<Function<B, C>> f1, final PointFree<Function<A, B>> f2) {
            final PointFree<Function<Function<A, C>, Function<S, U>>> lens = (PointFree<Function<Function<A, C>, Function<S, U>>>) (PointFree<?>) l1;
            final PointFree<Function<A, C>> arg = Functions.comp(f1, f2);
            return Optional.of((PointFree<R>) Functions.app(lens, arg));
        }
    }

    enum CataFuseSame implements CompRewrite {
        INSTANCE;

        // (fold g ◦ in) ◦ fold (f ◦ in) -> fold ( g ◦ f ◦ in), <== g ◦ in ◦ fold (f ◦ in) ◦ out == in ◦ fold (f ◦ in) ◦ out ◦ g <== g doesn't touch fold's index
        @SuppressWarnings("unchecked")
        @Override
        public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(final PointFree<? extends Function<?, ?>> first, final PointFree<? extends Function<?, ?>> second) {
            if (first instanceof Fold<?, ?> && second instanceof Fold<?, ?>) {
                // fold (_) ◦ fold (_)
                final Fold<?, ?> firstFold = (Fold<?, ?>) first;
                final Fold<?, ?> secondFold = (Fold<?, ?>) second;
                final RecursiveTypeFamily family = firstFold.aType.family();
                if (firstFold.index == secondFold.index && Objects.equals(family, secondFold.aType.family())) {
                    final RecursiveTypeFamily newFamily = firstFold.bType.family();
                    // same fold
                    final List<RewriteResult<?, ?>> newAlgebra = Lists.newArrayList();

                    // merge where both are touching, id where neither is

                    boolean foundOne = false;
                    for (int i = 0; i < family.size(); i++) {
                        final RewriteResult<?, ?> firstAlgFunc = firstFold.algebra.apply(i);
                        final RewriteResult<?, ?> secondAlgFunc = secondFold.algebra.apply(i);
                        final boolean firstId = firstAlgFunc.view().isNop();
                        final boolean secondId = secondAlgFunc.view().isNop();

                        if (firstId && secondId) {
                            newAlgebra.add(firstAlgFunc);
                        } else if (!foundOne && !firstId && !secondId) {
                            newAlgebra.add(getCompose(firstAlgFunc, secondAlgFunc));
                            foundOne = true;
                        } else {
                            return Optional.empty();
                        }
                    }
                    final Algebra algebra = new ListAlgebra("FusedSame", newAlgebra);
                    return Optional.of(family.fold(algebra, newFamily).apply(firstFold.index).view().function());
                }
            }
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        private <B> RewriteResult<?, ?> getCompose(final RewriteResult<B, ?> firstAlgFunc, final RewriteResult<?, ?> secondAlgFunc) {
            return firstAlgFunc.compose(((RewriteResult<?, B>) secondAlgFunc));
        }
    }

    enum CataFuseDifferent implements CompRewrite {
        INSTANCE;

        // (fold g ◦ in) ◦ fold (f ◦ in) -> fold ( g ◦ f ◦ in), <== g ◦ in ◦ fold (f ◦ in) ◦ out == in ◦ fold (f ◦ in) ◦ out ◦ g <== g doesn't touch fold's index
        @SuppressWarnings("unchecked")
        @Override
        public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(final PointFree<? extends Function<?, ?>> first, final PointFree<? extends Function<?, ?>> second) {
            if (first instanceof Fold<?, ?> && second instanceof Fold<?, ?>) {
                // fold (_) ◦ fold (_)
                final Fold<?, ?> firstFold = (Fold<?, ?>) first;
                final Fold<?, ?> secondFold = (Fold<?, ?>) second;
                final RecursiveTypeFamily family = firstFold.aType.family();
                if (firstFold.index == secondFold.index && Objects.equals(family, secondFold.aType.family())) {
                    final RecursiveTypeFamily newFamily = firstFold.bType.family();
                    // same fold
                    final List<RewriteResult<?, ?>> newAlgebra = Lists.newArrayList();

                    final BitSet firstModifies = new BitSet(family.size());
                    final BitSet secondModifies = new BitSet(family.size());
                    // mark all types that corresponding function modifies
                    for (int i = 0; i < family.size(); i++) {
                        final RewriteResult<?, ?> firstAlgFunc = firstFold.algebra.apply(i);
                        final RewriteResult<?, ?> secondAlgFunc = secondFold.algebra.apply(i);
                        final boolean firstId = firstAlgFunc.view().isNop();
                        final boolean secondId = secondAlgFunc.view().isNop();
                        if (!firstId && !secondId) {
                            return Optional.empty();
                        }
                        firstModifies.set(i, !firstId);
                        secondModifies.set(i, !secondId);
                    }

                    // if the left function doesn't care about the right modifications, and converse is correct, the merge is valid
                    // TODO: verify that this is enough
                    for (int i = 0; i < family.size(); i++) {
                        final RewriteResult<?, ?> firstAlgFunc = firstFold.algebra.apply(i);
                        final RewriteResult<?, ?> secondAlgFunc = secondFold.algebra.apply(i);
                        if (firstAlgFunc.recData().intersects(secondModifies) || secondAlgFunc.recData().intersects(firstModifies)) {
                            // outer function depends on the result of the inner one
                            return Optional.empty();
                        }
                        if (firstAlgFunc.view().isNop()) {
                            newAlgebra.add(secondAlgFunc);
                        } else {
                            newAlgebra.add(firstAlgFunc);
                        }
                    }
                    // have new algebra - make a new fold

                    final Algebra algebra = new ListAlgebra("FusedDifferent", newAlgebra);
                    return Optional.of(family.fold(algebra, newFamily).apply(firstFold.index).view().function());
                }
            }
            return Optional.empty();
        }
    }

    /*final class ReflexCata implements Rule {
        @SuppressWarnings("unchecked")
        @Override
        public <A> Optional<? extends PF<A>> rewrite(final Type<A> type, final PF<A> expr) {
            if (type instanceof Type.Func<?, ?> && expr instanceof PF.Cata<?, ?, ?, ?>) {
                final Type.Func<?, ?> funcType = (Type.Func<?, ?>) type;
                final PF.Cata<?, ?, ?, ?> cata = (PF.Cata<?, ?, ?, ?>) expr;
                // TODO better equality
                if (Objects.equals(cata.alg, PF.genBF(cata.family.to)) && Objects.equals(funcType.first, funcType.second)) {
                    return Optional.of((PF<A>) PF.id());
                }
            }
            return Optional.empty();
        }
    }*/

    static PointFreeRule seq(final PointFreeRule... rules) {
        return new Seq(rules);
    }

    record Seq(PointFreeRule[] rules) implements PointFreeRule {
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            PointFree<A> result = expr;
            for (final PointFreeRule rule : rules) {
                result = rule.rewriteOrNop(result);
            }
            return Optional.of(result);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            return obj instanceof final Seq that && Arrays.equals(rules, that.rules);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(rules);
        }
    }

    static PointFreeRule choice(final PointFreeRule... rules) {
        if (rules.length == 1) {
            return rules[0];
        } else if (rules.length == 2) {
            return new Choice2(rules[0], rules[1]);
        }
        return new Choice(rules);
    }

    record Choice2(PointFreeRule first, PointFreeRule second) implements PointFreeRule {
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            final Optional<? extends PointFree<A>> view = first.rewrite(expr);
            if (view.isPresent()) {
                return view;
            }
            return second.rewrite(expr);
        }
    }

    record Choice(PointFreeRule[] rules) implements PointFreeRule {
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            for (final PointFreeRule rule : rules) {
                final Optional<? extends PointFree<A>> view = rule.rewrite(expr);
                if (view.isPresent()) {
                    return view;
                }
            }
            return Optional.empty();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            return obj instanceof final Choice that && Arrays.equals(rules, that.rules);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(rules);
        }
    }

    static PointFreeRule all(final PointFreeRule rule) {
        return new All(rule);
    }

    static PointFreeRule one(final PointFreeRule rule) {
        return new One(rule);
    }

    static PointFreeRule once(final PointFreeRule rule) {
        return new Once(rule);
    }

    record Once(PointFreeRule rule) implements PointFreeRule {
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            final Optional<? extends PointFree<A>> view = rule.rewrite(expr);
            if (view.isPresent()) {
                return view;
            }
            return expr.one(this);
        }
    }

    static PointFreeRule many(final PointFreeRule rule) {
        return new Many(rule);
    }

    static PointFreeRule everywhere(final PointFreeRule topDown, final PointFreeRule bottomUp) {
        return new Everywhere(topDown, bottomUp);
    }

    record Everywhere(PointFreeRule topDown, PointFreeRule bottomUp) implements PointFreeRule {
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            final PointFree<A> topDown = this.topDown.rewriteOrNop(expr);
            final PointFree<A> all = DataFixUtils.orElse(topDown.all(this), topDown);
            final PointFree<A> bottomUp = this.bottomUp.rewriteOrNop(all);
            return Optional.of(bottomUp);
        }
    }

    record All(PointFreeRule rule) implements PointFreeRule {
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            return expr.all(rule);
        }
    }

    record One(PointFreeRule rule) implements PointFreeRule {
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            return expr.one(rule);
        }
    }

    record Many(PointFreeRule rule) implements PointFreeRule {
        @Override
        public <A> Optional<? extends PointFree<A>> rewrite(final PointFree<A> expr) {
            Optional<? extends PointFree<A>> result = Optional.of(expr);
            while (true) {
                final Optional<? extends PointFree<A>> newResult = result.flatMap(rule::rewrite);
                if (newResult.isEmpty()) {
                    return result;
                }
                result = newResult;
            }
        }
    }
}
