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

import walkingkooka.net.header.Accept;
import walkingkooka.net.header.AcceptCharset;
import walkingkooka.net.header.CharsetName;
import walkingkooka.net.header.HttpHeaderName;
import walkingkooka.net.header.MediaType;
import walkingkooka.net.header.NotAcceptableHeaderException;
import walkingkooka.net.http.HttpEntity;
import walkingkooka.net.http.HttpMethod;
import walkingkooka.net.http.HttpStatus;
import walkingkooka.net.http.HttpStatusCode;
import walkingkooka.net.http.server.HttpRequest;
import walkingkooka.net.http.server.HttpResponse;
import walkingkooka.tree.json.JsonNode;
import walkingkooka.tree.json.marshall.JsonNodeMarshallContext;
import walkingkooka.tree.json.marshall.JsonNodeUnmarshallContext;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Represents a single request and includes methods to handle key steps in processing. This methods are only called by
 * {@link PostRequestBodyJsonHttpRequestHttpResponseBiConsumer}.
 */
final class PostRequestBodyJsonHttpRequestHttpResponseBiConsumerRequest<I, O> {

    static <I, O> PostRequestBodyJsonHttpRequestHttpResponseBiConsumerRequest<I, O> with(final HttpRequest request,
                                                                                         final HttpResponse response) {
        return new PostRequestBodyJsonHttpRequestHttpResponseBiConsumerRequest<>(request, response);
    }

    private PostRequestBodyJsonHttpRequestHttpResponseBiConsumerRequest(final HttpRequest request,
                                                                        final HttpResponse response) {
        super();
        this.request = request;
        this.response = response;
    }

    HttpMethod postOrMethodNotAllowed() {
        HttpMethod method = this.request.method();
        if(!HttpMethod.POST.equals(method)) {
            this.setStatus(HttpStatusCode.METHOD_NOT_ALLOWED
                    .setMessage("Expected POST got " + method)
            );
            method = null;
        }
        return method;
    }

    MediaType contentTypeApplicationJsonOrBadRequest() {
        MediaType contentType = CONTENT_TYPE.header(this.request)
                .orElse(null);
        if (null == contentType) {
            this.badRequest("Missing " + HttpHeaderName.CONTENT_TYPE);
        } else {
            if (!JSON.equalsIgnoringParameters(contentType)) {
                this.badRequest("Header " + CONTENT_TYPE + " expected " + JSON + " got " + contentType);
                contentType = null;
            }
        }

        return contentType;
    }

    private final static HttpHeaderName<MediaType> CONTENT_TYPE = HttpHeaderName.CONTENT_TYPE;
    private final static MediaType JSON = MediaType.APPLICATION_JSON;

    /**
     * Reads and returns the body as text, with null signifying an error occurred and a bad request response set.
     */
    String resourceTextOrBadRequest() {
        final HttpRequest request = this.request;

        String bodyText;
        try {
            bodyText = request.bodyText();
        } catch (final RuntimeException cause) {
            this.badRequest("Invalid content: " + cause.getMessage(), cause);
            bodyText = null;
        }

        if (null != bodyText) {
            final Long contentLength = HttpHeaderName.CONTENT_LENGTH.header(request).orElse(null);
            if (bodyText.isEmpty()) {
                this.badRequest("Required body missing");
                bodyText = null;

            } else {
                if (null == contentLength) {
                    this.setStatus(HttpStatusCode.LENGTH_REQUIRED.status());
                    bodyText = null;
                } else {
                    final long bodyLength = request.bodyLength();
                    final long contentLengthLong = contentLength.longValue();
                    if (bodyLength != contentLengthLong) {
                        this.badRequest(HttpHeaderName.CONTENT_LENGTH + ": " + contentLengthLong + " != body length=" + bodyLength + " mismatch");
                        bodyText = null;
                    }
                }
            }
        }

        return bodyText;
    }

