package test;

import com.google.gwt.junit.client.GWTTestCase;

import walkingkooka.j2cl.locale.LocaleAware;
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

@LocaleAware
public class TestGwtTest extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "test.Test";
    }

    public void testAssertEquals() {
        assertEquals(
            1,
            1
        );
    }

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

        assertEquals(
            expected,
            response
        );
    }
}
