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

package org.axonframework.serialization.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.SchemaStore;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.messaging.MetaData;
import org.axonframework.serialization.RevisionResolver;
import org.axonframework.serialization.SerializationException;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.SerializedType;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.axonframework.serialization.UnknownSerializedType;
import org.axonframework.serialization.avro.test.ComplexObject;
import org.axonframework.serialization.json.JacksonSerializer;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.util.Collections.singletonMap;
import static org.axonframework.serialization.avro.AvroUtil.genericData;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test Avro Serializer with default Java-SpecificRecord-Strategy.
 *
 * @author Simon Zambrovski
 * @author Jan Galinski
 * @since 4.11.0
 */
public class AvroSerializerTest {

    private static final String compatibleSchemaWithoutValue2 = "{\n" +
            "  \"name\": \"ComplexObject\",\n" +
            "  \"namespace\": \"org.axonframework.serialization.avro.test\",\n" +
            "  \"type\": \"record\",\n" +
            "  \"fields\": [\n" +
            "    {\n" +
            "      \"name\": \"value1\",\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"value3\",\n" +
            "      \"type\": \"int\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String incompatibleSchema = "{\n" +
            "  \"name\": \"ComplexObject\",\n" +
            "  \"namespace\": \"org.axonframework.serialization.avro.test\",\n" +
            "  \"type\": \"record\",\n" +
            "  \"fields\": [\n" +
            "    {\n" +
            "      \"name\": \"value2\",\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"value3\",\n" +
            "      \"type\": \"int\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";


    private static final String compatibleSchema = "{\n" +
            "  \"name\": \"ComplexObject\",\n" +
            "  \"namespace\": \"org.axonframework.serialization.avro.test\",\n" +
            "  \"type\": \"record\",\n" +
            "  \"fields\": [\n" +
            "    {\n" +
            "      \"name\": \"value1\",\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"value2\",\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"value3\",\n" +
            "      \"type\": \"int\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String compatibleSchemaWithAdditionalField = "{\n" +
            "  \"name\": \"ComplexObject\",\n" +
            "  \"namespace\": \"org.axonframework.serialization.avro.test\",\n" +
            "  \"type\": \"record\",\n" +
            "  \"fields\": [\n" +
            "    {\n" +
            "      \"name\": \"value1\",\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"value2\",\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"value4\",\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"value3\",\n" +
            "      \"type\": \"int\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";


    private static final ComplexObject complexObject = ComplexObject
            .newBuilder()
            .setValue1("foo")
            .setValue2("bar")
            .setValue3(42)
            .build();


    private final RevisionResolver revisionResolver = payloadType -> null;
    private AvroSerializer testSubject;
    private Serializer serializer;
    private SchemaStore.Cache store;

    @BeforeEach
    void setUp() {


        serializer = spy(JacksonSerializer.defaultSerializer());
        store = new SchemaStore.Cache();
        testSubject = AvroSerializer
                .builder()
                .serializerDelegate(serializer)
                .revisionResolver(revisionResolver)
                .schemaStore(store)
                .build();
    }

    @Test
    void testBuilderMandatoryValues() {
        AxonConfigurationException revisionResolverMandatory = assertThrows(AxonConfigurationException.class,
                                                                            () -> AvroSerializer.builder().build());
        assertEquals("RevisionResolver is mandatory", revisionResolverMandatory.getMessage());

        AxonConfigurationException schemaStoreMandatory = assertThrows(AxonConfigurationException.class,
                                                                 () -> AvroSerializer.builder()
                                                                                     .revisionResolver(c -> "")
                                                                                     .build());
        assertEquals("SchemaStore is mandatory", schemaStoreMandatory.getMessage());

        AxonConfigurationException serializerDelegateMandatory = assertThrows(AxonConfigurationException.class,
                                                                        () -> AvroSerializer.builder()
                                                                                            .revisionResolver(c -> "")
                                                                                            .schemaStore(new SchemaStore.Cache())
                                                                                            .build());
        assertEquals("SerializerDelegate is mandatory", serializerDelegateMandatory.getMessage());
    }

    @Test
    void deliverUnknownClassIfTypeIsNotOnClasspath() {
        Class<?> clazz = testSubject.classForType(new SimpleSerializedType("org.acme.Foo", null));
        assertEquals(UnknownSerializedType.class, clazz);
    }

    @Test
    void deliverEmptyType() {
        assertEquals(SimpleSerializedType.emptyType(), testSubject.typeForClass(null));
        assertEquals(SimpleSerializedType.emptyType(), testSubject.typeForClass(Void.class));
    }

    @Test
    void deliverNonEmptyType() {
        assertEquals(new SimpleSerializedType(String.class.getCanonicalName(),
                                              revisionResolver.revisionOf(String.class)),
                     testSubject.typeForClass(String.class));
    }


    @Test
    void canSerialize() {
        assertTrue(testSubject.canSerializeTo(byte[].class));
        assertTrue(testSubject.canSerializeTo(GenericRecord.class));

        assertTrue(testSubject.canSerializeTo(String.class));
        assertTrue(testSubject.canSerializeTo(InputStream.class));
        assertFalse(testSubject.canSerializeTo(Integer.class));
    }

    @Test
    void serializeMetaDataByDelegate() {

        MetaData original = MetaData.from(singletonMap("test", "test"));
        SerializedObject<byte[]> serialized = testSubject.serialize(original, byte[].class);
        MetaData actual = testSubject.deserialize(serialized);

        assertNotNull(actual);
        assertEquals("test", actual.get("test"));
        assertEquals(1, actual.size());

        verify(serializer).serialize(original, byte[].class);
        verify(serializer).deserialize(serialized);
    }

    @Test
    void serializeNull() {
        SerializedObject<byte[]> serialized = testSubject.serialize(null, byte[].class);
        assertEquals(SerializedType.emptyType(), serialized.getType());
        assertArrayEquals("null".getBytes(StandardCharsets.UTF_8), serialized.getData());
        verify(serializer).serialize(null, byte[].class);
    }

    @Test
    void deserializeEmptyBytes() {
        assertEquals(Void.class, testSubject.classForType(SerializedType.emptyType()));
        assertNull(testSubject.deserialize(
                new SimpleSerializedObject<>(new byte[0], byte[].class, SerializedType.emptyType())
        ));
    }

    @Test
    void serializeAndDeserializeComplexObject() {
        store.addSchema(ComplexObject.getClassSchema());

        SerializedObject<byte[]> serialized = testSubject.serialize(complexObject, byte[].class);
        assertEquals(serialized.getType().getName(), ComplexObject.class.getCanonicalName());


        ComplexObject deserialized = testSubject.deserialize(serialized);
        assertEquals(complexObject, deserialized);
    }

    @Test
    void serializeFromCompatibleObjectAndDeserialize() {
        store.addSchema(ComplexObject.getClassSchema());
        Schema schema2 = new Schema.Parser().parse(compatibleSchema);
        store.addSchema(schema2);
        GenericData.Record record = new GenericData.Record(schema2);
        record.put("value1", complexObject.getValue1());
        record.put("value2", complexObject.getValue2());
        record.put("value3", complexObject.getValue3());

        byte[] encodedBytes = genericRecordToByteArray(record);

        SerializedObject<byte[]> serialized = createSerializedObject(encodedBytes,
                                                                     ComplexObject.class.getCanonicalName());
        assertEquals(serialized.getType().getName(), ComplexObject.class.getCanonicalName());

        ComplexObject deserialized = testSubject.deserialize(serialized);
        assertEquals(complexObject, deserialized);
    }

    @Test
    void deserializeFromGenericRecord() {
        store.addSchema(ComplexObject.getClassSchema());
        Schema schema2 = new Schema.Parser().parse(compatibleSchema);
        store.addSchema(schema2);
        GenericData.Record record = new GenericData.Record(schema2);
        record.put("value1", complexObject.getValue1());
        record.put("value2", complexObject.getValue2());
        record.put("value3", complexObject.getValue3());


        SerializedObject<GenericRecord> serialized = createSerializedObject(record);

        ComplexObject deserialized = testSubject.deserialize(serialized);
        assertEquals(complexObject, deserialized);
    }

    @Test
    void serializeFromCompatibleWithAdditionalIgnoredFieldObjectAndDeserialize() {
        store.addSchema(ComplexObject.getClassSchema());
        Schema schema2 = new Schema.Parser().parse(compatibleSchemaWithAdditionalField);
        store.addSchema(schema2);
        GenericData.Record record = new GenericData.Record(schema2);
        record.put("value1", complexObject.getValue1());
        record.put("value2", complexObject.getValue2());
        record.put("value3", complexObject.getValue3());
        record.put("value4", "ignored value");

        byte[] encodedBytes = genericRecordToByteArray(record);

        SerializedObject<byte[]> serialized = createSerializedObject(encodedBytes,
                                                                     ComplexObject.class.getCanonicalName());
        assertEquals(serialized.getType().getName(), ComplexObject.class.getCanonicalName());

        ComplexObject deserialized = testSubject.deserialize(serialized);
        assertEquals(complexObject, deserialized);
    }

    @Test
    void serializeFromCompatibleSchemaAndDeserializeUsingDefault() {
        store.addSchema(ComplexObject.getClassSchema());
        Schema schema2 = new Schema.Parser().parse(compatibleSchemaWithoutValue2);
        store.addSchema(schema2);
        GenericData.Record record = new GenericData.Record(schema2);
        record.put("value1", complexObject.getValue1());
        record.put("value3", complexObject.getValue3());

        byte[] encodedBytes = genericRecordToByteArray(record);

        SerializedObject<byte[]> serialized = createSerializedObject(encodedBytes,
                                                                     ComplexObject.class.getCanonicalName());
        assertEquals(serialized.getType().getName(), ComplexObject.class.getCanonicalName());

        ComplexObject deserialized = testSubject.deserialize(serialized);
        assertEquals("default value", deserialized.getValue2());
    }

    @Test
    void failsWhenSerializeFromIncompatibleSchemaAndDeserialize() {
        store.addSchema(ComplexObject.getClassSchema());
        Schema writerSchema = new Schema.Parser().parse(incompatibleSchema);
        store.addSchema(writerSchema);
        GenericData.Record record = new GenericData.Record(writerSchema);
        record.put("value2", complexObject.getValue1());
        record.put("value3", complexObject.getValue3());

        byte[] encodedBytes = genericRecordToByteArray(record);

        SerializedObject<byte[]> serialized = createSerializedObject(encodedBytes,
                                                                     ComplexObject.class.getCanonicalName());
        assertEquals(serialized.getType().getName(), ComplexObject.class.getCanonicalName());


        SerializationException exception = assertThrows(SerializationException.class,
                                                        () -> testSubject.deserialize(serialized));
        assertEquals(exception.getMessage(),
                     AvroUtil.createExceptionFailedToDeserialize(ComplexObject.class,
                                                                 ComplexObject.getClassSchema(),
                                                                 writerSchema,
                                                                 null).getMessage());
    }

    @Test
    void failToDeserializeIfClassIsNotAvailable() {
        Schema writerSchema = new Schema.Parser().parse(incompatibleSchema);
        store.addSchema(writerSchema);
        GenericData.Record record = new GenericData.Record(writerSchema);
        record.put("value2", complexObject.getValue1());
        record.put("value3", complexObject.getValue3());

        SimpleSerializedObject<GenericRecord> object = new SimpleSerializedObject<>(
                record,
                GenericRecord.class,
                new SimpleSerializedType("org.acme.Foo", null));
        Object deserialized = testSubject.deserialize(object);
        assertEquals(UnknownSerializedType.class, deserialized.getClass());
    }


    private static byte[] genericRecordToByteArray(GenericRecord genericRecord) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BinaryMessageEncoder<GenericRecord> encoder = new BinaryMessageEncoder<>(genericData,
                                                                                     genericRecord.getSchema());
            encoder.encode(genericRecord, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SerializedObject<byte[]> createSerializedObject(byte[] payload, String objectType) {
        return new SimpleSerializedObject<>(
                payload,
                byte[].class,
                new SimpleSerializedType(objectType, null));
    }

    private static SerializedObject<GenericRecord> createSerializedObject(GenericRecord record) {
        return new SimpleSerializedObject<>(
                record,
                GenericRecord.class,
                new SimpleSerializedType(record.getSchema().getFullName(), null));
    }
}
