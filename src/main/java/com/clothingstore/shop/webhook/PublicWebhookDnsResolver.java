package com.clothingstore.shop.webhook;

import org.apache.hc.client5.http.DnsResolver;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Rejects unsafe destinations as part of the HTTP client's actual connection resolution. */
@Component
public class PublicWebhookDnsResolver implements DnsResolver {
    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(host);
        if (addresses.length == 0) {
            throw new UnknownHostException("Webhook host has no addresses");
        }
        for (InetAddress address : addresses) {
            if (!isPublic(address)) {
                throw new UnknownHostException("Webhook host resolved to a non-public address");
            }
        }
        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) {
        return host;
    }

    private boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() ||
                address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc) {
            return false; // IPv6 unique local range
        }
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first != 0 && first != 10 && first != 127 && first < 224 &&
                !(first == 100 && second >= 64 && second <= 127) &&
                !(first == 169 && second == 254) &&
                !(first == 172 && second >= 16 && second <= 31) &&
                !(first == 192 && second == 168) &&
                !(first == 198 && (second == 18 || second == 19));
    }
}
