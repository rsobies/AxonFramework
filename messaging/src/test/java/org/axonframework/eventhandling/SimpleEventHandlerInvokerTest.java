/*
 * Copyright (c) 2010-2021. Axon Framework
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

package org.axonframework.eventhandling;

import org.axonframework.common.AxonConfigurationException;
import org.axonframework.eventhandling.async.SequencingPolicy;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.List;

import static org.axonframework.utils.EventTestUtils.createEvent;
import static org.axonframework.utils.EventTestUtils.createEvents;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class validating the {@link SimpleEventHandlerInvoker}.
 *
 * @author Rene de Waele
 */
class SimpleEventHandlerInvokerTest {

    private static final Object NO_RESET_PAYLOAD = null;
    private static final String PROCESSING_GROUP = "processingGroup";

    private EventMessageHandler mockHandler1;
    private EventMessageHandler mockHandler2;

    private SimpleEventHandlerInvoker testSubject;

    @BeforeEach
    void setUp() {
        mockHandler1 = mock(EventMessageHandler.class);
        mockHandler2 = mock(EventMessageHandler.class);
        testSubject = SimpleEventHandlerInvoker.builder()
                                               .eventHandlers("test", mockHandler1, mockHandler2)
                                               .processingGroup(PROCESSING_GROUP)
                                               .build();
    }

    @Test
    void testSingleEventPublication() throws Exception {
        EventMessage<?> event = createEvent();

        testSubject.handle(event, Segment.ROOT_SEGMENT);

        InOrder inOrder = inOrder(mockHandler1, mockHandler2);
        inOrder.verify(mockHandler1).handle(event);
        inOrder.verify(mockHandler2).handle(event);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void testRepeatedEventPublication() throws Exception {
        List<? extends EventMessage<?>> events = createEvents(2);

        for (EventMessage<?> event : events) {
            testSubject.handle(event, Segment.ROOT_SEGMENT);
        }

        InOrder inOrder = inOrder(mockHandler1, mockHandler2);
        inOrder.verify(mockHandler1).handle(events.get(0));
        inOrder.verify(mockHandler2).handle(events.get(0));
        inOrder.verify(mockHandler1).handle(events.get(1));
        inOrder.verify(mockHandler2).handle(events.get(1));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void testHandleWrapsExceptionInEventExecutionException() throws Exception {
        // given...
        ListenerInvocationErrorHandler errorHandler = mock(ListenerInvocationErrorHandler.class);
        //noinspection unchecked
        SequencingPolicy<EventMessage<?>> sequencingPolicy = mock(SequencingPolicy.class);

        SimpleEventHandlerInvoker customTestSubject =
                SimpleEventHandlerInvoker.builder()
                                         .eventHandlers(mockHandler1)
                                         .listenerInvocationErrorHandler(errorHandler)
                                         .sequencingPolicy(sequencingPolicy)
                                         .processingGroup(PROCESSING_GROUP)
                                         .build();

        EventMessage<?> testEvent = createEvent();

        RuntimeException expectedException = new RuntimeException("some-exception");
        String expectedSequenceIdentifier = "sequenceIdentifier";

        when(mockHandler1.handle(testEvent)).thenThrow(expectedException);
        when(sequencingPolicy.getSequenceIdentifierFor(testEvent)).thenReturn(expectedSequenceIdentifier);

        // when...
        customTestSubject.handle(testEvent, Segment.ROOT_SEGMENT);

        // then...
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(errorHandler).onError(exceptionCaptor.capture(), eq(testEvent), eq(mockHandler1));

        Exception result = exceptionCaptor.getValue();

        assertTrue(result instanceof EventExecutionException);
        EventExecutionException executionException = ((EventExecutionException) result);
        assertEquals(expectedSequenceIdentifier, executionException.getSequenceIdentifier());
        assertEquals(PROCESSING_GROUP, executionException.getProcessingGroup());
        assertEquals(expectedException, executionException.getCause());
    }

    @Test
    void testPerformReset() {
        testSubject.performReset();

        verify(mockHandler1).prepareReset(NO_RESET_PAYLOAD);
        verify(mockHandler2).prepareReset(NO_RESET_PAYLOAD);
    }

    @Test
    void testPerformResetWithResetContext() {
        String resetContext = "reset-context";

        testSubject.performReset(resetContext);

        verify(mockHandler1).prepareReset(eq(resetContext));
        verify(mockHandler2).prepareReset(eq(resetContext));
    }

    @Test
    void testBuildWithNullProcessingGroupThrowsAxonConfigurationException() {
        SimpleEventHandlerInvoker.Builder testSubject = SimpleEventHandlerInvoker.builder();

        assertThrows(AxonConfigurationException.class, () -> testSubject.processingGroup(null));
    }

    @Test
    void testBuildWithEmptyProcessingGroupThrowsAxonConfigurationException() {
        SimpleEventHandlerInvoker.Builder testSubject = SimpleEventHandlerInvoker.builder();

        assertThrows(AxonConfigurationException.class, () -> testSubject.processingGroup(""));
    }

    @Test
    void testBuildWithoutProcessingGroupThrowsAxonConfigurationException() {
        SimpleEventHandlerInvoker.Builder testSubject = SimpleEventHandlerInvoker.builder();

        assertThrows(AxonConfigurationException.class, testSubject::build);
    }
}
