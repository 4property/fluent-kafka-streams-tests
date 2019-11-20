/*
 * The MIT License
 *
 * Copyright (c) 2019 bakdata GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.bakdata.schemaregistrymock;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.avro.Schema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchemaRegistryMockTest {
    private final SchemaRegistryMock schemaRegistry = new SchemaRegistryMock();

    @BeforeEach
    void start() {
        this.schemaRegistry.start();
    }

    @AfterEach
    void stop() {
        this.schemaRegistry.stop();
    }

    @Test
    void shouldRegisterKeySchema() throws IOException, RestClientException {
        final Schema keySchema = createSchema("key_schema");
        final int id = this.schemaRegistry.registerKeySchema("test-topic", keySchema);

        final Schema retrievedSchema = this.schemaRegistry.getSchemaRegistryClient().getById(id);
        assertThat(retrievedSchema).isEqualTo(keySchema);
    }

    @Test
    void shouldRegisterValueSchema() throws IOException, RestClientException {
        final Schema valueSchema = createSchema("value_schema");
        final int id = this.schemaRegistry.registerValueSchema("test-topic", valueSchema);

        final Schema retrievedSchema = this.schemaRegistry.getSchemaRegistryClient().getById(id);
        assertThat(retrievedSchema).isEqualTo(valueSchema);
    }

    @Test
    void shouldRegisterKeySchemaWithClient() throws IOException, RestClientException {
        final Schema keySchema = createSchema("key_schema");
        final int id = this.schemaRegistry.getSchemaRegistryClient().register("test-topic-key", keySchema);

        final Schema retrievedSchema = this.schemaRegistry.getSchemaRegistryClient().getById(id);
        assertThat(retrievedSchema).isEqualTo(keySchema);
    }

    @Test
    void shouldRegisterValueSchemaWithClient() throws IOException, RestClientException {
        final Schema valueSchema = createSchema("value_schema");
        final int id = this.schemaRegistry.getSchemaRegistryClient().register("test-topic-value", valueSchema);

        final Schema retrievedSchema = this.schemaRegistry.getSchemaRegistryClient().getById(id);
        assertThat(retrievedSchema).isEqualTo(valueSchema);
    }

    @Test
    void shouldHaveSchemaVersions() throws IOException, RestClientException {
        final Schema valueSchema = createSchema("value_schema");
        final String topic = "test-topic";
        final int id = this.schemaRegistry.registerValueSchema(topic, valueSchema);

        final List<Integer> versions = this.schemaRegistry.getSchemaRegistryClient().getAllVersions(topic + "-value");
        assertThat(versions.size()).isOne();

        final SchemaMetadata metadata = this.schemaRegistry.getSchemaRegistryClient()
                .getSchemaMetadata(topic + "-value", versions.get(0));
        assertThat(metadata.getId()).isEqualTo(id);
        final String schemaString = metadata.getSchema();
        final Schema retrievedSchema = new Schema.Parser().parse(schemaString);
        assertThat(retrievedSchema).isEqualTo(valueSchema);
    }

    @Test
    void shouldNotHaveSchemaVersionsForUnknownSubject() {
        assertThatExceptionOfType(RestClientException.class)
                .isThrownBy(() -> this.schemaRegistry.getSchemaRegistryClient().getAllVersions("does_not_exist"))
                .satisfies(e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND));
        assertThatExceptionOfType(RestClientException.class)
                .isThrownBy(() -> this.schemaRegistry.getSchemaRegistryClient().getSchemaMetadata("does_not_exist", 0))
                .satisfies(e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND));
    }

    @Test
    void shouldHaveLatestSchemaVersion() throws IOException, RestClientException {
        final Schema valueSchema1 = createSchema("value_schema");
        final String topic = "test-topic";
        final int id1 = this.schemaRegistry.registerValueSchema(topic, valueSchema1);

        final List<Schema.Field> fields = Collections.singletonList(
                new Schema.Field("f1", Schema.create(Schema.Type.STRING), "", (Object) null));
        final Schema valueSchema2 = Schema.createRecord("value_schema", "no doc", "", false, fields);
        final int id2 = this.schemaRegistry.registerValueSchema(topic, valueSchema2);

        final List<Integer> versions = this.schemaRegistry.getSchemaRegistryClient().getAllVersions(topic + "-value");
        assertThat(versions.size()).isEqualTo(2);

        final SchemaMetadata metadata =
                this.schemaRegistry.getSchemaRegistryClient().getLatestSchemaMetadata(topic + "-value");
        final int metadataId = metadata.getId();
        assertThat(metadataId).isNotEqualTo(id1);
        assertThat(metadataId).isEqualTo(id2);
        final String schemaString = metadata.getSchema();
        final Schema retrievedSchema = new Schema.Parser().parse(schemaString);
        assertThat(retrievedSchema).isEqualTo(valueSchema2);
    }

    @Test
    void shouldNotHaveLatestSchemaVersionForUnknownSubject() {
        assertThatExceptionOfType(RestClientException.class)
                .isThrownBy(
                        () -> this.schemaRegistry.getSchemaRegistryClient().getLatestSchemaMetadata("does_not_exist"))
                .satisfies(e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND));
    }

    @Test
    void shouldReturnAllSubjects() throws IOException, RestClientException {
        this.schemaRegistry.registerKeySchema("test-topic", createSchema("key_schema"));
        this.schemaRegistry.registerValueSchema("test-topic", createSchema("value_schema"));
        final Collection<String> allSubjects = this.schemaRegistry.getSchemaRegistryClient().getAllSubjects();
        assertThat(allSubjects).hasSize(2).containsExactly("test-topic-key", "test-topic-value");
    }

    @Test
    void shouldReturnEmptyListForNoSubjects() throws IOException, RestClientException {
        final Collection<String> allSubjects = this.schemaRegistry.getSchemaRegistryClient().getAllSubjects();
        assertThat(allSubjects).isEmpty();
    }

    @Test
    void shouldDeleteKeySchema() throws IOException, RestClientException {
        this.schemaRegistry.registerKeySchema("test-topic", createSchema("key_schema"));
        final SchemaRegistryClient client = this.schemaRegistry.getSchemaRegistryClient();
        final Collection<String> allSubjects = client.getAllSubjects();
        assertThat(allSubjects).hasSize(1).containsExactly("test-topic-key");
        this.schemaRegistry.deleteKeySchema("test-topic");
        final Collection<String> subjectsAfterDeletion = client.getAllSubjects();
        assertThat(subjectsAfterDeletion).isEmpty();
    }


    @Test
    void shouldDeleteValueSchema() throws IOException, RestClientException {
        final SchemaRegistryClient client = this.schemaRegistry.getSchemaRegistryClient();
        this.schemaRegistry.registerValueSchema("test-topic", createSchema("value_schema"));
        final Collection<String> allSubjects = client.getAllSubjects();
        assertThat(allSubjects).hasSize(1).containsExactly("test-topic-value");
        this.schemaRegistry.deleteValueSchema("test-topic");
        final Collection<String> subjectsAfterDeletion = client.getAllSubjects();
        assertThat(subjectsAfterDeletion).isEmpty();
    }

    @Test
    void shouldDeleteKeySchemaWithClient() throws IOException, RestClientException {
        final SchemaRegistryClient client = this.schemaRegistry.getSchemaRegistryClient();
        this.schemaRegistry.registerKeySchema("test-topic", createSchema("key_schema"));
        final Collection<String> allSubjects = client.getAllSubjects();
        assertThat(allSubjects).hasSize(1).containsExactly("test-topic-key");
        client.deleteSubject("test-topic-key");
        final Collection<String> subjectsAfterDeletion = client.getAllSubjects();
        assertThat(subjectsAfterDeletion).isEmpty();
    }


    @Test
    void shouldDeleteValueSchemaWithClient() throws IOException, RestClientException {
        final SchemaRegistryClient client = this.schemaRegistry.getSchemaRegistryClient();
        this.schemaRegistry.registerValueSchema("test-topic", createSchema("value_schema"));
        final Collection<String> allSubjects = client.getAllSubjects();
        assertThat(allSubjects).hasSize(1).containsExactly("test-topic-value");
        client.deleteSubject("test-topic-value");
        final Collection<String> subjectsAfterDeletion = client.getAllSubjects();
        assertThat(subjectsAfterDeletion).isEmpty();
    }

    @Test
    void shouldNotDeleteUnknownSubject() {
        assertThatExceptionOfType(RestClientException.class)
                .isThrownBy(() -> this.schemaRegistry.getSchemaRegistryClient().deleteSubject("does_not_exist"))
                .satisfies(e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND));
    }

    @Test
    void shouldNotHaveSchemaVersionsForDeletedSubject() throws IOException, RestClientException {
        final Schema valueSchema = createSchema("value_schema");
        final String topic = "test-topic";
        final int id = this.schemaRegistry.registerValueSchema(topic, valueSchema);

        final SchemaRegistryClient schemaRegistryClient = this.schemaRegistry.getSchemaRegistryClient();
        final List<Integer> versions = schemaRegistryClient.getAllVersions(topic + "-value");
        assertThat(versions.size()).isOne();

        final SchemaMetadata metadata = schemaRegistryClient.getSchemaMetadata(topic + "-value", versions.get(0));
        assertThat(metadata.getId()).isEqualTo(id);
        assertThat(schemaRegistryClient.getLatestSchemaMetadata(topic + "-value"))
                .isNotNull();
        this.schemaRegistry.deleteValueSchema(topic);
        assertThatExceptionOfType(RestClientException.class)
                .isThrownBy(() -> schemaRegistryClient.getAllVersions(topic + "-value"))
                .satisfies(e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND));
        assertThatExceptionOfType(RestClientException.class)
                .isThrownBy(() -> schemaRegistryClient.getSchemaMetadata(topic + "-value", versions.get(0)))
                .satisfies(e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND));
        assertThatExceptionOfType(RestClientException.class)
                .isThrownBy(() -> schemaRegistryClient.getLatestSchemaMetadata(topic + "-value"))
                .satisfies(e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND));
    }

    @Test
    void shouldReturnValueSchemaVersion() throws IOException, RestClientException {
        final Schema valueSchema = createSchema("value_schema");
        this.schemaRegistry.registerValueSchema("test-topic", valueSchema);

        final Integer version =
                this.schemaRegistry.getSchemaRegistryClient().getVersion("test-topic-value", valueSchema);
        assertThat(version).isEqualTo(1);
    }

    @Test
    void shouldReturnKeySchemaVersion() throws IOException, RestClientException {
        final Schema valueSchema = createSchema("value_schema");
        this.schemaRegistry.registerKeySchema("test-topic", valueSchema);

        final Integer version = this.schemaRegistry.getSchemaRegistryClient().getVersion("test-topic-key", valueSchema);
        assertThat(version).isEqualTo(1);
    }

    @Test
    void shouldReturnValueSchemaId() throws IOException, RestClientException {
        final Schema valueSchema = createSchema("value_schema");
        this.schemaRegistry.registerValueSchema("test-topic", valueSchema);

        final Integer id = this.schemaRegistry.getSchemaRegistryClient().getId("test-topic-value", valueSchema);
        assertThat(id).isEqualTo(1);
    }

    @Test
    void shouldReturnKeySchemaId() throws IOException, RestClientException {
        final Schema valueSchema = createSchema("value_schema");
        this.schemaRegistry.registerKeySchema("test-topic", valueSchema);

        final Integer id = this.schemaRegistry.getSchemaRegistryClient().getId("test-topic-key", valueSchema);
        assertThat(id).isEqualTo(1);
    }

    @Test
    void shouldNotReturnVersionForNonExistingSchema() {
        final Schema test = createSchema("test");
        final Schema other = createSchema("other");
        this.schemaRegistry.registerValueSchema("test-topic", other);
        assertThatThrownBy(() -> this.schemaRegistry.getSchemaRegistryClient().getVersion("test-topic-value", test))
                .isInstanceOfSatisfying(RestClientException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND))
                .hasMessage("Schema not found; error code: 40403");
    }

    @Test
    void shouldNotReturnIdForNonExistingSchema() {
        final Schema test = createSchema("test");
        final Schema other = createSchema("other");
        this.schemaRegistry.registerValueSchema("test-topic", other);
        assertThatThrownBy(() -> this.schemaRegistry.getSchemaRegistryClient().getId("test-topic-value", test))
                .isInstanceOfSatisfying(RestClientException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND))
                .hasMessage("Schema not found; error code: 40403");
    }

    @Test
    void shouldNotReturnVersionForNonExistingSubject() {
        final Schema test = createSchema("test");
        assertThatThrownBy(() -> this.schemaRegistry.getSchemaRegistryClient().getVersion("test-topic-value", test))
                .isInstanceOfSatisfying(RestClientException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND))
                .hasMessage("Subject not found; error code: 40401");
    }

    @Test
    void shouldNotReturnIdForNonExistingSubject() {
        final Schema test = createSchema("test");
        assertThatThrownBy(() -> this.schemaRegistry.getSchemaRegistryClient().getId("test-topic-value", test))
                .isInstanceOfSatisfying(RestClientException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND))
                .hasMessage("Subject not found; error code: 40401");
    }

    @Test
    void shouldReturnVersionForDeletedSchema() throws IOException, RestClientException {
        final Schema testSchema = createSchema("value_schema");
        this.schemaRegistry.registerKeySchema("test-topic", testSchema);

        final int version = this.schemaRegistry.getSchemaRegistryClient().getVersion("test-topic-key", testSchema);
        assertThat(version).isEqualTo(1);

        this.schemaRegistry.deleteValueSchema("test-topic");
        this.schemaRegistry.registerValueSchema("test-topic", createSchema("new_schema"));

        final Integer versionAfterDeletion =
                this.schemaRegistry.getSchemaRegistryClient().getVersion("test-topic-key", testSchema);
        assertThat(versionAfterDeletion).isEqualTo(1);
    }

    @Test
    void shouldNotReturnIdForDeletedSchema() throws IOException, RestClientException {
        final Schema testSchema = createSchema("value_schema");
        this.schemaRegistry.registerKeySchema("test-topic", testSchema);

        final Integer version = this.schemaRegistry.getSchemaRegistryClient().getId("test-topic-key", testSchema);
        assertThat(version).isEqualTo(1);

        this.schemaRegistry.deleteKeySchema("test-topic");
        this.schemaRegistry.registerKeySchema("test-topic", createSchema("new_schema"));

        assertThatThrownBy(() -> this.schemaRegistry.getSchemaRegistryClient().getId("test-topic-key", testSchema))
                .isInstanceOfSatisfying(RestClientException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HTTP_NOT_FOUND))
                .hasMessage("Schema not found; error code: 40403");
    }

    private static Schema createSchema(final String name) {
        return Schema.createRecord(name, "no doc", "", false, Collections.emptyList());
    }
}
