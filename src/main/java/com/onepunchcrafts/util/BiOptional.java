package com.onepunchcrafts.util;/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.AbstractMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A container object which may or may not contain a non-{@code null} left.
 * If a left is present, {@code isPresent()} returns {@code true}. If no
 * left is present, the object is considered <i>empty</i> and
 * {@code isPresent()} returns {@code false}.
 *
 * <p>Additional methods that depend on the presence or absence of a contained
 * left are provided, such as {@link #orElse(Object) orElse()}
 * (returns a default left if no left is present) and
 * {@link #ifPresent(Consumer) ifPresent()} (performs an
 * action if a left is present).
 *
 * <p>This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">left-based</a>
 * class; programmers should treat instances that are
 * {@linkplain #equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may
 * occur. For example, in a future release, synchronization may fail.
 *
 * @param <L> the type of left and <R> right
 * @apiNote {@code BiOptional} is primarily intended for use as a method return type where
 * there is a clear need to represent "no result," and where using {@code null}
 * is likely to cause errors. A variable whose type is {@code BiOptional} should
 * never itself be {@code null}; it should always point to an {@code BiOptional}
 * instance.
 * @since 1.8
 */
public final class BiOptional<L, R> {
    /**
     * Common instance for {@code empty()}.
     */
    private static final BiOptional<?, ?> EMPTY = new BiOptional<>(null, null);

    /**
     * If non-null, the left; if null, indicates no left is present
     */
    private final L left;
    private final R right;
    private final ImmutablePair<L, R> value;

    /**
     * Returns an empty {@code BiOptional} instance.  No left is present for this
     * {@code BiOptional}.
     *
     * @param <L,R> The type of the non-existent left
     * @return an empty {@code BiOptional}
     * @apiNote Though it may be tempting to do so, avoid testing if an object is empty
     * by comparing with {@code ==} or {@code !=} against instances returned by
     * {@code BiOptional.empty()}.  There is no guarantee that it is a singleton.
     * Instead, use {@link #isEmpty()} or {@link #isPresent()}.
     */
    public static <L, R> BiOptional<L, R> empty() {
        @SuppressWarnings("unchecked")
        BiOptional<L, R> t = (BiOptional<L, R>) EMPTY;
        return t;
    }

    /**
     * Constructs an instance with the described left.
     *
     * @param left the left to describe; it's the caller's responsibility to
     *             ensure the left is non-{@code null} unless creating the singleton
     *             instance returned by {@code empty()}.
     */
    private BiOptional(L left, R right) {
        this.left = left;
        this.right = right;
        value = new ImmutablePair<>(left, right);
    }

    /**
     * Returns an {@code BiOptional} describing the given non-{@code null}
     * left.
     *
     * @param left the left to describe, which must be non-{@code null}
     * @param <T>  the type of the left
     * @return an {@code BiOptional} with the left present
     * @throws NullPointerException if left is {@code null}
     */
    public static <L, R> BiOptional<L, R> of(L left, R right) {
        if (left == null && right == null)
            throw new NullPointerException();
        return new BiOptional<>(left, right);
    }

    public static <L, R> BiOptional<L, R> fullOf(L left, R right) {
        if (left == null || right == null)
            throw new NullPointerException();
        return new BiOptional<>(left, right);
    }

    /**
     * Returns an {@code BiOptional} describing the given left, if
     * non-{@code null}, otherwise returns an empty {@code BiOptional}.
     *
     * @param left the possibly-{@code null} left to describe
     * @param <T>  the type of the left
     * @return an {@code BiOptional} with a present left if the specified left
     * is non-{@code null}, otherwise an empty {@code BiOptional}
     */
    @SuppressWarnings("unchecked")
    public static <L, R> BiOptional<L, R> ofNullable(L left, R right) {
        return left == null || right == null ? (BiOptional<L, R>) EMPTY
                : new BiOptional<>(left, right);
    }

    @SuppressWarnings("unchecked")
    public static <L, R> BiOptional<L, R> ofBothNullable(L left, R right) {
        return left == null && right == null ? (BiOptional<L, R>) EMPTY
                : new BiOptional<>(left, right);
    }

    /**
     * If a left is present, returns the left, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @return the non-{@code null} left described by this {@code BiOptional}
     * @throws NoSuchElementException if no left is present
     * @apiNote The preferred alternative to this method is {@link #orElseThrow()}.
     */
    public Pair<L, R> get() {
        return value;
    }

    /**
     * If a left is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a left is present, otherwise {@code false}
     */
    public boolean isPresent() {
        return left != null || right != null;
    }

    public boolean isBothPresent() {
        return left != null && right != null;
    }

    /**
     * If a left is  not present, returns {@code true}, otherwise
     * {@code false}.
     *
     * @return {@code true} if a left is not present, otherwise {@code false}
     * @since 11
     */
    public boolean isEmpty() {
        return left == null || right == null;
    }

    public boolean isBothEmpty() {
        return left == null && right == null;
    }

    /**
     * If a left is present, performs the given action with the left,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a left is present
     * @throws NullPointerException if left is present and the given action is
     *                              {@code null}
     */
    public void ifPresent(BiConsumer<L, R> action) {
        if (left != null || right != null) {
            action.accept(left, right);
        }
    }

    public void ifBothPresent(BiConsumer<? super L, ? super R> action) {
        if (left != null && right != null) {
            action.accept(left, right);
        }
    }

    /**
     * If a left is present, performs the given action with the left,
     * otherwise performs the given empty-based action.
     *
     * @param action      the action to be performed, if a left is present
     * @param emptyAction the empty-based action to be performed, if no left is
     *                    present
     * @throws NullPointerException if a left is present and the given action
     *                              is {@code null}, or no left is present and the given empty-based
     *                              action is {@code null}.
     * @since 9
     */
    public void ifPresentOrElse(BiConsumer<? super L, ? super R> action, Runnable emptyAction) {
        if (left != null || right != null) {
            action.accept(left, right);
        } else {
            emptyAction.run();
        }
    }

    public void ifBothPresentOrElse(BiConsumer<? super L, ? super R> action, Runnable emptyAction) {
        if (left != null && right != null) {
            action.accept(left, right);
        } else {
            emptyAction.run();
        }
    }

    /**
     * If a left is present, and the left matches the given predicate,
     * returns an {@code BiOptional} describing the left, otherwise returns an
     * empty {@code BiOptional}.
     *
     * @param predicate the predicate to apply to a left, if present
     * @return an {@code BiOptional} describing the left of this
     * {@code BiOptional}, if a left is present and the left matches the
     * given predicate, otherwise an empty {@code BiOptional}
     * @throws NullPointerException if the predicate is {@code null}
     */
    public BiOptional<L, R> filter(BiPredicate<? super L, ? super R> predicate) {
        Objects.requireNonNull(predicate);
        if (!isPresent()) {
            return this;
        } else {
            return predicate.test(left, right) ? this : empty();
        }
    }

    public BiOptional<L, R> bothFilter(BiPredicate<? super L, ? super R> predicate) {
        Objects.requireNonNull(predicate);
        if (!isBothPresent()) {
            return this;
        } else {
            return predicate.test(left, right) ? this : empty();
        }
    }

    /**
     * If a left is present, returns an {@code BiOptional} describing (as if by
     * {@link #ofNullable}) the result of applying the given mapping function to
     * the left, otherwise returns an empty {@code BiOptional}.
     *
     * <p>If the mapping function returns a {@code null} result then this method
     * returns an empty {@code BiOptional}.
     *
     * @param mapper the mapping function to apply to a left, if present
     * @param <U>    The type of the left returned from the mapping function
     * @return an {@code BiOptional} describing the result of applying a mapping
     * function to the left of this {@code BiOptional}, if a left is
     * present, otherwise an empty {@code BiOptional}
     * @throws NullPointerException if the mapping function is {@code null}
     * @apiNote This method supports post-processing on {@code BiOptional} values, without
     * the need to explicitly check for a return status.  For example, the
     * following code traverses a stream of URIs, selects one that has not
     * yet been processed, and creates a path from that URI, returning
     * an {@code BiOptional<Path>}:
     *
     * <pre>{@code
     *     BiOptional<Path> p =
     *         uris.stream().filter(uri -> !isProcessedYet(uri))
     *                       .findFirst()
     *                       .map(Paths::get);
     * }</pre>
     * <p>
     * Here, {@code findFirst} returns an {@code BiOptional<URI>}, and then
     * {@code map} returns an {@code BiOptional<Path>} for the desired
     * URI if one exists.
     */
    public BiOptional<L, R> map(BiFunction<? super L, ? super R, Pair<L, R>> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            Pair<L, R> result = mapper.apply(left, right);
            return BiOptional.ofNullable(result.getLeft(), result.getRight());
        }
    }


    /**
     * If a left is present, returns the result of applying the given
     * {@code BiOptional}-bearing mapping function to the left, otherwise returns
     * an empty {@code BiOptional}.
     *
     * <p>This method is similar to {@link #map(Function)}, but the mapping
     * function is one whose result is already an {@code BiOptional}, and if
     * invoked, {@code flatMap} does not wrap it within an additional
     * {@code BiOptional}.
     *
     * @param <U>    The type of left of the {@code BiOptional} returned by the
     *               mapping function
     * @param mapper the mapping function to apply to a left, if present
     * @return the result of applying an {@code BiOptional}-bearing mapping
     * function to the left of this {@code BiOptional}, if a left is
     * present, otherwise an empty {@code BiOptional}
     * @throws NullPointerException if the mapping function is {@code null} or
     *                              returns a {@code null} result
     */
    public <L2, R2> BiOptional<L2, R2> flatMap(BiFunction<? super L, ? super R, BiOptional<? extends L2, ? extends R2>> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            @SuppressWarnings("unchecked")
            BiOptional<L2, R2> result = (BiOptional<L2, R2>) mapper.apply(left, right);
            return Objects.requireNonNull(result);
        }
    }


    /**
     * If a left is present, returns an {@code BiOptional} describing the left,
     * otherwise returns an {@code BiOptional} produced by the supplying function.
     *
     * @param supplier the supplying function that produces an {@code BiOptional}
     *                 to be returned
     * @return returns an {@code BiOptional} describing the left of this
     * {@code BiOptional}, if a left is present, otherwise an
     * {@code BiOptional} produced by the supplying function.
     * @throws NullPointerException if the supplying function is {@code null} or
     *                              produces a {@code null} result
     * @since 9
     */
    public BiOptional<L, R> or(Supplier<? extends BiOptional<? extends L, ? extends R>> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) {
            return this;
        } else {
            @SuppressWarnings("unchecked")
            BiOptional<L, R> r = (BiOptional<L, R>) supplier.get();
            return Objects.requireNonNull(r);
        }
    }

    /**
     * If a left is present, returns a sequential {@link Stream} containing
     * only that left, otherwise returns an empty {@code Stream}.
     *
     * @return the optional left as a {@code Stream}
     * @apiNote This method can be used to transform a {@code Stream} of optional
     * elements to a {@code Stream} of present left elements:
     * <pre>{@code
     *     Stream<BiOptional<T>> os = ..
     *     Stream<T> s = os.flatMap(BiOptional::stream)
     * }</pre>
     * @since 9
     */
    public Stream<?> stream(char c) {
        if (c == 'l') {
            return Stream.ofNullable(left);
        } else {
            return Stream.ofNullable(right);
        }
    }

    /**
     * If a left is present, returns the left, otherwise returns
     * {@code other}.
     *
     * @param other the left to be returned, if no left is present.
     *              May be {@code null}.
     * @return the left, if present, otherwise {@code other}
     */
    public Pair<L, R> orElse(Pair<L, R> other) {
        return left != null || right != null ? value : other;
    }

    public Pair<L, R> orBothElse(Pair<L, R> other) {
        return left != null && right != null ? value : other;
    }

    /**
     * If a left is present, returns the left, otherwise returns the result
     * produced by the supplying function.
     *
     * @param supplier the supplying function that produces a left to be returned
     * @return the left, if present, otherwise the result produced by the
     * supplying function
     * @throws NullPointerException if no left is present and the supplying
     *                              function is {@code null}
     */
    public Pair<? extends L, ? extends R> orElseGet(Supplier<Pair<? extends L, ? extends R>> supplier) {
        return left != null || right != null ? value : supplier.get();
    }

    public Pair<? extends L, ? extends R> orBothElseGet(Supplier<Pair<? extends L, ? extends R>> supplier) {
        return left != null && right != null ? value : supplier.get();
    }

    /**
     * If a left is present, returns the left, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @return the non-{@code null} left described by this {@code BiOptional}
     * @throws NoSuchElementException if no left is present
     * @since 10
     */
    public Pair<L, R> orElseThrow() {
        if (left == null && right == null) {
            throw new NoSuchElementException("No left and right present");
        }
        return value;
    }

    /**
     * If a left is present, returns the left, otherwise throws an exception
     * produced by the exception supplying function.
     *
     * @param <X>               Type of the exception to be thrown
     * @param exceptionSupplier the supplying function that produces an
     *                          exception to be thrown
     * @return the left, if present
     * @throws X                    if no left is present
     * @throws NullPointerException if no left is present and the exception
     *                              supplying function is {@code null}
     * @apiNote A method reference to the exception constructor with an empty argument
     * list can be used as the supplier. For example,
     * {@code IllegalStateException::new}
     */
    public <X extends Throwable> Pair<L, R> orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (left != null && right != null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Indicates whether some other object is "equal to" this {@code BiOptional}.
     * The other object is considered equal if:
     * <ul>
     * <li>it is also an {@code BiOptional} and;
     * <li>both instances have no left present or;
     * <li>the present values are "equal to" each other via {@code equals()}.
     * </ul>
     *
     * @param obj an object to be tested for equality
     * @return {@code true} if the other object is "equal to" this object
     * otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof BiOptional<?, ?> other
                && Objects.equals(value, other.value);
    }

    /**
     * Returns the hash code of the left, if present, otherwise {@code 0}
     * (zero) if no left is present.
     *
     * @return hash code left of the present left or {@code 0} if no left is
     * present
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(left);
    }

    /**
     * Returns a non-empty string representation of this {@code BiOptional}
     * suitable for debugging.  The exact presentation format is unspecified and
     * may vary between implementations and versions.
     *
     * @return the string representation of this instance
     * @implSpec If a left is present the result must include its string representation
     * in the result.  Empty and present {@code BiOptional}s must be unambiguously
     * differentiable.
     */
    @Override
    public String toString() {
        return left != null
                ? ("BiOptional[" + left + ", " + right + "]")
                : "BiOptional.empty";
    }
}