    /**
     * Turns the request text which should be JSON into a value.
     */
    I resourceOrBadRequest(final String requestText,
                           final Class<I> type,
                           final JsonNodeUnmarshallContext context) {
        I resource;

        try {
            final JsonNode node = JsonNode.parse(requestText);
            resource = context.unmarshall(node, type);
        } catch (final Exception cause) {
            this.badRequest("Invalid " + type.getName() + ": " + cause.getMessage(), cause);
            resource = null;
        }
        return resource;
    }

    /**
     * The client must include a header accept: application/json
     */
    Accept acceptApplicationJsonOrBadRequest() {
        final HttpHeaderName<Accept> header = HttpHeaderName.ACCEPT;
        Accept accept = header.header(this.request)
                .orElse(null);
        if (null == accept) {
            this.badRequest("Missing " + HttpHeaderName.ACCEPT);
        } else {
            if (!accept.test(JSON)) {
                this.badRequest("Header " + header + " expected " + JSON + " got " + accept);
                accept = null;
            }
        }

        return accept;
    }

    /**
     * Converts the output to JSON and then sets the required headers and body.
     */
    void writeResponse(final O output,
                       final JsonNodeMarshallContext context) {
        this.setStatusAndBody(context.marshall(output).toString(),
                output.getClass(),
                this.request.method());
    }

    // error reporting..................................................................................................

    void badRequest(final String message) {
        this.setStatus(HttpStatusCode.BAD_REQUEST, message);
    }

    /**
     * Reports a bad request with the body filled with the stack trace of the provided {@link Throwable}.
     */
    void badRequest(final String message,
                    final Throwable cause) {
        this.badRequest(message);
        this.response.addEntity(HttpEntity.dumpStackTrace(cause));
    }

    void handleFailure(final Throwable cause) {
        this.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.setMessageOrDefault(cause.getMessage()));
        this.response.addEntity(HttpEntity.dumpStackTrace(cause));
    }

    // set status, headers, body........................................................................................

    /**
     * Sets the status and message to match the content.
     */
    private void setStatusAndBody(final String content,
                                  final Class<?> contentValueType,
                                  final HttpMethod method) {
        final HttpStatusCode statusCode;
        final String contentTypeText = contentValueType.getSimpleName();

        HttpEntity entity;
        if (null != content) {
            statusCode = HttpStatusCode.OK;

            entity = HttpEntity.EMPTY
                    .addHeader(HttpHeaderName.CONTENT_TYPE,
                            MediaType.APPLICATION_JSON.setCharset(this.selectCharsetName()))
                    .addHeader(JsonHttpRequestHttpResponseBiConsumers.X_CONTENT_TYPE_NAME, contentTypeText) // this header is used a hint about the response.
                    .setBodyText(content)
                    .setContentLength();
        } else {
            statusCode = HttpStatusCode.NO_CONTENT;
            entity = HttpEntity.EMPTY;
        }

        this.setStatus(statusCode.setMessage(method + " " + contentTypeText + " " + statusCode.status().message()));

        if (!entity.isEmpty()) {
            this.response.addEntity(entity);
        }
    }

    private CharsetName selectCharsetName() {
        final AcceptCharset acceptCharset = HttpHeaderName.ACCEPT_CHARSET.header(this.request)
                .orElse(UTF8);
        final Optional<Charset> charset = acceptCharset.charset();
        if (!charset.isPresent()) {
            throw new NotAcceptableHeaderException("AcceptCharset " + acceptCharset + " contain unsupported charset");
        }
        return CharsetName.with(charset.get().name());
    }

    private final static AcceptCharset UTF8 = AcceptCharset.parse("utf-8");

    void setStatus(final HttpStatusCode statusCode,
                   final String message) {
        this.setStatus(statusCode.setMessageOrDefault(message)); // message could be null if Exception#getMessage
    }

    void setStatus(final HttpStatus status) {
        this.response.setStatus(status);
    }

    private final HttpRequest request;
    private final HttpResponse response;
}
