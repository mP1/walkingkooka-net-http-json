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
import walkingkooka.net.header.AcceptCharset;
import walkingkooka.net.header.CharsetName;
import walkingkooka.net.header.ETag;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class JsonHttpHandlerTest implements ToStringTesting<JsonHttpHandler> {

    private final static JsonNode INPUT = JsonNode.object()
            .set(JsonPropertyName.with("input"), JsonNode.number(123.5));

    private final static JsonNode OUTPUT = JsonNode.object()
            .set(JsonPropertyName.with("output"), JsonNode.number(45.75));

    private final Function<JsonNode, JsonNode> HANDLER = (i) -> {
        this.checkEquals(INPUT, i);
        return OUTPUT;
    };

    private final static HttpHeaderName<ETag> POST_HEADER_NAME = HttpHeaderName.E_TAG;
    private final static ETag POST_HEADER_VALUE = ETag.wildcard();

    private final static Function<HttpEntity, HttpEntity> POST = (e) -> e.addHeader(POST_HEADER_NAME, POST_HEADER_VALUE);

    @Test
    public void testWithNullHandlerFails() {
        assertThrows(NullPointerException.class, () -> JsonHttpHandler.with(null, POST));
    }

    @Test
    public void testWithNullPostFails() {
        assertThrows(NullPointerException.class, () -> JsonHttpHandler.with(HANDLER, null));
    }

    @Test
    public void testMissingRequestBodyFails() {
        final HttpRequest request = this.request(HttpEntity.EMPTY
                .setBodyText("")
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .handle(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Required body missing"));
        expected.setEntity(HttpEntity.EMPTY);

        this.checkEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testInvalidContentLengthFails() {
        final HttpRequest request = this.request(HttpEntity.EMPTY
                .setContentType(MediaType.APPLICATION_JSON)
                .addHeader(HttpHeaderName.CONTENT_LENGTH, 100L)
                .setBodyText("{}"));

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .handle(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Content-Length: 100 != body length=2 mismatch"));
        expected.setEntity(HttpEntity.EMPTY);

        this.checkEquals(expected, response, () -> "response\n" + request);
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
                .handle(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Invalid character '{' at 0 in \"{\""));
        expected.setEntity(HttpEntity.EMPTY);

        this.checkEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testContentLengthMissingFails() {
        final HttpRequest request = this.request(
                HttpEntity.EMPTY
                        .setBodyText("{")
        );

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .handle(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.LENGTH_REQUIRED.status());
        expected.setEntity(HttpEntity.EMPTY);

        this.checkEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testSuccessMissingAcceptCharset() {
        final HttpRequest request = this.request(
                HttpEntity.EMPTY
                        .setBodyText(INPUT.toString())
                        .setContentLength()
        );

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .handle(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.OK.status());
        expected.setEntity(
                HttpEntity.EMPTY
                        .setContentType(MediaType.APPLICATION_JSON.setCharset(CharsetName.UTF_8))
                        .addHeader(POST_HEADER_NAME, POST_HEADER_VALUE)
                        .setBodyText(OUTPUT.toString())
                        .setContentLength()
        );

        this.checkEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testSuccess() {
        final CharsetName charsetName = CharsetName.UTF_16;

        final HttpRequest request = this.request(
                HttpEntity.EMPTY
                        .addHeader(HttpHeaderName.ACCEPT_CHARSET, AcceptCharset.parse(charsetName.toHeaderText()))
                        .setBodyText(INPUT.toString())
                        .setContentLength()
        );

        final HttpResponse response = HttpResponses.recording();

        this.createConsumer()
                .handle(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.OK.status());
        expected.setEntity(
                HttpEntity.EMPTY
                        .setContentType(MediaType.APPLICATION_JSON.setCharset(charsetName))
                        .addHeader(POST_HEADER_NAME, POST_HEADER_VALUE)
                        .setBodyText(OUTPUT.toString())
                        .setContentLength()
        );

        this.checkEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testSuccessNoContent() {
        final CharsetName charsetName = CharsetName.UTF_16;

        final HttpRequest request = this.request(
                HttpEntity.EMPTY
                        .addHeader(HttpHeaderName.ACCEPT_CHARSET, AcceptCharset.parse(charsetName.toHeaderText()))
                        .setBodyText(INPUT.toString())
                        .setContentLength()
        );

        final HttpResponse response = HttpResponses.recording();

        JsonHttpHandler.with((inputIgnored) -> null, POST)
                .handle(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.NO_CONTENT.status());
        expected.setEntity(
                HttpEntity.EMPTY
                        .addHeader(POST_HEADER_NAME, POST_HEADER_VALUE)
        );

        this.checkEquals(expected, response, () -> "response\n" + request);
    }

    private JsonHttpHandler createConsumer() {
        return JsonHttpHandler.with(HANDLER, POST);
    }

    private HttpRequest request(final HttpEntity entity) {
        return this.request(HttpMethod.POST, entity);
    }

    private HttpRequest request(final HttpMethod method,
                                final HttpEntity entity) {
        this.checkNotEquals(null, method, "method");
        this.checkNotEquals(null, entity, "entity");

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
        this.toStringAndCheck(JsonHttpHandler.with(HANDLER, POST), HANDLER + " " + POST);
    }

    // ClassTesting.....................................................................................................

    @Override
    public Class<JsonHttpHandler> type() {
        return Cast.to(JsonHttpHandler.class);
    }
}
