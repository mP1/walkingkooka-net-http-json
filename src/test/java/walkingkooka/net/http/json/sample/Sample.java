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

package walkingkooka.net.http.json.sample;

import walkingkooka.net.AbsoluteUrl;
import walkingkooka.net.RelativeUrl;
import walkingkooka.net.Url;
import walkingkooka.net.header.CharsetName;
import walkingkooka.net.header.HttpHeaderName;
import walkingkooka.net.header.MediaType;
import walkingkooka.net.http.HttpEntity;
import walkingkooka.net.http.HttpProtocolVersion;
import walkingkooka.net.http.HttpStatusCode;
import walkingkooka.net.http.HttpTransport;
import walkingkooka.net.http.json.JsonHttpRequestHttpResponseBiConsumers;
import walkingkooka.net.http.server.HttpRequest;
import walkingkooka.net.http.server.HttpRequests;
import walkingkooka.net.http.server.HttpResponse;
import walkingkooka.net.http.server.HttpResponses;
import walkingkooka.tree.expression.ExpressionNumberContexts;
import walkingkooka.tree.json.marshall.JsonNodeMarshallContext;
import walkingkooka.tree.json.marshall.JsonNodeMarshallContexts;
import walkingkooka.tree.json.marshall.JsonNodeUnmarshallContext;
import walkingkooka.tree.json.marshall.JsonNodeUnmarshallContexts;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Sample {

    public static void main(final String[] args) {
        test();
    }

    private static void test() {
        final RelativeUrl input = Url.parseRelative("/input");
        final AbsoluteUrl output = Url.parseAbsolute("https://example/output");

        final Function<RelativeUrl, AbsoluteUrl> handler = (i) -> {
            assertEquals(input, i);
            return output;
        };

        final JsonNodeMarshallContext marshallContext = JsonNodeMarshallContexts.basic();
        final JsonNodeUnmarshallContext unmarshallContext = JsonNodeUnmarshallContexts.basic(ExpressionNumberContexts.fake());

        final BiConsumer<HttpRequest, HttpResponse> consumer = JsonHttpRequestHttpResponseBiConsumers.postRequestBody(handler,
                RelativeUrl.class,
                AbsoluteUrl.class,
                marshallContext,
                unmarshallContext);

        final HttpRequest request = HttpRequests.post(HttpTransport.UNSECURED,
                Url.parseRelative("/handler"),
                HttpProtocolVersion.VERSION_1_0,
                HttpEntity.EMPTY
                        .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .setBodyText(marshallContext.marshall(input)
                                .toString())
                        .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        consumer.accept(request, response);

        final String responseBody = marshallContext.marshall(output).toString();

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.OK.setMessage("POST AbsoluteUrl OK"));
        expected.setVersion(HttpProtocolVersion.VERSION_1_0);
        expected.addEntity(HttpEntity.EMPTY
                .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.APPLICATION_JSON.setCharset(CharsetName.UTF_8))
                .addHeader(JsonHttpRequestHttpResponseBiConsumers.X_CONTENT_TYPE_NAME, output.getClass().getSimpleName())
                .setBodyText(responseBody)
                .setContentLength());

        assertEquals(expected, response, "response");
    }
}
