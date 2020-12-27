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
import walkingkooka.collect.list.Lists;
import walkingkooka.net.AbsoluteUrl;
import walkingkooka.net.RelativeUrl;
import walkingkooka.net.Url;
import walkingkooka.net.header.Accept;
import walkingkooka.net.header.CharsetName;
import walkingkooka.net.header.HttpHeaderName;
import walkingkooka.net.header.MediaType;
import walkingkooka.net.http.HttpEntity;
import walkingkooka.net.http.HttpMethod;
import walkingkooka.net.http.HttpProtocolVersion;
import walkingkooka.net.http.HttpStatus;
import walkingkooka.net.http.HttpStatusCode;
import walkingkooka.net.http.HttpTransport;
import walkingkooka.net.http.server.HttpRequest;
import walkingkooka.net.http.server.HttpRequests;
import walkingkooka.net.http.server.HttpResponse;
import walkingkooka.net.http.server.HttpResponses;
import walkingkooka.tree.expression.ExpressionNumberContexts;
import walkingkooka.tree.json.marshall.JsonNodeMarshallContext;
import walkingkooka.tree.json.marshall.JsonNodeMarshallContexts;
import walkingkooka.tree.json.marshall.JsonNodeUnmarshallContext;
import walkingkooka.tree.json.marshall.JsonNodeUnmarshallContexts;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PostRequestBodyJsonHttpRequestHttpResponseBiConsumerTest extends PostRequestBodyJsonHttpRequestHttpResponseBiConsumerTestCase<PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl>>
        implements ToStringTesting<PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl>> {

    private final static RelativeUrl INPUT = Url.parseRelative("/input");
    private final static Class<RelativeUrl> INPUT_TYPE = RelativeUrl.class;

    private final static AbsoluteUrl OUTPUT = Url.parseAbsolute("http://example.com/output");
    private final static Class<AbsoluteUrl> OUTPUT_TYPE = AbsoluteUrl.class;

    private final static Function<RelativeUrl, AbsoluteUrl> HANDLER = (i) -> {
        assertEquals(INPUT, i);
        return OUTPUT;
    };

    private final JsonNodeMarshallContext MARSHALL_CONTEXT = JsonNodeMarshallContexts.basic();
    private final JsonNodeUnmarshallContext UNMARSHALL_CONTEXT = JsonNodeUnmarshallContexts.basic(ExpressionNumberContexts.fake());

    @Test
    public void testWithNullHandlerFails() {
        this.withFails(null, INPUT_TYPE, OUTPUT_TYPE, MARSHALL_CONTEXT, UNMARSHALL_CONTEXT);
    }

    @Test
    public void testWithNullInputTypeFails() {
        this.withFails(HANDLER, null, OUTPUT_TYPE, MARSHALL_CONTEXT, UNMARSHALL_CONTEXT);
    }

    @Test
    public void testWithNullOutputTypeFails() {
        this.withFails(HANDLER, INPUT_TYPE, null, MARSHALL_CONTEXT, UNMARSHALL_CONTEXT);
    }

    @Test
    public void testWithNullMarshallContextFails() {
        this.withFails(HANDLER, INPUT_TYPE, OUTPUT_TYPE, null, UNMARSHALL_CONTEXT);
    }

    @Test
    public void testWithNullUnmarshallContextFails() {
        this.withFails(HANDLER, INPUT_TYPE, OUTPUT_TYPE, MARSHALL_CONTEXT, null);
    }

    private void withFails(final Function<RelativeUrl, AbsoluteUrl> handler,
                           final Class<RelativeUrl> inputType,
                           final Class<AbsoluteUrl> outputType,
                           final JsonNodeMarshallContext marshallContext,
                           final JsonNodeUnmarshallContext unmarshallContext) {
        assertThrows(NullPointerException.class, () -> PostRequestBodyJsonHttpRequestHttpResponseBiConsumer.with(handler, inputType, outputType, marshallContext, unmarshallContext));
    }

    @Test
    public void testMethodNotPostFailsFails() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpMethod.PUT,
                HttpEntity.EMPTY
                        .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .setBodyText("")
                        .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.METHOD_NOT_ALLOWED.setMessage("Expected POST got PUT"));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testMissingContentTypeFails() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .setBodyText("")
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Missing " + HttpHeaderName.CONTENT_TYPE));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testInvalidContentTypeFails() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final MediaType contentType = MediaType.BINARY;

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, contentType)
                .setBodyText("")
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Header " + HttpHeaderName.CONTENT_TYPE + " expected " + MediaType.APPLICATION_JSON + " got " + contentType));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testMissingRequestBodyFails() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBodyText("")
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Required body missing"));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testInvalidContentLengthFails() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .addHeader(HttpHeaderName.CONTENT_LENGTH, 100L)
                .setBodyText("{}"));

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Content-Length: 100 != body length=2 mismatch"));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testInvalidRequestBodyFails() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBodyText("{")
                .setContentLength());

        this.acceptFails(consumer,
                request,
                HttpStatusCode.BAD_REQUEST.setMessage("Invalid walkingkooka.net.RelativeUrl: End of text at (2,1) \"{\" expected [ OBJECT_PROPERTY, [{[ WHITESPACE ], SEPARATOR, OBJECT_PROPERTY_REQUIRED }]], [ WHITESPACE ], OBJECT_END"),
                "End of text at (2,1) \"{\" ");
    }

    @Test
    public void testRequestContentLengthMissingFails() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBodyText("{"));

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.LENGTH_REQUIRED.status());
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testRequestAcceptMissingFails() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBodyText(MARSHALL_CONTEXT.marshall(INPUT)
                        .toString())
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Missing Accept"));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testRequestAcceptIncompatibleFails() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .addHeader(HttpHeaderName.ACCEPT, MediaType.IMAGE_JPEG.accept())
                .setBodyText(MARSHALL_CONTEXT.marshall(INPUT)
                        .toString())
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.BAD_REQUEST.setMessage("Header Accept expected application/json got image/jpeg"));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testHandlerFails() {
        final String message = "Something went wrong!";
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer((i) -> {
            throw new IllegalArgumentException(message);
        });

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .addHeader(HttpHeaderName.ACCEPT, MediaType.APPLICATION_JSON.accept())
                .setBodyText(MARSHALL_CONTEXT.marshall(INPUT)
                        .toString())
                .setContentLength());

        this.acceptFails(consumer,
                request,
                HttpStatusCode.INTERNAL_SERVER_ERROR.status().setMessage(message),
                message);
    }

    private void acceptFails(final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer,
                             final HttpRequest request,
                             final HttpStatus status,
                             final String responseBodyContains) {
        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        assertEquals(Optional.of(status),
                response.status(),
                "status");
        final List<HttpEntity> entities = response.entities();
        assertEquals(1, entities.size(), "entities count");

        final HttpEntity entity = response.entities()
                .get(0);
        final Map<HttpHeaderName<?>, List<?>> headers = entity.headers();

        assertEquals(Lists.of(MediaType.TEXT_PLAIN), headers.get(HttpHeaderName.CONTENT_TYPE), () -> "headers\n" + response);

        final String responseBody = entity.bodyText();
        assertTrue(responseBody.contains(responseBodyContains), () -> response.toString());
    }

    @Test
    public void testSuccessfulAcceptApplicationJson() {
        this.successfulAndCheck(MediaType.APPLICATION_JSON.accept());
    }

    @Test
    public void testSuccessfulAcceptWildcard() {
        this.successfulAndCheck(MediaType.ALL.accept());
    }

    private void successfulAndCheck(final Accept accept) {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .addHeader(HttpHeaderName.ACCEPT, accept)
                .setBodyText(MARSHALL_CONTEXT.marshall(INPUT)
                        .toString())
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final String responseBody = MARSHALL_CONTEXT.marshall(OUTPUT).toString();

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.OK.setMessage("POST AbsoluteUrl OK"));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);
        expected.addEntity(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON.setCharset(CharsetName.UTF_8))
                .addHeader(JsonHttpRequestHttpResponseBiConsumers.X_CONTENT_TYPE_NAME, OUTPUT_TYPE.getSimpleName())
                .setBodyText(responseBody)
                .setContentLength());

        assertEquals(expected, response, () -> "response\n" + request);
    }

    @Test
    public void testSuccessfulContentTypeWithCharset() {
        final PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl> consumer = this.createConsumer();

        final HttpRequest request = this.request(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON.setCharset(CharsetName.UTF_8))
                .addHeader(HttpHeaderName.ACCEPT, MediaType.APPLICATION_JSON.accept())
                .setBodyText(MARSHALL_CONTEXT.marshall(INPUT)
                        .toString())
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final String responseBody = MARSHALL_CONTEXT.marshall(OUTPUT).toString();

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.OK.setMessage("POST AbsoluteUrl OK"));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);
        expected.addEntity(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON.setCharset(CharsetName.UTF_8))
                .addHeader(JsonHttpRequestHttpResponseBiConsumers.X_CONTENT_TYPE_NAME, OUTPUT_TYPE.getSimpleName())
                .setBodyText(responseBody)
                .setContentLength());

        assertEquals(expected, response, () -> "response\n" + request);
    }

    private PostRequestBodyJsonHttpRequestHttpResponseBiConsumer createConsumer() {
        return this.createConsumer(HANDLER);
    }

    private PostRequestBodyJsonHttpRequestHttpResponseBiConsumer createConsumer(final Function<RelativeUrl, AbsoluteUrl> handler) {
        return PostRequestBodyJsonHttpRequestHttpResponseBiConsumer.with(handler,
                INPUT_TYPE,
                OUTPUT_TYPE,
                MARSHALL_CONTEXT,
                UNMARSHALL_CONTEXT);
    }

    private HttpRequest request(final HttpEntity entity) {
        return this.request(HttpMethod.POST, entity);
    }

    private HttpRequest request(final HttpMethod method,
                                final HttpEntity entity) {
        assertNotEquals(null, method, "method");
        assertNotEquals(null, entity, "entity");

        return HttpRequests.value(method,
                HttpTransport.UNSECURED,
                Url.parseRelative("/handler"),
                HttpProtocolVersion.VERSION_1_0,
                entity);
    }

    @Test
    public void testToString() {
        this.toStringAndCheck(PostRequestBodyJsonHttpRequestHttpResponseBiConsumer.with(HANDLER, INPUT_TYPE, OUTPUT_TYPE, MARSHALL_CONTEXT, UNMARSHALL_CONTEXT), HANDLER.toString());
    }

    // ClassTesting.....................................................................................................

    @Override
    public Class<PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<RelativeUrl, AbsoluteUrl>> type() {
        return Cast.to(PostRequestBodyJsonHttpRequestHttpResponseBiConsumer.class);
    }
}
