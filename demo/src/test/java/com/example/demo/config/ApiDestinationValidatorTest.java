package com.example.demo.config;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiDestinationValidatorTest {

    @Test
    void shouldAcceptExplicitlyAllowedPublicHostAndPort() throws Exception {
        ExportApiProperties properties = properties(
                "https://api.example.com:8443/v1/",
                "https",
                "api.example.com:8443"
        );
        ApiDestinationValidator validator = validator(properties, "93.184.216.34");

        URI destination = validator.validateConfiguredDestination();

        assertEquals("https://api.example.com:8443/v1", destination.toString());
    }

    @Test
    void shouldAcceptHttpOnlyWhenEnabledForTheEnvironment() {
        ExportApiProperties properties = properties(
                "http://api.example.com:8080",
                "http",
                "api.example.com:8080"
        );

        URI destination = validator(properties, "93.184.216.34").validateConfiguredDestination();

        assertEquals("http://api.example.com:8080", destination.toString());
    }

    @Test
    void shouldRejectHostNotInAllowlist() {
        ExportApiProperties properties = properties(
                "https://evil.example:443",
                "https",
                "api.example.com:443"
        );

        assertInvalid(validator(properties, "93.184.216.34"));
    }

    @Test
    void shouldRejectLocalhost() {
        ExportApiProperties properties = properties(
                "https://localhost:443",
                "https",
                "localhost:443"
        );

        assertInvalid(validator(properties, "127.0.0.1"));
    }

    @Test
    void shouldRejectPrivateIpv4() {
        ExportApiProperties properties = properties(
                "https://10.20.30.40:443",
                "https",
                "10.20.30.40:443"
        );

        assertInvalid(validator(properties, "10.20.30.40"));
    }

    @Test
    void shouldRejectPrivateIpv6() {
        ExportApiProperties properties = properties(
                "https://[fd00::1]:443",
                "https",
                "[fd00::1]:443"
        );

        assertInvalid(validator(properties, "fd00::1"));
    }

    @Test
    void shouldRejectUrlWithCredentials() {
        ExportApiProperties properties = properties(
                "https://user:secret@api.example.com:443",
                "https",
                "api.example.com:443"
        );

        assertInvalid(validator(properties, "93.184.216.34"));
    }

    @Test
    void shouldRejectForbiddenSchemes() {
        for (String scheme : new String[]{"file", "jar", "ftp", "gopher"}) {
            ExportApiProperties properties = properties(
                    scheme + "://api.example.com:443/resource",
                    "https",
                    "api.example.com:443"
            );
            assertInvalid(validator(properties, "93.184.216.34"));
        }
    }

    @Test
    void shouldRejectMalformedAndAmbiguousUrls() {
        String[] urls = {
                "https://",
                "https://api.example.com:bad",
                "https://api.example.com:443/path/../admin",
                "https://api.example.com:443/path%2fadmin",
                " https://api.example.com:443",
                "https://api.example.com:443/#fragment",
                "https://api.example.com:443?token=secret",
                "https://api.example.com:0",
                "https://api.example.com:99999",
                "https://2130706433:443"
        };

        for (String url : urls) {
            ExportApiProperties properties = properties(url, "https", "api.example.com:443");
            assertInvalid(validator(properties, "93.184.216.34"));
        }
    }

    @Test
    void shouldRejectWhenAnyDnsAnswerIsPrivate() {
        ExportApiProperties properties = properties(
                "https://api.example.com:443",
                "https",
                "api.example.com:443"
        );
        ApiDestinationValidator validator = new ApiDestinationValidator(
                properties,
                host -> new InetAddress[]{
                        InetAddress.getByName("93.184.216.34"),
                        InetAddress.getByName("192.168.1.10")
                }
        );

        assertInvalid(validator);
    }

    private ExportApiProperties properties(String baseUrl, String scheme, String authority) {
        ExportApiProperties properties = new ExportApiProperties();
        properties.setBaseUrl(baseUrl);
        properties.setAllowedSchemes(java.util.List.of(scheme));
        properties.setAllowedAuthorities(java.util.List.of(authority));
        return properties;
    }

    private ApiDestinationValidator validator(ExportApiProperties properties, String resolvedAddress) {
        return new ApiDestinationValidator(
                properties,
                host -> new InetAddress[]{InetAddress.getByName(resolvedAddress)}
        );
    }

    private void assertInvalid(ApiDestinationValidator validator) {
        assertThrows(InvalidApiDestinationException.class, validator::validateConfiguredDestination);
    }
}
