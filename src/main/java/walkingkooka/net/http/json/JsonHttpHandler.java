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

import walkingkooka.net.header.AcceptCharset;
import walkingkooka.net.header.CharsetName;
import walkingkooka.net.header.HttpHeaderName;
import walkingkooka.net.header.MediaType;
import walkingkooka.net.header.NotAcceptableHeaderException;
import walkingkooka.net.http.HttpEntity;
import walkingkooka.net.http.HttpStatusCode;
import walkingkooka.net.http.server.HttpHandler;
import walkingkooka.net.http.server.HttpRequest;
import walkingkooka.net.http.server.HttpResponse;
import walkingkooka.tree.json.JsonNode;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A handler that assumes the request contains json, and after parsing invokes the provided handler.
 */
final class JsonHttpHandler implements HttpHandler {

    static JsonHttpHandler with(final Function<JsonNode, JsonNode> handler,
                                final Function<HttpEntity, HttpEntity> post) {
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(post, "post");

        return new JsonHttpHandler(handler, post);
    }

    private JsonHttpHandler(final Function<JsonNode, JsonNode> handler,
                            final Function<HttpEntity, HttpEntity> post) {
        super();

        this.handler = handler;
        this.post = post;
    }

    @Override
    public void handle(final HttpRequest request,
                       final HttpResponse response) {
        final String body = resourceTextOrBadRequest(request, response);
        if (null != body) {
            JsonNode json = null;
            try {
                json = JsonNode.parse(body);
            } catch (final Exception parseFail) {
                response.setStatus(HttpStatusCode.BAD_REQUEST.setMessage(parseFail.getMessage()));
                response.addEntity(HttpEntity.EMPTY);
            }

            if (null != json) {
                final JsonNode output = this.handler.apply(json);

                final HttpStatusCode statusCode;
                final HttpEntity entity;

                if (null == output) {
                    statusCode = HttpStatusCode.NO_CONTENT;
                    entity = HttpEntity.EMPTY;
                } else {
                    statusCode = HttpStatusCode.OK;
                    entity = HttpEntity.EMPTY
                            .setContentType(
                                    MediaType.APPLICATION_JSON.setCharset(
                                            selectCharsetName(request)
                                    )
                            ).setBodyText(output.toString())
                            .setContentLength();
                }

                final HttpEntity post = this.post.apply(entity);

                response.setStatus(statusCode.status());
                response.addEntity(post);
            }
        }
    }

    /**
     * Reads and returns the body as text, with null signifying an error occurred and a bad request response set.
     */
    private static String resourceTextOrBadRequest(final HttpRequest request, final HttpResponse response) {
        String bodyText;
        try {
            bodyText = request.bodyText();
        } catch (final RuntimeException cause) {
            bodyText = badRequest(
                    "Invalid content: " + cause.getMessage(),
                    cause,
                    response
            );
        }

        if (null != bodyText) {
            final Long contentLength = HttpHeaderName.CONTENT_LENGTH.header(request).orElse(null);
            if (bodyText.isEmpty()) {
                bodyText = badRequest(
                        "Required body missing",
                        response
                );

            } else {
                if (null == contentLength) {
                    response.setStatus(HttpStatusCode.LENGTH_REQUIRED.status());
                    response.addEntity(HttpEntity.EMPTY);
                    bodyText = null;
                } else {
                    final long bodyLength = request.bodyLength();
                    final long contentLengthLong = contentLength.longValue();
                    if (bodyLength != contentLengthLong) {
                        bodyText = badRequest(
                                HttpHeaderName.CONTENT_LENGTH + ": " + contentLengthLong + " != body length=" + bodyLength + " mismatch",
                                response
                        );
                    }
                }
            }
        }

        return bodyText;
    }

    /**
     * Attempts to select the appropriate {@link CharsetName} or dfaults to UTF-8.
     */
    private static CharsetName selectCharsetName(final HttpRequest request) {
        final AcceptCharset acceptCharset = HttpHeaderName.ACCEPT_CHARSET.header(request)
                .orElse(UTF8);
        final Optional<Charset> charset = acceptCharset.charset();
        if (!charset.isPresent()) {
            throw new NotAcceptableHeaderException("AcceptCharset " + acceptCharset + " contain unsupported charset");
        }
        return CharsetName.with(charset.get().name());
    }

    private final static AcceptCharset UTF8 = AcceptCharset.parse("utf-8");

    private static String badRequest(final String message,
                                     final Throwable cause,
                                     final HttpResponse response) {
        response.setStatus(HttpStatusCode.BAD_REQUEST.setMessage(message));
        response.addEntity(null != cause ? HttpEntity.dumpStackTrace(cause) : HttpEntity.EMPTY);
        return null;
    }

    private static String badRequest(final String message,
                                     final HttpResponse response) {
        return badRequest(message, null, response);
    }

    /**
     * The function that is invoked if the request body can be parsed into a {@link JsonNode}.
     */
    private final Function<JsonNode, JsonNode> handler;

    /**
     * This is called after the JSON response is written providing an opportunity to modify, performing actions
     * such as adding headers.
     */
    private final Function<HttpEntity, HttpEntity> post;

    @Override
    public String toString() {
        return this.handler + " " + this.post;
    }
}
