/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.http.client;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Configuration class to use with a {@link AsyncHttpClient}. System property can be also used to configure this
 * object default behavior by doing:
 * <p/>
 * -Dcom.ning.http.client.AsyncHttpClientConfig.nameOfTheProperty
 * ex:
 * <p/>
 * -Dcom.ning.http.client.AsyncHttpClientConfig.defaultMaxTotalConnections
 * -Dcom.ning.http.client.AsyncHttpClientConfig.defaultMaxTotalConnections
 * -Dcom.ning.http.client.AsyncHttpClientConfig.defaultMaxConnectionsPerHost
 * -Dcom.ning.http.client.AsyncHttpClientConfig.defaultConnectionTimeoutInMS
 * -Dcom.ning.http.client.AsyncHttpClientConfig.defaultIdleConnectionTimeoutInMS
 * -Dcom.ning.http.client.AsyncHttpClientConfig.defaultRequestTimeoutInMS
 * -Dcom.ning.http.client.AsyncHttpClientConfig.defaultRedirectsEnabled
 * -Dcom.ning.http.client.AsyncHttpClientConfig.defaultMaxRedirects
 */
public class AsyncHttpClientConfig {

    private final static String ASYNC_CLIENT = AsyncHttpClientConfig.class.getName() + ".";

    private final int maxTotalConnections;
    private final int maxConnectionPerHost;
    private final int connectionTimeOutInMs;
    private final int idleConnectionTimeoutInMs;
    private final int requestTimeoutInMs;
    private final boolean redirectEnabled;
    private final int maxDefaultRedirects;
    private final boolean compressionEnabled;
    private final String userAgent;
    private final boolean keepAlive;
    private final ScheduledExecutorService reaper;
    private final ExecutorService applicationThreadPool;
    private final ProxyServer proxyServer;
    private final SSLContext sslContext;
    private final SSLEngineFactory sslEngineFactory;
    private final AsyncHttpProviderConfig<?, ?> providerConfig;
    private final ConnectionsPool<?, ?> connectionsPool;

