package org.apache.skywalking.apm.toolkit.activation.micrometer;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SpanHelperTest {

    @Test
    public void testTryToGetPeer() {
        String remoteName = SpanHelper.tryToGetPeer("http://localhost:8080", "remoteName",
            KeyValues.of(KeyValue.of("http.url", "http://localhost:8081")));
        assertThat(remoteName, is("http://localhost:8080"));
    }

    @Test
    public void testTryToGetPeerWhenRemoteAddressIsNull() {
        String remoteName = SpanHelper.tryToGetPeer(null, "remoteName",
            KeyValues.of(KeyValue.of("http.url", "http://localhost:8080")));
        assertThat(remoteName, is("http://localhost:8080"));
    }

    @Test
    public void testTryToGetPeerWhenURIWithQueryString() {
        String remoteName = SpanHelper.tryToGetPeer(null, "remoteName",
            KeyValues.of(KeyValue.of("http.url", "http://localhost:8080?a=b")));
        assertThat(remoteName, is("http://localhost:8080"));
    }

    @Test
    public void testTryToGetPeerWhenLackOfURIComponents() {
        String remoteName = SpanHelper.tryToGetPeer(null, "remoteName",
            KeyValues.empty());
        assertThat(remoteName, is("remoteName"));
    }

    @Test
    public void testTryToGetPeerWhenLackOfURIComponentsAndRemoteName() {
        String remoteName = SpanHelper.tryToGetPeer(null, null,
            KeyValues.of(KeyValue.of("http.url", "/post")));
        assertThat(remoteName, is("/post"));
    }

}
