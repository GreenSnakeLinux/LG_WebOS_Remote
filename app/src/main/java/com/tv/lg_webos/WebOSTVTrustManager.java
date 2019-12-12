package com.tv.lg_webos;

import android.util.Log;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.X509TrustManager;

public class WebOSTVTrustManager implements X509TrustManager {
    private X509Certificate expectedCert;
    private X509Certificate lastCheckedCert;
    private final static String WEB_OS = "WEB_OS";

    public void setExpectedCertificate(X509Certificate cert) {
        this.expectedCert = cert;
    }

    public X509Certificate getLastCheckedCertificate () {
        return lastCheckedCert;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        Log.d(WEB_OS, "Expecting device cert " + (expectedCert != null ? expectedCert.getSubjectDN() : "(any)"));

        if (chain != null && chain.length > 0) {
            X509Certificate cert = chain[0];

            lastCheckedCert = cert;

            if (expectedCert != null) {
                byte [] certBytes = cert.getEncoded();
                byte [] expectedCertBytes = expectedCert.getEncoded();

                Log.d(WEB_OS, "Device presented cert " + cert.getSubjectDN());

                if (!Arrays.equals(certBytes, expectedCertBytes)) {
                    throw new CertificateException("certificate does not match");
                }
            }
        } else {
            lastCheckedCert = null;
            throw new CertificateException("no server certificate");
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}