    private AsyncHttpClientConfig(int maxTotalConnections,
                                  int maxConnectionPerHost,
                                  int connectionTimeOutInMs,
                                  int idleConnectionTimeoutInMs,
                                  int requestTimeoutInMs,
                                  boolean redirectEnabled,
                                  int maxDefaultRedirects,
                                  boolean compressionEnabled,
                                  String userAgent,
                                  boolean keepAlive,
                                  ScheduledExecutorService reaper,
                                  ExecutorService applicationThreadPool,
                                  ProxyServer proxyServer,
                                  SSLContext sslContext,
                                  SSLEngineFactory sslEngineFactory,
                                  AsyncHttpProviderConfig<?,?> providerConfig,
                                  ConnectionsPool<?, ?> connectionsPool) {

        this.maxTotalConnections = maxTotalConnections;
        this.maxConnectionPerHost = maxConnectionPerHost;
        this.connectionTimeOutInMs = connectionTimeOutInMs;
        this.idleConnectionTimeoutInMs = idleConnectionTimeoutInMs;
        this.requestTimeoutInMs = requestTimeoutInMs;
        this.redirectEnabled = redirectEnabled;
        this.maxDefaultRedirects = maxDefaultRedirects;
        this.compressionEnabled = compressionEnabled;
        this.userAgent = userAgent;
        this.keepAlive = keepAlive;
        this.sslContext = sslContext;
        this.sslEngineFactory = sslEngineFactory;
        this.providerConfig = providerConfig;
        this.connectionsPool = connectionsPool;

        if (reaper == null) {
            this.reaper = Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){
                public Thread newThread(Runnable r) {
                    return new Thread(r,"AsyncHttpClient-Reaper");
                }
            });
        } else {
            this.reaper = reaper;
        }

        if (applicationThreadPool == null) {
            this.applicationThreadPool = Executors.newCachedThreadPool();
        } else {
            this.applicationThreadPool = applicationThreadPool;
        }
        this.proxyServer = proxyServer;
    }

    /**
     * A {@link ScheduledExecutorService} used to expire idle connections.
     *
     * @return {@link ScheduledExecutorService} 
     */
    public ScheduledExecutorService reaper() {
        return reaper;
    }

    /**
     * Return the maximum number of connections an {@link com.ning.http.client.AsyncHttpClient} can handle.
     *
     * @return the maximum number of connections an {@link com.ning.http.client.AsyncHttpClient} can handle.
     */
    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    /**
     * Return the maximum number of connections per hosts an {@link com.ning.http.client.AsyncHttpClient} can handle.
     *
     * @return the maximum number of connections per host an {@link com.ning.http.client.AsyncHttpClient} can handle.
     */
    public int getMaxConnectionPerHost() {
        return maxConnectionPerHost;
    }

    /**
     * Return the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can wait when connecting to a remote host
     *
     * @return the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can wait when connecting to a remote host
     */
    public int getConnectionTimeoutInMs() {
        return connectionTimeOutInMs;
    }

    /**
     * Return the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can stay idle.
     *
     * @return the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can stay idle.
     */
    public int getIdleConnectionTimeoutInMs() {
        return idleConnectionTimeoutInMs;
    }

    /**
     * Return the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} wait for a response
     *
     * @return the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} wait for a response
     */
    public int getRequestTimeoutInMs() {
        return requestTimeoutInMs;
    }

    /**
     * Is HTTP redirect enabled
     *
     * @return true if enabled.
     */
    public boolean isRedirectEnabled() {
        return redirectEnabled;
    }

    /**
     * Get the maximum number of HTTP redirect
     *
     * @return the maximum number of HTTP redirect
     */
    public int getMaxRedirects() {
        return maxDefaultRedirects;
    }

    /**
     * Is HTTP keep-alive enabled.
     *
     * @return true if keep-alive is enabled
     */
    public boolean getKeepAlive() {
        return keepAlive;
    }

    /**
     * Return the USER_AGENT header value
     *
     * @return the USER_AGENT header value
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Is HTTP compression enabled.
     *
     * @return true if compression is enabled
     */
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    /**
     * Return the {@link java.util.concurrent.ExecutorService} an {@link AsyncHttpClient} use for handling
     * asynchronous response.
     *
     * @return the {@link java.util.concurrent.ExecutorService} an {@link AsyncHttpClient} use for handling
     *         asynchronous response.
     */
    public ExecutorService executorService() {
        return applicationThreadPool;
    }

    /**
     * An instance of {@link com.ning.http.client.ProxyServer} used by an {@link AsyncHttpClient}
     *
     * @return instance of {@link com.ning.http.client.ProxyServer}
     */
    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    /**
     * Return an instance of {@link SSLContext} used for SSL connection.
     * @return an instance of {@link SSLContext} used for SSL connection.
     */
    public SSLContext getSSLContext() {
        return sslContext;
    }

    /**
     * Return an instance of {@link ConnectionsPool}
     * @return an instance of {@link ConnectionsPool}
     */
    public ConnectionsPool<?, ?> getConnectionsPool(){
        return connectionsPool;
    }
    
    /**
     * Return an instance of {@link SSLEngineFactory} used for SSL connection.
     * @return an instance of {@link SSLEngineFactory} used for SSL connection.
     */
    public SSLEngineFactory getSSLEngineFactory() {
        if (sslEngineFactory == null) {
            return new SSLEngineFactory()
            {
                public SSLEngine newSSLEngine()
                {
                    if (sslContext != null) {
                        SSLEngine sslEngine = sslContext.createSSLEngine();
                        sslEngine.setUseClientMode(true);
                        return sslEngine;
                    } else {
                        return null;
                    }
                }
            };
        }
        return sslEngineFactory;
    }

    /**
     * Return the {@link com.ning.http.client.AsyncHttpProviderConfig}
     * @return the {@link com.ning.http.client.AsyncHttpProviderConfig}
     */
    public AsyncHttpProviderConfig<?,?> getAsyncHttpProviderConfig() {
        return providerConfig;
    }

    /**
     * Builder for an {@link AsyncHttpClient}
     */
    public static class Builder {
        private int defaultMaxTotalConnections = Integer.getInteger(ASYNC_CLIENT + "defaultMaxTotalConnections", -1);
        private int defaultMaxConnectionPerHost = Integer.getInteger(ASYNC_CLIENT + "defaultMaxConnectionsPerHost", -1);
        private int defaultConnectionTimeOutInMs = Integer.getInteger(ASYNC_CLIENT + "defaultConnectionTimeoutInMS", 60 * 1000);
        private int defaultIdleConnectionTimeoutInMs = Integer.getInteger(ASYNC_CLIENT + "defaultIdleConnectionTimeoutInMS", 60 * 1000);
        private int defaultRequestTimeoutInMs = Integer.getInteger(ASYNC_CLIENT + "defaultRequestTimeoutInMS", 60 * 1000);
        private boolean redirectEnabled = Boolean.getBoolean(ASYNC_CLIENT + "defaultRedirectsEnabled");
        private int maxDefaultRedirects = Integer.getInteger(ASYNC_CLIENT + "defaultMaxRedirects", 5);
        private boolean compressionEnabled = Boolean.getBoolean(ASYNC_CLIENT + "compressionEnabled");
        private String userAgent = System.getProperty(ASYNC_CLIENT + "userAgent", "NING/1.0");
        private boolean keepAlive = true;
        private ScheduledExecutorService reaper = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
                new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "AsyncHttpClient-Reaper");
                    }
                });
        private ExecutorService applicationThreadPool = Executors.newCachedThreadPool();
        private ProxyServer proxyServer = null;
        private SSLContext sslContext;
        private SSLEngineFactory sslEngineFactory;
        private AsyncHttpProviderConfig<?,?> providerConfig;
        private ConnectionsPool<?, ?> connectionsPool;

        public Builder() {
        }

        /**
         * Set the maximum number of connections an {@link com.ning.http.client.AsyncHttpClient} can handle.
         *
         * @param defaultMaxTotalConnections the maximum number of connections an {@link com.ning.http.client.AsyncHttpClient} can handle.
         * @return a {@link Builder}
         */
        public Builder setMaximumConnectionsTotal(int defaultMaxTotalConnections) {
            this.defaultMaxTotalConnections = defaultMaxTotalConnections;
            return this;
        }

        /**
         * Set the maximum number of connections per hosts an {@link com.ning.http.client.AsyncHttpClient} can handle.
         *
         * @param defaultMaxConnectionPerHost the maximum number of connections per host an {@link com.ning.http.client.AsyncHttpClient} can handle.
         * @return a {@link Builder}
         */
        public Builder setMaximumConnectionsPerHost(int defaultMaxConnectionPerHost) {
            this.defaultMaxConnectionPerHost = defaultMaxConnectionPerHost;
            return this;
        }

        /**
         * Set the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can wait when connecting to a remote host
         *
         * @param defaultConnectionTimeOutInMs the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can wait when connecting to a remote host
         * @return a {@link Builder}
         */
        public Builder setConnectionTimeoutInMs(int defaultConnectionTimeOutInMs) {
            this.defaultConnectionTimeOutInMs = defaultConnectionTimeOutInMs;
            return this;
        }

        /**
         * Set the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can stay idle.
         *
         * @param defaultIdleConnectionTimeoutInMs
         *         the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can stay idle.
         * @return a {@link Builder}
         */
        public Builder setIdleConnectionTimeoutInMs(int defaultIdleConnectionTimeoutInMs) {
            this.defaultIdleConnectionTimeoutInMs = defaultIdleConnectionTimeoutInMs;
            return this;
        }

        /**
         * Set the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} wait for a response
         *
         * @param defaultRequestTimeoutInMs the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} wait for a response
         * @return a {@link Builder}
         */
        public Builder setRequestTimeoutInMs(int defaultRequestTimeoutInMs) {
            this.defaultRequestTimeoutInMs = defaultRequestTimeoutInMs;
            return this;
        }

        /**
         * Set to true to enable HTTP redirect
         *
         * @param redirectEnabled true if enabled.
         * @return a {@link Builder}
         */
        public Builder setFollowRedirects(boolean redirectEnabled) {
            this.redirectEnabled = redirectEnabled;
            return this;
        }

        /**
         * Set the maximum number of HTTP redirect
         *
         * @param maxDefaultRedirects the maximum number of HTTP redirect
         * @return a {@link Builder}
         */
        public Builder setMaximumNumberOfRedirects(int maxDefaultRedirects) {
            this.maxDefaultRedirects = maxDefaultRedirects;
            return this;
        }

        /**
         * Enable HTTP compression.
         *
         * @param compressionEnabled true if compression is enabled
         * @return a {@link Builder}
         */
        public Builder setCompressionEnabled(boolean compressionEnabled) {
            this.compressionEnabled = compressionEnabled;
            return this;
        }

        /**
         * Set the USER_AGENT header value
         *
         * @param userAgent the USER_AGENT header value
         * @return a {@link Builder}
         */
        public Builder setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Set HTTP keep-alive value.
         *
         * @param keepAlive true if keep-alive is enabled
         * @return a {@link Builder}
         */
        public Builder setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        /**
         * Set the{@link ScheduledExecutorService} used to expire idle connections.
         *
         * @param reaper the{@link ScheduledExecutorService} used to expire idle connections.
         * @return a {@link Builder}
         */
        public Builder setScheduledExecutorService(ScheduledExecutorService reaper) {
            if (this.reaper != null) this.reaper.shutdown();
            this.reaper = reaper;
            return this;
        }

        /**
         * Set the {@link java.util.concurrent.ExecutorService} an {@link AsyncHttpClient} use for handling
         * asynchronous response.
         *
         * @param applicationThreadPool the {@link java.util.concurrent.ExecutorService} an {@link AsyncHttpClient} use for handling
         *                              asynchronous response.
         * @return a {@link Builder}
         */
        public Builder setExecutorService(ExecutorService applicationThreadPool) {
            if (this.applicationThreadPool != null) this.applicationThreadPool.shutdown();
            this.applicationThreadPool = applicationThreadPool;
            return this;
        }

        /**
         * Set an instance of {@link com.ning.http.client.ProxyServer} used by an {@link AsyncHttpClient}
         *
         * @param proxyServer instance of {@link com.ning.http.client.ProxyServer}
         * @return a {@link Builder}
         */
        public Builder setProxyServer(ProxyServer proxyServer) {
            this.proxyServer = proxyServer;
            return this;
        }

        /**
         * Set the {@link SSLEngineFactory} for secure connection.
         * 
         * @param sslEngineFactory the {@link SSLEngineFactory} for secure connection
         * @return a {@link Builder}
         */
        public Builder setSSLEngineFactory(SSLEngineFactory sslEngineFactory){
            this.sslEngineFactory = sslEngineFactory;
            return this;
        }
        
        /**
         * Set the {@link SSLContext} for secure connection.
         * 
         * @param sslContext the {@link SSLContext} for secure connection
         * @return a {@link Builder}
         */
        public Builder setSSLContext(final SSLContext sslContext){
            this.sslEngineFactory = new SSLEngineFactory()
            {
                public SSLEngine newSSLEngine() throws GeneralSecurityException
                {
                    SSLEngine sslEngine = sslContext.createSSLEngine();
                    sslEngine.setUseClientMode(true);
                    return sslEngine;
                }
            };
            this.sslContext = sslContext;
            return this;
        }

        /**
         * Set the {@link com.ning.http.client.AsyncHttpProviderConfig}
         * @param providerConfig
         */
        public Builder setAsyncHttpClientProviderConfig(AsyncHttpProviderConfig<?, ?> providerConfig) {
            this.providerConfig = providerConfig;
            return this;
        }

        /**
         * Set the {@link com.ning.http.client.AsyncHttpProviderConfig}
         * @param connectionsPool
         */
        public Builder setConnectionsPool(ConnectionsPool<?, ?> connectionsPool) {
            this.connectionsPool = connectionsPool;
            return this;
        }

        /**
         * Build an {@link AsyncHttpClientConfig}
         *
         * @return an {@link AsyncHttpClientConfig}
         */
        public AsyncHttpClientConfig build() {
            return new AsyncHttpClientConfig(defaultMaxTotalConnections,
                    defaultMaxConnectionPerHost,
                    defaultConnectionTimeOutInMs,
                    defaultIdleConnectionTimeoutInMs,
                    defaultRequestTimeoutInMs,
                    redirectEnabled,
                    maxDefaultRedirects,
                    compressionEnabled,
                    userAgent,
                    keepAlive,
                    reaper,
                    applicationThreadPool,
                    proxyServer,
                    sslContext,
                    sslEngineFactory,
                    providerConfig,
                    connectionsPool);
        }

    }
}

