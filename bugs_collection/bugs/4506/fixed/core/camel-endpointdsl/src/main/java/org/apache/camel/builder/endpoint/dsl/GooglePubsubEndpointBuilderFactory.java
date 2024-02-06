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
 * Messaging client for Google Cloud Platform PubSub Service
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointDslMojo")
public interface GooglePubsubEndpointBuilderFactory {


    /**
     * Builder for endpoint consumers for the Google Pubsub component.
     */
    public interface GooglePubsubEndpointConsumerBuilder
            extends
                EndpointConsumerBuilder {
        default AdvancedGooglePubsubEndpointConsumerBuilder advanced() {
            return (AdvancedGooglePubsubEndpointConsumerBuilder) this;
        }
        /**
         * Project Id.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder projectId(String projectId) {
            setProperty("projectId", projectId);
            return this;
        }
        /**
         * Destination Name.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder destinationName(
                String destinationName) {
            setProperty("destinationName", destinationName);
            return this;
        }
        /**
         * AUTO = exchange gets ack'ed/nack'ed on completion. NONE = downstream
         * process has to ack/nack explicitly.
         * The option is a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConstants$AckMode</code> type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder ackMode(AckMode ackMode) {
            setProperty("ackMode", ackMode);
            return this;
        }
        /**
         * AUTO = exchange gets ack'ed/nack'ed on completion. NONE = downstream
         * process has to ack/nack explicitly.
         * The option will be converted to a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConstants$AckMode</code> type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder ackMode(String ackMode) {
            setProperty("ackMode", ackMode);
            return this;
        }
        /**
         * The number of parallel streams consuming from the subscription.
         * The option is a <code>java.lang.Integer</code> type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder concurrentConsumers(
                Integer concurrentConsumers) {
            setProperty("concurrentConsumers", concurrentConsumers);
            return this;
        }
        /**
         * The number of parallel streams consuming from the subscription.
         * The option will be converted to a <code>java.lang.Integer</code>
         * type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder concurrentConsumers(
                String concurrentConsumers) {
            setProperty("concurrentConsumers", concurrentConsumers);
            return this;
        }
        /**
         * ConnectionFactory to obtain connection to PubSub Service. If non
         * provided the default will be used.
         * The option is a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConnectionFactory</code> type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder connectionFactory(
                Object connectionFactory) {
            setProperty("connectionFactory", connectionFactory);
            return this;
        }
        /**
         * ConnectionFactory to obtain connection to PubSub Service. If non
         * provided the default will be used.
         * The option will be converted to a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConnectionFactory</code> type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder connectionFactory(
                String connectionFactory) {
            setProperty("connectionFactory", connectionFactory);
            return this;
        }
        /**
         * Logger ID to use when a match to the parent route required.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder loggerId(String loggerId) {
            setProperty("loggerId", loggerId);
            return this;
        }
        /**
         * The max number of messages to receive from the server in a single API
         * call.
         * The option is a <code>java.lang.Integer</code> type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder maxMessagesPerPoll(
                Integer maxMessagesPerPoll) {
            setProperty("maxMessagesPerPoll", maxMessagesPerPoll);
            return this;
        }
        /**
         * The max number of messages to receive from the server in a single API
         * call.
         * The option will be converted to a <code>java.lang.Integer</code>
         * type.
         * @group common
         */
        default GooglePubsubEndpointConsumerBuilder maxMessagesPerPoll(
                String maxMessagesPerPoll) {
            setProperty("maxMessagesPerPoll", maxMessagesPerPoll);
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
        default GooglePubsubEndpointConsumerBuilder bridgeErrorHandler(
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
        default GooglePubsubEndpointConsumerBuilder bridgeErrorHandler(
                String bridgeErrorHandler) {
            setProperty("bridgeErrorHandler", bridgeErrorHandler);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint consumers for the Google Pubsub component.
     */
    public interface AdvancedGooglePubsubEndpointConsumerBuilder
            extends
                EndpointConsumerBuilder {
        default GooglePubsubEndpointConsumerBuilder basic() {
            return (GooglePubsubEndpointConsumerBuilder) this;
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
        default AdvancedGooglePubsubEndpointConsumerBuilder exceptionHandler(
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
        default AdvancedGooglePubsubEndpointConsumerBuilder exceptionHandler(
                String exceptionHandler) {
            setProperty("exceptionHandler", exceptionHandler);
            return this;
        }
        /**
         * Sets the exchange pattern when the consumer creates an exchange.
         * The option is a <code>org.apache.camel.ExchangePattern</code> type.
         * @group consumer (advanced)
         */
        default AdvancedGooglePubsubEndpointConsumerBuilder exchangePattern(
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
        default AdvancedGooglePubsubEndpointConsumerBuilder exchangePattern(
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
        default AdvancedGooglePubsubEndpointConsumerBuilder basicPropertyBinding(
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
        default AdvancedGooglePubsubEndpointConsumerBuilder basicPropertyBinding(
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
        default AdvancedGooglePubsubEndpointConsumerBuilder synchronous(
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
        default AdvancedGooglePubsubEndpointConsumerBuilder synchronous(
                String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Builder for endpoint producers for the Google Pubsub component.
     */
    public interface GooglePubsubEndpointProducerBuilder
            extends
                EndpointProducerBuilder {
        default AdvancedGooglePubsubEndpointProducerBuilder advanced() {
            return (AdvancedGooglePubsubEndpointProducerBuilder) this;
        }
        /**
         * Project Id.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder projectId(String projectId) {
            setProperty("projectId", projectId);
            return this;
        }
        /**
         * Destination Name.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder destinationName(
                String destinationName) {
            setProperty("destinationName", destinationName);
            return this;
        }
        /**
         * AUTO = exchange gets ack'ed/nack'ed on completion. NONE = downstream
         * process has to ack/nack explicitly.
         * The option is a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConstants$AckMode</code> type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder ackMode(AckMode ackMode) {
            setProperty("ackMode", ackMode);
            return this;
        }
        /**
         * AUTO = exchange gets ack'ed/nack'ed on completion. NONE = downstream
         * process has to ack/nack explicitly.
         * The option will be converted to a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConstants$AckMode</code> type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder ackMode(String ackMode) {
            setProperty("ackMode", ackMode);
            return this;
        }
        /**
         * The number of parallel streams consuming from the subscription.
         * The option is a <code>java.lang.Integer</code> type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder concurrentConsumers(
                Integer concurrentConsumers) {
            setProperty("concurrentConsumers", concurrentConsumers);
            return this;
        }
        /**
         * The number of parallel streams consuming from the subscription.
         * The option will be converted to a <code>java.lang.Integer</code>
         * type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder concurrentConsumers(
                String concurrentConsumers) {
            setProperty("concurrentConsumers", concurrentConsumers);
            return this;
        }
        /**
         * ConnectionFactory to obtain connection to PubSub Service. If non
         * provided the default will be used.
         * The option is a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConnectionFactory</code> type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder connectionFactory(
                Object connectionFactory) {
            setProperty("connectionFactory", connectionFactory);
            return this;
        }
        /**
         * ConnectionFactory to obtain connection to PubSub Service. If non
         * provided the default will be used.
         * The option will be converted to a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConnectionFactory</code> type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder connectionFactory(
                String connectionFactory) {
            setProperty("connectionFactory", connectionFactory);
            return this;
        }
        /**
         * Logger ID to use when a match to the parent route required.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder loggerId(String loggerId) {
            setProperty("loggerId", loggerId);
            return this;
        }
        /**
         * The max number of messages to receive from the server in a single API
         * call.
         * The option is a <code>java.lang.Integer</code> type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder maxMessagesPerPoll(
                Integer maxMessagesPerPoll) {
            setProperty("maxMessagesPerPoll", maxMessagesPerPoll);
            return this;
        }
        /**
         * The max number of messages to receive from the server in a single API
         * call.
         * The option will be converted to a <code>java.lang.Integer</code>
         * type.
         * @group common
         */
        default GooglePubsubEndpointProducerBuilder maxMessagesPerPoll(
                String maxMessagesPerPoll) {
            setProperty("maxMessagesPerPoll", maxMessagesPerPoll);
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
        default GooglePubsubEndpointProducerBuilder lazyStartProducer(
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
        default GooglePubsubEndpointProducerBuilder lazyStartProducer(
                String lazyStartProducer) {
            setProperty("lazyStartProducer", lazyStartProducer);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint producers for the Google Pubsub component.
     */
    public interface AdvancedGooglePubsubEndpointProducerBuilder
            extends
                EndpointProducerBuilder {
        default GooglePubsubEndpointProducerBuilder basic() {
            return (GooglePubsubEndpointProducerBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGooglePubsubEndpointProducerBuilder basicPropertyBinding(
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
        default AdvancedGooglePubsubEndpointProducerBuilder basicPropertyBinding(
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
        default AdvancedGooglePubsubEndpointProducerBuilder synchronous(
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
        default AdvancedGooglePubsubEndpointProducerBuilder synchronous(
                String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Builder for endpoint for the Google Pubsub component.
     */
    public interface GooglePubsubEndpointBuilder
            extends
                GooglePubsubEndpointConsumerBuilder, GooglePubsubEndpointProducerBuilder {
        default AdvancedGooglePubsubEndpointBuilder advanced() {
            return (AdvancedGooglePubsubEndpointBuilder) this;
        }
        /**
         * Project Id.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GooglePubsubEndpointBuilder projectId(String projectId) {
            setProperty("projectId", projectId);
            return this;
        }
        /**
         * Destination Name.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GooglePubsubEndpointBuilder destinationName(
                String destinationName) {
            setProperty("destinationName", destinationName);
            return this;
        }
        /**
         * AUTO = exchange gets ack'ed/nack'ed on completion. NONE = downstream
         * process has to ack/nack explicitly.
         * The option is a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConstants$AckMode</code> type.
         * @group common
         */
        default GooglePubsubEndpointBuilder ackMode(AckMode ackMode) {
            setProperty("ackMode", ackMode);
            return this;
        }
        /**
         * AUTO = exchange gets ack'ed/nack'ed on completion. NONE = downstream
         * process has to ack/nack explicitly.
         * The option will be converted to a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConstants$AckMode</code> type.
         * @group common
         */
        default GooglePubsubEndpointBuilder ackMode(String ackMode) {
            setProperty("ackMode", ackMode);
            return this;
        }
        /**
         * The number of parallel streams consuming from the subscription.
         * The option is a <code>java.lang.Integer</code> type.
         * @group common
         */
        default GooglePubsubEndpointBuilder concurrentConsumers(
                Integer concurrentConsumers) {
            setProperty("concurrentConsumers", concurrentConsumers);
            return this;
        }
        /**
         * The number of parallel streams consuming from the subscription.
         * The option will be converted to a <code>java.lang.Integer</code>
         * type.
         * @group common
         */
        default GooglePubsubEndpointBuilder concurrentConsumers(
                String concurrentConsumers) {
            setProperty("concurrentConsumers", concurrentConsumers);
            return this;
        }
        /**
         * ConnectionFactory to obtain connection to PubSub Service. If non
         * provided the default will be used.
         * The option is a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConnectionFactory</code> type.
         * @group common
         */
        default GooglePubsubEndpointBuilder connectionFactory(
                Object connectionFactory) {
            setProperty("connectionFactory", connectionFactory);
            return this;
        }
        /**
         * ConnectionFactory to obtain connection to PubSub Service. If non
         * provided the default will be used.
         * The option will be converted to a
         * <code>org.apache.camel.component.google.pubsub.GooglePubsubConnectionFactory</code> type.
         * @group common
         */
        default GooglePubsubEndpointBuilder connectionFactory(
                String connectionFactory) {
            setProperty("connectionFactory", connectionFactory);
            return this;
        }
        /**
         * Logger ID to use when a match to the parent route required.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GooglePubsubEndpointBuilder loggerId(String loggerId) {
            setProperty("loggerId", loggerId);
            return this;
        }
        /**
         * The max number of messages to receive from the server in a single API
         * call.
         * The option is a <code>java.lang.Integer</code> type.
         * @group common
         */
        default GooglePubsubEndpointBuilder maxMessagesPerPoll(
                Integer maxMessagesPerPoll) {
            setProperty("maxMessagesPerPoll", maxMessagesPerPoll);
            return this;
        }
        /**
         * The max number of messages to receive from the server in a single API
         * call.
         * The option will be converted to a <code>java.lang.Integer</code>
         * type.
         * @group common
         */
        default GooglePubsubEndpointBuilder maxMessagesPerPoll(
                String maxMessagesPerPoll) {
            setProperty("maxMessagesPerPoll", maxMessagesPerPoll);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint for the Google Pubsub component.
     */
    public interface AdvancedGooglePubsubEndpointBuilder
            extends
                AdvancedGooglePubsubEndpointConsumerBuilder, AdvancedGooglePubsubEndpointProducerBuilder {
        default GooglePubsubEndpointBuilder basic() {
            return (GooglePubsubEndpointBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGooglePubsubEndpointBuilder basicPropertyBinding(
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
        default AdvancedGooglePubsubEndpointBuilder basicPropertyBinding(
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
        default AdvancedGooglePubsubEndpointBuilder synchronous(
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
        default AdvancedGooglePubsubEndpointBuilder synchronous(
                String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Proxy enum for
     * <code>org.apache.camel.component.google.pubsub.GooglePubsubConstants$AckMode</code> enum.
     */
    enum AckMode {
        AUTO, NONE;
    }
    /**
     * Messaging client for Google Cloud Platform PubSub Service Creates a
     * builder to build endpoints for the Google Pubsub component.
     */
    default GooglePubsubEndpointBuilder googlePubsub(String path) {
        class GooglePubsubEndpointBuilderImpl extends AbstractEndpointBuilder implements GooglePubsubEndpointBuilder, AdvancedGooglePubsubEndpointBuilder {
            public GooglePubsubEndpointBuilderImpl(String path) {
                super("google-pubsub", path);
            }
        }
        return new GooglePubsubEndpointBuilderImpl(path);
    }
}