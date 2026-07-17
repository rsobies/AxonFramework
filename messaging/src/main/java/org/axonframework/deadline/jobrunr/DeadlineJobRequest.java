/*
 * Copyright (c) 2010-2025. Axon Framework
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

package org.axonframework.deadline.jobrunr;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import javax.annotation.Nonnull;

/**
 * {@link JobRequest} carrying the details of a deadline scheduled through {@link JobRunrDeadlineManager}.
 * <p>
 * Unlike a job lambda, a {@link JobRequest} is resolved by JobRunr through {@link #getJobRequestHandler()}
 * and reflection on named, stable classes, rather than by inspecting the bytecode of a synthetic lambda
 * class. This makes it safe to use under GraalVM native image, where lambda classes don't retain the
 * metadata JobRunr's default {@code JobDetailsGenerator} needs.
 *
 * @author Tom de Backer
 * @author Gerard Klijs
 * @since 4.13.1
 */
public class DeadlineJobRequest implements JobRequest {

    private String serializedDeadlineDetails;
    private String deadlineId;

    private DeadlineJobRequest() {
        //private no-args constructor needed for deserialization
    }

    /**
     * Creates a new {@link DeadlineJobRequest}.
     *
     * @param serializedDeadlineDetails The serialized {@link DeadlineDetails} needed to execute the deadline.
     * @param deadlineId                The {@link String} representation of the deadline's {@link java.util.UUID}.
     */
    public DeadlineJobRequest(@Nonnull String serializedDeadlineDetails, @Nonnull String deadlineId) {
        this.serializedDeadlineDetails = serializedDeadlineDetails;
        this.deadlineId = deadlineId;
    }

    /**
     * Returns the serialized {@link DeadlineDetails} needed to execute the deadline.
     *
     * @return The serialized {@link DeadlineDetails} needed to execute the deadline.
     */
    public String getSerializedDeadlineDetails() {
        return serializedDeadlineDetails;
    }

    /**
     * Returns the {@link String} representation of the deadline's {@link java.util.UUID}.
     *
     * @return The {@link String} representation of the deadline's {@link java.util.UUID}.
     */
    public String getDeadlineId() {
        return deadlineId;
    }

    @Override
    public Class<DeadlineJobRequestHandler> getJobRequestHandler() {
        return DeadlineJobRequestHandler.class;
    }
}
