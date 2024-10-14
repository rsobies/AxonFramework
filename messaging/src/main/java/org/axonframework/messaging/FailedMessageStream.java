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

import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link MessageStream} implementation that completes exceptionally through the given {@code error}.
 *
 * @param <M> The type of {@link Message} contained in the {@link Entry entries} of this stream.
 * @author Allard Buijze
 * @author Steven van Beelen
 * @since 5.0.0
 */
class FailedMessageStream<M extends Message<?>> implements MessageStream<M> {

    private final Throwable error;

    /**
     * Constructs a {@link MessageStream stream} that will complete exceptionally with the given {@code error}.
     *
     * @param error The {@link Throwable} that caused this {@link MessageStream stream} to complete exceptionally.
     */
    FailedMessageStream(@Nonnull Throwable error) {
        this.error = error;
    }

    @Override
    public CompletableFuture<Entry<M>> asCompletableFuture() {
        return CompletableFuture.failedFuture(error);
    }

    @Override
    public Flux<Entry<M>> asFlux() {
        return Flux.error(error);
    }

    @Override
    public <RM extends Message<?>> MessageStream<RM> map(@Nonnull Function<Entry<M>, Entry<RM>> mapper) {
        //noinspection unchecked
        return (FailedMessageStream<RM>) this;
    }

    @Override
    public <R> CompletableFuture<R> reduce(@Nonnull R identity,
                                           @Nonnull BiFunction<R, Entry<M>, R> accumulator) {
        return CompletableFuture.failedFuture(error);
    }

    @Override
    public MessageStream<M> whenComplete(@Nonnull Runnable completeHandler) {
        return this;
    }
}