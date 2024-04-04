/*
 * Copyright 2024 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.internal.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SchemeAndAuthorityTest {
    @ParameterizedTest
    @CsvSource({
            "0.0.0.0:80,        0.0.0.0:80,         0.0.0.0,            80",    // IPv4
            "[::1]:8080,        [::1]:8080,         [::1],              8080",  // IPv6
            "unix%3Afoo.sock,   unix%3Afoo.sock,    unix%3Afoo.sock,    -1",    // Domain socket
            "foo.bar,           foo.bar,            foo.bar,            -1",    // Only host
            "foo:,              foo:,               foo,                -1",    // Empty port
            "bar:80,            bar:80,             bar,                80",    // Host and port
            "foo@bar:80,        bar:80,             bar,                80",    // Userinfo and host and port
    })
    void fromAuthority(String authority, String expectedAuthority, String expectedHost,
                       int expectedPort) {
        assertThat(SchemeAndAuthority.fromAuthority(authority)).satisfies(uri -> {
            assertThat(uri.getScheme()).isNull();
            assertThat(uri.getAuthority()).isEqualTo(expectedAuthority);
            assertThat(uri.getHost()).isEqualTo(expectedHost);
            assertThat(uri.getPort()).isEqualTo(expectedPort);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "foo:bar", "http://foo:80", "foo/bar", "foo?bar=1", "foo#bar",
            "[192.168.0.1]", "[::1", "::1]", "[::1]%eth0", "unix:foo.sock"
    })
    void fromBadAuthority(String badAuthority) {
        assertThatThrownBy(() -> SchemeAndAuthority.fromAuthority(badAuthority))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({ "http", "https", "ftp", "mailto", "file", "data", "tel", "ssh" })
    void fromSchemeAndAuthority(String scheme) {
        assertThat(SchemeAndAuthority.fromSchemeAndAuthority(scheme, "foo")).satisfies(uri -> {
            assertThat(uri.getScheme()).isEqualTo(scheme);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "1http", "+http", ".http", "-http", "http!", "http$", "http?", "http#", "http ftp", "htTP", "HTTP"
    })
    void fromBadSchemeAndAuthority(String badScheme) {
        assertThatThrownBy(() -> SchemeAndAuthority.fromSchemeAndAuthority(badScheme, "foo"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
