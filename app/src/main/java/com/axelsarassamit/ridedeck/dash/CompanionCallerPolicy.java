package com.axelsarassamit.ridedeck.dash;

/** Package and certificate must both match. Debug identities never authorize a release Dash. */
final class CompanionCallerPolicy {
    static final String HOST_CERT = "79f76edba53e22dcb9e4610dc5fa151e5c52d0895c68b2802444f1ced3a1c06f";
    static String canonicalHost(String name, String certificate, boolean debugDash, String dashCertificate) {
        for (String host : new String[]{RideSessionPolicy.STANDARD, RideSessionPolicy.YAMAHA}) {
            if (host.equals(name) && HOST_CERT.equals(certificate)) return host;
            if (debugDash && (host + ".preview").equals(name) && certificate != null
                    && !certificate.isEmpty() && certificate.equals(dashCertificate)) return host;
        }
        return null;
    }
}
