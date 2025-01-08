/*
 * Copyright © 2020 Miroslav Pokorny
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
 */
package test;

import com.google.j2cl.junit.apt.J2clTestInput;
import org.junit.Assert;
import org.junit.Test;

import walkingkooka.net.Url;
import walkingkooka.net.header.CharsetName;
import walkingkooka.net.header.HttpHeaderName;
import walkingkooka.net.header.MediaType;
import walkingkooka.net.http.HttpEntity;
import walkingkooka.net.http.HttpProtocolVersion;
import walkingkooka.net.http.HttpStatusCode;
import walkingkooka.net.http.HttpTransport;
import walkingkooka.net.http.json.JsonHttpHandlers;
import walkingkooka.net.http.server.HttpHandler;
import walkingkooka.net.http.server.HttpRequest;
import walkingkooka.net.http.server.HttpRequests;
import walkingkooka.net.http.server.HttpResponse;
import walkingkooka.net.http.server.HttpResponses;
import walkingkooka.tree.json.JsonNode;

import java.util.function.Function;

@J2clTestInput(JunitTest.class)
public class JunitTest {

    @Test
    public void testSuccessful() throws Exception {
        final JsonNode in = JsonNode.number(1);
        final JsonNode out = JsonNode.number(2);
        final HttpHandler handler = JsonHttpHandlers.json(
            (json) -> out,
            Function.identity()
        );

        final HttpRequest request = HttpRequests.post(HttpTransport.UNSECURED,
            Url.parseRelative("/handler"),
            HttpProtocolVersion.VERSION_1_0,
            HttpEntity.EMPTY
                .setContentType(MediaType.APPLICATION_JSON)
                .addHeader(HttpHeaderName.ACCEPT, MediaType.APPLICATION_JSON.accept())
                .setBodyText(in.toString())
                .setContentLength());

        final HttpResponse response = HttpResponses.recording();

        handler.handle(request, response);

        final String responseBody = out.toString();

        final HttpResponse expected = HttpResponses.recording();
        expected.setStatus(HttpStatusCode.OK.status());
        expected.setEntity(HttpEntity.EMPTY
            .setContentType(MediaType.APPLICATION_JSON.setCharset(CharsetName.UTF_8))
            .setBodyText(responseBody)
            .setContentLength());

        Assert.assertEquals(expected, response);
    }
}
