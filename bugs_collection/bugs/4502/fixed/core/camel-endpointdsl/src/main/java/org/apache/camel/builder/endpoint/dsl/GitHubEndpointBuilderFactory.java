/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.builder.endpoint.dsl;

import javax.annotation.Generated;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.EndpointProducerBuilder;
import org.apache.camel.builder.endpoint.AbstractEndpointBuilder;
import org.apache.camel.spi.ExceptionHandler;

/**
 * The github component is used for integrating Camel with github.
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointDslMojo")
public interface GitHubEndpointBuilderFactory {


    /**
     * Builder for endpoint consumers for the GitHub component.
     */
    public interface GitHubEndpointConsumerBuilder
            extends
                EndpointConsumerBuilder {
        default AdvancedGitHubEndpointConsumerBuilder advanced() {
            return (AdvancedGitHubEndpointConsumerBuilder) this;
        }
        /**
         * What git operation to execute.
         * The option is a
         * <code>org.apache.camel.component.github.GitHubType</code> type.
         * @group common
         */
        default GitHubEndpointConsumerBuilder type(GitHubType type) {
            setProperty("type", type);
            return this;
        }
        /**
         * What git operation to execute.
         * The option will be converted to a
         * <code>org.apache.camel.component.github.GitHubType</code> type.
         * @group common
         */
        default GitHubEndpointConsumerBuilder type(String type) {
            setProperty("type", type);
            return this;
        }
        /**
         * Name of branch.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GitHubEndpointConsumerBuilder branchName(String branchName) {
            setProperty("branchName", branchName);
            return this;
        }
        /**
         * GitHub OAuth token, required unless username & password are provided.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointConsumerBuilder oauthToken(String oauthToken) {
            setProperty("oauthToken", oauthToken);
            return this;
        }
        /**
         * GitHub password, required unless oauthToken is provided.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointConsumerBuilder password(String password) {
            setProperty("password", password);
            return this;
        }
        /**
         * GitHub repository name.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointConsumerBuilder repoName(String repoName) {
            setProperty("repoName", repoName);
            return this;
        }
        /**
         * GitHub repository owner (organization).
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointConsumerBuilder repoOwner(String repoOwner) {
            setProperty("repoOwner", repoOwner);
            return this;
        }
        /**
         * GitHub username, required unless oauthToken is provided.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointConsumerBuilder username(String username) {
            setProperty("username", username);
            return this;
        }
        /**
         * Allows for bridging the consumer to the Camel routing Error Handler,
         * which mean any exceptions occurred while the consumer is trying to
         * pickup incoming messages, or the likes, will now be processed as a
         * message and handled by the routing Error Handler. By default the
         * consumer will use the org.apache.camel.spi.ExceptionHandler to deal
         * with exceptions, that will be logged at WARN or ERROR level and
         * ignored.
         * The option is a <code>boolean</code> type.
         * @group consumer
         */
        default GitHubEndpointConsumerBuilder bridgeErrorHandler(
                boolean bridgeErrorHandler) {
            setProperty("bridgeErrorHandler", bridgeErrorHandler);
            return this;
        }
        /**
         * Allows for bridging the consumer to the Camel routing Error Handler,
         * which mean any exceptions occurred while the consumer is trying to
         * pickup incoming messages, or the likes, will now be processed as a
         * message and handled by the routing Error Handler. By default the
         * consumer will use the org.apache.camel.spi.ExceptionHandler to deal
         * with exceptions, that will be logged at WARN or ERROR level and
         * ignored.
         * The option will be converted to a <code>boolean</code> type.
         * @group consumer
         */
        default GitHubEndpointConsumerBuilder bridgeErrorHandler(
                String bridgeErrorHandler) {
            setProperty("bridgeErrorHandler", bridgeErrorHandler);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint consumers for the GitHub component.
     */
    public interface AdvancedGitHubEndpointConsumerBuilder
            extends
                EndpointConsumerBuilder {
        default GitHubEndpointConsumerBuilder basic() {
            return (GitHubEndpointConsumerBuilder) this;
        }
        /**
         * To let the consumer use a custom ExceptionHandler. Notice if the
         * option bridgeErrorHandler is enabled then this option is not in use.
         * By default the consumer will deal with exceptions, that will be
         * logged at WARN or ERROR level and ignored.
         * The option is a <code>org.apache.camel.spi.ExceptionHandler</code>
         * type.
         * @group consumer (advanced)
         */
        default AdvancedGitHubEndpointConsumerBuilder exceptionHandler(
                ExceptionHandler exceptionHandler) {
            setProperty("exceptionHandler", exceptionHandler);
            return this;
        }
        /**
         * To let the consumer use a custom ExceptionHandler. Notice if the
         * option bridgeErrorHandler is enabled then this option is not in use.
         * By default the consumer will deal with exceptions, that will be
         * logged at WARN or ERROR level and ignored.
         * The option will be converted to a
         * <code>org.apache.camel.spi.ExceptionHandler</code> type.
         * @group consumer (advanced)
         */
        default AdvancedGitHubEndpointConsumerBuilder exceptionHandler(
                String exceptionHandler) {
            setProperty("exceptionHandler", exceptionHandler);
            return this;
        }
        /**
         * Sets the exchange pattern when the consumer creates an exchange.
         * The option is a <code>org.apache.camel.ExchangePattern</code> type.
         * @group consumer (advanced)
         */
        default AdvancedGitHubEndpointConsumerBuilder exchangePattern(
                ExchangePattern exchangePattern) {
            setProperty("exchangePattern", exchangePattern);
            return this;
        }
        /**
         * Sets the exchange pattern when the consumer creates an exchange.
         * The option will be converted to a
         * <code>org.apache.camel.ExchangePattern</code> type.
         * @group consumer (advanced)
         */
        default AdvancedGitHubEndpointConsumerBuilder exchangePattern(
                String exchangePattern) {
            setProperty("exchangePattern", exchangePattern);
            return this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointConsumerBuilder basicPropertyBinding(
                boolean basicPropertyBinding) {
            setProperty("basicPropertyBinding", basicPropertyBinding);
            return this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointConsumerBuilder basicPropertyBinding(
                String basicPropertyBinding) {
            setProperty("basicPropertyBinding", basicPropertyBinding);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointConsumerBuilder synchronous(
                boolean synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointConsumerBuilder synchronous(
                String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Builder for endpoint producers for the GitHub component.
     */
    public interface GitHubEndpointProducerBuilder
            extends
                EndpointProducerBuilder {
        default AdvancedGitHubEndpointProducerBuilder advanced() {
            return (AdvancedGitHubEndpointProducerBuilder) this;
        }
        /**
         * What git operation to execute.
         * The option is a
         * <code>org.apache.camel.component.github.GitHubType</code> type.
         * @group common
         */
        default GitHubEndpointProducerBuilder type(GitHubType type) {
            setProperty("type", type);
            return this;
        }
        /**
         * What git operation to execute.
         * The option will be converted to a
         * <code>org.apache.camel.component.github.GitHubType</code> type.
         * @group common
         */
        default GitHubEndpointProducerBuilder type(String type) {
            setProperty("type", type);
            return this;
        }
        /**
         * GitHub OAuth token, required unless username & password are provided.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointProducerBuilder oauthToken(String oauthToken) {
            setProperty("oauthToken", oauthToken);
            return this;
        }
        /**
         * GitHub password, required unless oauthToken is provided.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointProducerBuilder password(String password) {
            setProperty("password", password);
            return this;
        }
        /**
         * GitHub repository name.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointProducerBuilder repoName(String repoName) {
            setProperty("repoName", repoName);
            return this;
        }
        /**
         * GitHub repository owner (organization).
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointProducerBuilder repoOwner(String repoOwner) {
            setProperty("repoOwner", repoOwner);
            return this;
        }
        /**
         * GitHub username, required unless oauthToken is provided.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointProducerBuilder username(String username) {
            setProperty("username", username);
            return this;
        }
        /**
         * To use the given encoding when getting a git commit file.
         * The option is a <code>java.lang.String</code> type.
         * @group producer
         */
        default GitHubEndpointProducerBuilder encoding(String encoding) {
            setProperty("encoding", encoding);
            return this;
        }
        /**
         * Whether the producer should be started lazy (on the first message).
         * By starting lazy you can use this to allow CamelContext and routes to
         * startup in situations where a producer may otherwise fail during
         * starting and cause the route to fail being started. By deferring this
         * startup to be lazy then the startup failure can be handled during
         * routing messages via Camel's routing error handlers. Beware that when
         * the first message is processed then creating and starting the
         * producer may take a little time and prolong the total processing time
         * of the processing.
         * The option is a <code>boolean</code> type.
         * @group producer
         */
        default GitHubEndpointProducerBuilder lazyStartProducer(
                boolean lazyStartProducer) {
            setProperty("lazyStartProducer", lazyStartProducer);
            return this;
        }
        /**
         * Whether the producer should be started lazy (on the first message).
         * By starting lazy you can use this to allow CamelContext and routes to
         * startup in situations where a producer may otherwise fail during
         * starting and cause the route to fail being started. By deferring this
         * startup to be lazy then the startup failure can be handled during
         * routing messages via Camel's routing error handlers. Beware that when
         * the first message is processed then creating and starting the
         * producer may take a little time and prolong the total processing time
         * of the processing.
         * The option will be converted to a <code>boolean</code> type.
         * @group producer
         */
        default GitHubEndpointProducerBuilder lazyStartProducer(
                String lazyStartProducer) {
            setProperty("lazyStartProducer", lazyStartProducer);
            return this;
        }
        /**
         * To set git commit status state.
         * The option is a <code>java.lang.String</code> type.
         * @group producer
         */
        default GitHubEndpointProducerBuilder state(String state) {
            setProperty("state", state);
            return this;
        }
        /**
         * To set git commit status target url.
         * The option is a <code>java.lang.String</code> type.
         * @group producer
         */
        default GitHubEndpointProducerBuilder targetUrl(String targetUrl) {
            setProperty("targetUrl", targetUrl);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint producers for the GitHub component.
     */
    public interface AdvancedGitHubEndpointProducerBuilder
            extends
                EndpointProducerBuilder {
        default GitHubEndpointProducerBuilder basic() {
            return (GitHubEndpointProducerBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointProducerBuilder basicPropertyBinding(
                boolean basicPropertyBinding) {
            setProperty("basicPropertyBinding", basicPropertyBinding);
            return this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointProducerBuilder basicPropertyBinding(
                String basicPropertyBinding) {
            setProperty("basicPropertyBinding", basicPropertyBinding);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointProducerBuilder synchronous(
                boolean synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointProducerBuilder synchronous(
                String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Builder for endpoint for the GitHub component.
     */
    public interface GitHubEndpointBuilder
            extends
                GitHubEndpointConsumerBuilder, GitHubEndpointProducerBuilder {
        default AdvancedGitHubEndpointBuilder advanced() {
            return (AdvancedGitHubEndpointBuilder) this;
        }
        /**
         * What git operation to execute.
         * The option is a
         * <code>org.apache.camel.component.github.GitHubType</code> type.
         * @group common
         */
        default GitHubEndpointBuilder type(GitHubType type) {
            setProperty("type", type);
            return this;
        }
        /**
         * What git operation to execute.
         * The option will be converted to a
         * <code>org.apache.camel.component.github.GitHubType</code> type.
         * @group common
         */
        default GitHubEndpointBuilder type(String type) {
            setProperty("type", type);
            return this;
        }
        /**
         * GitHub OAuth token, required unless username & password are provided.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointBuilder oauthToken(String oauthToken) {
            setProperty("oauthToken", oauthToken);
            return this;
        }
        /**
         * GitHub password, required unless oauthToken is provided.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointBuilder password(String password) {
            setProperty("password", password);
            return this;
        }
        /**
         * GitHub repository name.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointBuilder repoName(String repoName) {
            setProperty("repoName", repoName);
            return this;
        }
        /**
         * GitHub repository owner (organization).
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointBuilder repoOwner(String repoOwner) {
            setProperty("repoOwner", repoOwner);
            return this;
        }
        /**
         * GitHub username, required unless oauthToken is provided.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GitHubEndpointBuilder username(String username) {
            setProperty("username", username);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint for the GitHub component.
     */
    public interface AdvancedGitHubEndpointBuilder
            extends
                AdvancedGitHubEndpointConsumerBuilder, AdvancedGitHubEndpointProducerBuilder {
        default GitHubEndpointBuilder basic() {
            return (GitHubEndpointBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointBuilder basicPropertyBinding(
                boolean basicPropertyBinding) {
            setProperty("basicPropertyBinding", basicPropertyBinding);
            return this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointBuilder basicPropertyBinding(
                String basicPropertyBinding) {
            setProperty("basicPropertyBinding", basicPropertyBinding);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointBuilder synchronous(boolean synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGitHubEndpointBuilder synchronous(String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Proxy enum for <code>org.apache.camel.component.github.GitHubType</code>
     * enum.
     */
    enum GitHubType {
        CLOSEPULLREQUEST, PULLREQUESTCOMMENT, COMMIT, PULLREQUEST, TAG, PULLREQUESTSTATE, PULLREQUESTFILES, GETCOMMITFILE, CREATEISSUE;
    }
    /**
     * The github component is used for integrating Camel with github. Creates a
     * builder to build endpoints for the GitHub component.
     */
    default GitHubEndpointBuilder gitHub(String path) {
        class GitHubEndpointBuilderImpl extends AbstractEndpointBuilder implements GitHubEndpointBuilder, AdvancedGitHubEndpointBuilder {
            public GitHubEndpointBuilderImpl(String path) {
                super("github", path);
            }
        }
        return new GitHubEndpointBuilderImpl(path);
    }
}