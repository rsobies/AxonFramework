/*
 * Copyright (c) 2010-2024. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.messaging;

import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link MessageStream} implementation that completes exceptionally through the given {@code error}.
 *
 * @param <E> The type of {@link Message} carried in this stream.
 * @author Allard Buijze
 * @author Steven van Beelen
 * @since 5.0.0
 */
class FailedMessageStream<E> implements MessageStream<E> {

    private final Throwable error;

    /**
     * Constructs a {@link FailedMessageStream} that will complete exceptionally with the given {@code error}.
     *
     * @param error The {@link Throwable} that caused this {@link MessageStream} to complete exceptionally.
     */
    FailedMessageStream(@NotNull Throwable error) {
        this.error = error;
    }

    @Override
    public CompletableFuture<E> asCompletableFuture() {
        return CompletableFuture.failedFuture(error);
    }

    @Override
    public Flux<E> asFlux() {
        return Flux.error(error);
    }

    @Override
    public <R> MessageStream<R> map(@NotNull Function<E, R> mapper) {
        //noinspection unchecked
        return (FailedMessageStream<R>) this;
    }

    @Override
    public <R> CompletableFuture<R> reduce(@NotNull R identity,
                                           @NotNull BiFunction<R, E, R> accumulator) {
        return CompletableFuture.failedFuture(error);
    }

    @Override
    public MessageStream<E> whenComplete(Runnable completeHandler) {
        return this;
    }
}
