package com.tricare.manuals.data.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u0004\u0018\u00010\b2\u0006\u0010\n\u001a\u00020\bJ\u000e\u0010\u000b\u001a\u00020\f2\u0006\u0010\n\u001a\u00020\bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/tricare/manuals/data/network/TricareWebClient;", "", "()V", "client", "Lokhttp3/OkHttpClient;", "dodTrustManager", "Ljavax/net/ssl/X509TrustManager;", "userAgent", "", "fetchHtml", "url", "headCheck", "", "app_debug"})
public final class TricareWebClient {
    @org.jetbrains.annotations.NotNull()
    private final javax.net.ssl.X509TrustManager dodTrustManager = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient client = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    
    @javax.inject.Inject()
    public TricareWebClient() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String fetchHtml(@org.jetbrains.annotations.NotNull()
    java.lang.String url) {
        return null;
    }
    
    public final boolean headCheck(@org.jetbrains.annotations.NotNull()
    java.lang.String url) {
        return false;
    }
}