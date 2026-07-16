package com.example.demo.config;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ApiDestinationValidator {

    @FunctionalInterface
    interface DnsResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private final ExportApiProperties properties;
    private final DnsResolver dnsResolver;

    @Autowired
    public ApiDestinationValidator(ExportApiProperties properties) {
        this(properties, InetAddress::getAllByName);
    }

    ApiDestinationValidator(ExportApiProperties properties, DnsResolver dnsResolver) {
        this.properties = properties;
        this.dnsResolver = dnsResolver;
    }

    public URI validateConfiguredDestination() {
        return validate(properties.getBaseUrl());
    }

    URI validate(String rawUrl) {
        try {
            if (rawUrl == null || rawUrl.isBlank() || !rawUrl.equals(rawUrl.trim())
                    || containsControlOrWhitespace(rawUrl) || rawUrl.contains("\\")) {
                throw new InvalidApiDestinationException();
            }

            URI uri = new URI(rawUrl);
            if (!uri.isAbsolute() || uri.isOpaque() || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                    || uri.getRawQuery() != null) {
                throw new InvalidApiDestinationException();
            }

            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            Set<String> allowedSchemes = normalizeSchemes(properties.getAllowedSchemes());
            if (!allowedSchemes.contains(scheme)) {
                throw new InvalidApiDestinationException();
            }

            String host = normalizeHost(uri.getHost());
            rejectAmbiguousNumericHost(host);
            int port = effectivePort(uri, scheme);
            if (!isAllowedAuthority(host, port, properties.getAllowedAuthorities())) {
                throw new InvalidApiDestinationException();
            }

            validatePath(uri);
            validateResolvedAddresses(host);

            String normalizedPath = normalizeBasePath(uri.getRawPath());
            return new URI(scheme, null, host, port, normalizedPath, null, null);
        } catch (InvalidApiDestinationException ex) {
            throw ex;
        } catch (IllegalArgumentException | URISyntaxException | UnknownHostException ex) {
            throw new InvalidApiDestinationException(ex);
        }
    }

    private Set<String> normalizeSchemes(List<String> schemes) {
        Set<String> normalized = new HashSet<>();
        if (schemes != null) {
            for (String scheme : schemes) {
                if (scheme != null && !scheme.isBlank()) {
                    normalized.add(scheme.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return normalized;
    }

    private boolean isAllowedAuthority(String host, int port, List<String> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }

        for (String authority : authorities) {
            if (authority == null || authority.isBlank()) {
                continue;
            }
            try {
                URI parsed = new URI("https://" + authority.trim());
                if (parsed.getHost() == null || parsed.getPort() < 1 || parsed.getRawUserInfo() != null
                        || parsed.getRawPath() == null || !parsed.getRawPath().isEmpty()
                        || parsed.getRawQuery() != null || parsed.getRawFragment() != null) {
                    continue;
                }
                if (normalizeHost(parsed.getHost()).equals(host) && parsed.getPort() == port) {
                    return true;
                }
            } catch (IllegalArgumentException | URISyntaxException ignored) {
                // Uma entrada invalida da allowlist nunca autoriza um destino.
            }
        }
        return false;
    }

    private String normalizeHost(String host) {
        String withoutBrackets = host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
        if (withoutBrackets.contains("%")) {
            throw new InvalidApiDestinationException();
        }
        if (withoutBrackets.contains(":")) {
            return withoutBrackets.toLowerCase(Locale.ROOT);
        }
        return IDN.toASCII(withoutBrackets, IDN.USE_STD3_ASCII_RULES)
                .toLowerCase(Locale.ROOT);
    }

    private int effectivePort(URI uri, String scheme) {
        if (uri.getPort() != -1) {
            if (uri.getPort() < 1 || uri.getPort() > 65_535) {
                throw new InvalidApiDestinationException();
            }
            return uri.getPort();
        }
        return "https".equals(scheme) ? 443 : 80;
    }

    private void validatePath(URI uri) {
        String rawPath = uri.getRawPath();
        if (rawPath == null || rawPath.isEmpty()) {
            return;
        }
        String lowerPath = rawPath.toLowerCase(Locale.ROOT);
        if (lowerPath.contains("%2f") || lowerPath.contains("%5c")
                || !uri.normalize().getRawPath().equals(rawPath)) {
            throw new InvalidApiDestinationException();
        }
    }

    private String normalizeBasePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty() || "/".equals(rawPath)) {
            return "";
        }
        int end = rawPath.length();
        while (end > 0 && rawPath.charAt(end - 1) == '/') {
            end--;
        }
        return rawPath.substring(0, end);
    }

    private void validateResolvedAddresses(String host) throws UnknownHostException {
        InetAddress[] addresses = dnsResolver.resolve(host);
        if (addresses == null || addresses.length == 0) {
            throw new InvalidApiDestinationException();
        }
        for (InetAddress address : addresses) {
            if (address == null || !isPublicAddress(address.getAddress())) {
                throw new InvalidApiDestinationException();
            }
        }
    }

    private boolean isPublicAddress(byte[] address) {
        if (address.length == 4) {
            return isPublicIpv4(address);
        }
        if (address.length != 16) {
            return false;
        }

        int first = unsigned(address[0]);
        int second = unsigned(address[1]);

        // Somente unicast global 2000::/3; exclui ULA, link-local, multicast,
        // loopback, unspecified, IPv4-mapped e demais faixas especiais.
        if ((first & 0xe0) != 0x20) {
            return false;
        }

        // 2001:0000::/23 contem faixas especiais/reservadas.
        if (first == 0x20 && second == 0x01 && (unsigned(address[2]) & 0xfe) == 0) {
            return false;
        }

        // 2002::/16 (6to4, descontinuado).
        if (first == 0x20 && second == 0x02) {
            return false;
        }

        // 2001:db8::/32 (documentacao).
        return !(first == 0x20 && second == 0x01
                && unsigned(address[2]) == 0x0d && unsigned(address[3]) == 0xb8);
    }

    private boolean isPublicIpv4(byte[] address) {
        int a = unsigned(address[0]);
        int b = unsigned(address[1]);
        int c = unsigned(address[2]);

        if (a == 0 || a == 10 || a == 127 || a >= 224) {
            return false;
        }
        if (a == 100 && b >= 64 && b <= 127) {
            return false;
        }
        if (a == 169 && b == 254) {
            return false;
        }
        if (a == 172 && b >= 16 && b <= 31) {
            return false;
        }
        if (a == 192 && b == 168) {
            return false;
        }
        if (a == 192 && b == 0 && c == 0) {
            return false;
        }
        if (a == 192 && b == 0 && c == 2) {
            return false;
        }
        if (a == 192 && b == 31 && c == 196) {
            return false;
        }
        if (a == 192 && b == 52 && c == 193) {
            return false;
        }
        if (a == 192 && b == 88 && c == 99) {
            return false;
        }
        if (a == 192 && b == 175 && c == 48) {
            return false;
        }
        if (a == 198 && (b == 18 || b == 19)) {
            return false;
        }
        if (a == 198 && b == 51 && c == 100) {
            return false;
        }
        return !(a == 203 && b == 0 && c == 113);
    }

    private void rejectAmbiguousNumericHost(String host) {
        if (!host.matches("[0-9.]+")) {
            return;
        }
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            throw new InvalidApiDestinationException();
        }
        for (String part : parts) {
            if (part.isEmpty() || (part.length() > 1 && part.startsWith("0"))) {
                throw new InvalidApiDestinationException();
            }
            int value = Integer.parseInt(part);
            if (value < 0 || value > 255) {
                throw new InvalidApiDestinationException();
            }
        }
    }

    private boolean containsControlOrWhitespace(String value) {
        return value.codePoints().anyMatch(codePoint ->
                Character.isISOControl(codePoint) || Character.isWhitespace(codePoint));
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
