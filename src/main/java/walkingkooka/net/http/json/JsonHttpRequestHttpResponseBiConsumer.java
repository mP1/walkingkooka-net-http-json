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

import walkingkooka.net.http.HttpStatusCode;
import walkingkooka.net.http.server.HttpRequest;
import walkingkooka.net.http.server.HttpResponse;
import walkingkooka.tree.json.marshall.JsonNodeMarshallContext;
import walkingkooka.tree.json.marshall.JsonNodeUnmarshallContext;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Accepts a {@link HttpRequest} expecting JSON, handling the glue between marshalling and unmarshalling and invoking the given {@link Function}.
 */
final class JsonHttpRequestHttpResponseBiConsumer<I, O> implements BiConsumer<HttpRequest, HttpResponse> {

    static <I, O> JsonHttpRequestHttpResponseBiConsumer<I, O> with(final Function<I, O> handler,
                                                                   final Class<I> inputType,
                                                                   final Class<O> outputType,
                                                                   final JsonNodeMarshallContext marshallContext,
                                                                   final JsonNodeUnmarshallContext unmarshallContext) {
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(inputType, "inputType");
        Objects.requireNonNull(outputType, "outputType");
        Objects.requireNonNull(marshallContext, "marshallContext");
        Objects.requireNonNull(unmarshallContext, "unmarshallContext");

        return new JsonHttpRequestHttpResponseBiConsumer<>(handler,
                inputType,
                outputType,
                marshallContext,
                unmarshallContext);
    }


    private JsonHttpRequestHttpResponseBiConsumer(final Function<I, O> handler,
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

        this.handle(JsonHttpRequestHttpResponseBiConsumerRequest.with(request, response));
    }

    /**
     * Handles a request expecting JSON, unmarshalling the request body, calling the function and then marshalling the
     * result back to the response.
     */
    private void handle(final JsonHttpRequestHttpResponseBiConsumerRequest<I, O> request) {
        final String bodyText = request.resourceTextOrBadRequest();
        if (null != bodyText) {
            final JsonNodeUnmarshallContext unmarshallContext = this.unmarshallContext;
            final I input = request.resourceOrBadRequest(bodyText, this.inputType, unmarshallContext);
            if (null != input) {
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

    private final Class<I> inputType;
    private final Function<I, O> handler;
    private final JsonNodeMarshallContext marshallContext;
    private final JsonNodeUnmarshallContext unmarshallContext;

    @Override
    public String toString() {
        return this.handler.toString();
    }
}
