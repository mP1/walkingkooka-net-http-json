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
import walkingkooka.net.header.MediaType;
import walkingkooka.net.http.HttpMethod;
import walkingkooka.net.http.HttpStatusCode;
import walkingkooka.net.http.server.HttpRequest;
import walkingkooka.net.http.server.HttpResponse;
import walkingkooka.tree.json.marshall.JsonNodeMarshallContext;
import walkingkooka.tree.json.marshall.JsonNodeUnmarshallContext;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A handler that accepts a POST request, unmarshalling the body into a the given {@link Class input type} instance.
 * This instance is then passed to a {@link Function} which then returns another instance as its result. This result
 * is then marshalled into JSON and written to the response body.
 * Any {@link RuntimeException} that are thrown by the {@link Function} result in a {@link HttpStatusCode#INTERNAL_SERVER_ERROR},
 * with the stack trace written to the response body.
 * The following are requirements of the request.
 * <ul>
 * <li>Only POST method is allowed, others result in {#link HttpStatusCode#METHOD_NOT_ALLOWED} </li>
 * <li>Content-Type: application/json other types result in a @link HttpStatusCode#BAD_REQUEST</li>
 * <li>JSON Marshalling failures result in @link HttpStatusCode#BAD_REQUEST</li>
 * </ul>
 */
final class PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<I, O> implements BiConsumer<HttpRequest, HttpResponse> {

    static <I, O> PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<I, O> with(final Function<I, O> handler,
                                                                                  final Class<I> inputType,
                                                                                  final Class<O> outputType,
                                                                                  final JsonNodeMarshallContext marshallContext,
                                                                                  final JsonNodeUnmarshallContext unmarshallContext) {
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(inputType, "inputType");
        Objects.requireNonNull(outputType, "outputType");
        Objects.requireNonNull(marshallContext, "marshallContext");
        Objects.requireNonNull(unmarshallContext, "unmarshallContext");

        return new PostRequestBodyJsonHttpRequestHttpResponseBiConsumer<>(handler,
                inputType,
                outputType,
                marshallContext,
                unmarshallContext);
    }

    private PostRequestBodyJsonHttpRequestHttpResponseBiConsumer(final Function<I, O> handler,
                                                                 final Class<I> inputType,
                                                                 final Class<O> outputType,
                                                                 final JsonNodeMarshallContext marshallContext,
                                                                 final JsonNodeUnmarshallContext unmarshallContext) {
        super();

        this.handler = handler;
        this.inputType = inputType;
        this.marshallContext = marshallContext;
        this.unmarshallContext = unmarshallContext;
    }

    @Override
    public void accept(final HttpRequest request, final HttpResponse response) {
        response.setVersion(request.protocolVersion());

        this.handle(PostRequestBodyJsonHttpRequestHttpResponseBiConsumerRequest.with(request, response));
    }

    /**
     * Handles a request expecting JSON, unmarshalling the request body, calling the function and then marshalling the
     * result back to the response.
     */
    private void handle(final PostRequestBodyJsonHttpRequestHttpResponseBiConsumerRequest<I, O> request) {
        final HttpMethod method = request.postOrMethodNotAllowed();
        if (null != method) {
            final MediaType contentType = request.contentTypeApplicationJsonOrBadRequest();
            if (null != contentType) {
                final String bodyText = request.resourceTextOrBadRequest();
                if (null != bodyText) {
                    final JsonNodeUnmarshallContext unmarshallContext = this.unmarshallContext;
                    final I input = request.resourceOrBadRequest(bodyText, this.inputType, unmarshallContext);
                    if (null != input) {
                        final Accept accept = request.acceptApplicationJsonOrBadRequest();
                        if (null != accept) {
                            try {
                                final O output = this.handler.apply(input);
                                request.setStatus(HttpStatusCode.OK.status());
                                request.writeResponse(output, this.marshallContext);
                            } catch (final Exception cause) {
                                request.handleFailure(cause);
                            }
                        }
                    }
                }
            }
        }
    }

    private final Class<I> inputType;
    private final Function<I, O> handler;
    private final JsonNodeMarshallContext marshallContext;
    private final JsonNodeUnmarshallContext unmarshallContext;

    @Override
    public String toString() {
        return this.handler.toString();
    }
}
