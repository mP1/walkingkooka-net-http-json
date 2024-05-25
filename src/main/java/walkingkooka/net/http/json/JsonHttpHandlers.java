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

import walkingkooka.net.header.HttpHeaderName;
import walkingkooka.net.http.HttpEntity;
import walkingkooka.net.http.server.HttpHandler;
import walkingkooka.reflect.PublicStaticHelper;
import walkingkooka.tree.json.JsonNode;

import java.util.function.Function;

public final class JsonHttpHandlers implements PublicStaticHelper {

    /**
     * This header will appear in any successful JSON response and contains the simple java type name (Class#getSimpleName())
     * for the java object converted to JSON.
     */
    public final static HttpHeaderName<String> X_CONTENT_TYPE_NAME = HttpHeaderName.with("X-Content-Type-Name").stringValues();

    /**
     * {@see JsonHttpRequestHttpResponseBiConsumer}
     */
    public static HttpHandler json(final Function<JsonNode, JsonNode> handler,
                                   final Function<HttpEntity, HttpEntity> post) {
        return JsonHttpHandler.with(handler, post);
    }

    /**
     * Stop creation
     */
    private JsonHttpHandlers() {
        throw new UnsupportedOperationException();
    }
}
