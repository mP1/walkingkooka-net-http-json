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

import javaemul.internal.annotations.GwtIncompatible;
import walkingkooka.net.header.HttpHeaderName;
import walkingkooka.net.header.MediaType;
import walkingkooka.net.http.HttpEntity;
import walkingkooka.net.http.server.HttpResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

abstract class JsonHttpRequestHttpResponseBiConsumerRequestStackTrace extends JsonHttpRequestHttpResponseBiConsumerRequestStackTraceJ2cl {

    /**
     * Javascript JRE does not support capturing stacktraces so skip this step.
     */
    @GwtIncompatible
    static void setResponseBody(final HttpResponse response,
                                final Throwable cause) {
        try (final StringWriter stringWriter = new StringWriter()) {
            try (final PrintWriter printWriter = new PrintWriter(stringWriter)) {
                cause.printStackTrace(printWriter);
                printWriter.flush();

                response.addEntity(HttpEntity.EMPTY
                        .addHeader(HttpHeaderName.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                        .setBodyText(stringWriter.toString())
                        .setContentLength());
            }
        } catch (final IOException never) {
        }
    }
}
