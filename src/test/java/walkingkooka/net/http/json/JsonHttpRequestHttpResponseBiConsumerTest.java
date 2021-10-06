/*
 * Copyright 2020 Miroslav Pokorny (github.com/mP1)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package walkingkooka.net.http.json;

import org.junit.jupiter.api.Test;
import walkingkooka.Cast;
import walkingkooka.ToStringTesting;
import walkingkooka.net.Url;
import walkingkooka.net.header.HttpHeaderName;
import walkingkooka.net.header.MediaType;
import walkingkooka.net.http.HttpEntity;
import walkingkooka.net.http.HttpMethod;
import walkingkooka.net.http.HttpProtocolVersion;
import walkingkooka.net.http.HttpStatusCode;
import walkingkooka.net.http.HttpTransport;
import walkingkooka.net.http.server.HttpRequest;
import walkingkooka.net.http.server.HttpRequests;
import walkingkooka.net.http.server.HttpResponse;
import walkingkooka.net.http.server.HttpResponses;
import walkingkooka.tree.json.JsonNode;
import walkingkooka.tree.json.JsonPropertyName;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class JsonHttpRequestHttpResponseBiConsumerTest implements ToStringTesting<JsonHttpRequestHttpResponseBiConsumer> {

    private final static JsonNode INPUT = JsonNode.object()
            .set(JsonPropertyName.with("input"), JsonNode.number(123.5));

    private final static JsonNode OUTPUT = JsonNode.object()
            .set(JsonPropertyName.with("output"), JsonNode.number(45.75));

    private final static Function<JsonNode, JsonNode> HANDLER = (i) -> {
        assertEquals(INPUT, i);
        return OUTPUT;
    };

    @Test
    public void testWithNullHandlerFails() {
        assertThrows(NullPointerException.class, () -> JsonHttpRequestHttpResponseBiConsumer.with(null));
    }

    @Test
    public void testMissingRequestBodyFails() {
        final HttpRequest request = this.request(HttpEntity.EMPTY
                .setBodyText("")
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Required body missing"));
        expected.addEntity(HttpEntity.EMPTY);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testInvalidContentLengthFails() {
        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .addHeader(HttpHeaderName.CONTENT_LENGTH, 100L)
                .setBodyText("{}"));

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Content-Length: 100 != body length=2 mismatch"));
        expected.addEntity(HttpEntity.EMPTY);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testInvalidRequestBodyFails() {
        final HttpRequest request = this.request(
                HttpEntity.EMPTY
                        .setBodyText("{")
                        .setContentLength()
        );

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("End of text at (2,1) \"{\" expected [ OBJECT_PROPERTY, [{[ WHITESPACE ], SEPARATOR, OBJECT_PROPERTY_REQUIRED }]], [ WHITESPACE ], OBJECT_END"));
        expected.addEntity(HttpEntity.EMPTY);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testContentLengthMissingFails() {
        final HttpRequest request = this.request(
                HttpEntity.EMPTY
                        .setBodyText("{")
        );

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.LENGTH_REQUIRED.status());
        expected.addEntity(HttpEntity.EMPTY);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testSuccess() {
        final HttpRequest request = this.request(
                HttpEntity.EMPTY
                        .setBodyText(INPUT.toString())
                        .setContentLength()
        );

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.OK.status());
        expected.addEntity(
                HttpEntity.EMPTY
                        .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .setBodyText(OUTPUT.toString())
                        .setContentLength()
        );

        assertEquals(expected, response, () -> "response\n" + request);
    }

    private JsonHttpRequestHttpResponseBiConsumer createConsumer() {
        return JsonHttpRequestHttpResponseBiConsumer.with(HANDLER);
    }

    private HttpRequest request(final HttpEntity entity) {
        return this.request(HttpMethod.POST, entity);
    }

    private HttpRequest request(final HttpMethod method,
                                final HttpEntity entity) {
        assertNotEquals(null, method, "method");
        assertNotEquals(null, entity, "entity");

        return HttpRequests.value(
                method,
                HttpTransport.UNSECURED,
                Url.parseRelative("/handler"),
                HttpProtocolVersion.VERSION_1_0,
                entity
        );
    }

    @Test
    public void testToString() {
        this.toStringAndCheck(JsonHttpRequestHttpResponseBiConsumer.with(HANDLER), HANDLER.toString());
    }

    // ClassTesting.....................................................................................................

    @Override
    public Class<JsonHttpRequestHttpResponseBiConsumer> type() {
        return Cast.to(JsonHttpRequestHttpResponseBiConsumer.class);
    }
}
