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
 * Component for working with MongoDB GridFS.
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointDslMojo")
public interface GridFsEndpointBuilderFactory {


    /**
     * Builder for endpoint consumers for the MongoDB GridFS component.
     */
    public interface GridFsEndpointConsumerBuilder
            extends
                EndpointConsumerBuilder {
        default AdvancedGridFsEndpointConsumerBuilder advanced() {
            return (AdvancedGridFsEndpointConsumerBuilder) this;
        }
        /**
         * Name of com.mongodb.Mongo to use.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GridFsEndpointConsumerBuilder connectionBean(
                String connectionBean) {
            setProperty("connectionBean", connectionBean);
            return this;
        }
        /**
         * Sets the name of the GridFS bucket within the database. Default is
         * fs.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GridFsEndpointConsumerBuilder bucket(String bucket) {
            setProperty("bucket", bucket);
            return this;
        }
        /**
         * Sets the name of the MongoDB database to target.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GridFsEndpointConsumerBuilder database(String database) {
            setProperty("database", database);
            return this;
        }
        /**
         * Sets a MongoDB ReadPreference on the Mongo connection. Read
         * preferences set directly on the connection will be overridden by this
         * setting. The com.mongodb.ReadPreference#valueOf(String) utility
         * method is used to resolve the passed readPreference value. Some
         * examples for the possible values are nearest, primary or secondary
         * etc.
         * The option is a <code>com.mongodb.ReadPreference</code> type.
         * @group common
         */
        default GridFsEndpointConsumerBuilder readPreference(
                Object readPreference) {
            setProperty("readPreference", readPreference);
            return this;
        }
        /**
         * Sets a MongoDB ReadPreference on the Mongo connection. Read
         * preferences set directly on the connection will be overridden by this
         * setting. The com.mongodb.ReadPreference#valueOf(String) utility
         * method is used to resolve the passed readPreference value. Some
         * examples for the possible values are nearest, primary or secondary
         * etc.
         * The option will be converted to a
         * <code>com.mongodb.ReadPreference</code> type.
         * @group common
         */
        default GridFsEndpointConsumerBuilder readPreference(
                String readPreference) {
            setProperty("readPreference", readPreference);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB using the
         * standard ones. Resolved from the fields of the WriteConcern class by
         * calling the WriteConcern#valueOf(String) method.
         * The option is a <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointConsumerBuilder writeConcern(Object writeConcern) {
            setProperty("writeConcern", writeConcern);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB using the
         * standard ones. Resolved from the fields of the WriteConcern class by
         * calling the WriteConcern#valueOf(String) method.
         * The option will be converted to a
         * <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointConsumerBuilder writeConcern(String writeConcern) {
            setProperty("writeConcern", writeConcern);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB, passing in the
         * bean ref to a custom WriteConcern which exists in the Registry. You
         * can also use standard WriteConcerns by passing in their key. See the
         * {link #setWriteConcern(String) setWriteConcern} method.
         * The option is a <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointConsumerBuilder writeConcernRef(
                Object writeConcernRef) {
            setProperty("writeConcernRef", writeConcernRef);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB, passing in the
         * bean ref to a custom WriteConcern which exists in the Registry. You
         * can also use standard WriteConcerns by passing in their key. See the
         * {link #setWriteConcern(String) setWriteConcern} method.
         * The option will be converted to a
         * <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointConsumerBuilder writeConcernRef(
                String writeConcernRef) {
            setProperty("writeConcernRef", writeConcernRef);
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
        default GridFsEndpointConsumerBuilder bridgeErrorHandler(
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
        default GridFsEndpointConsumerBuilder bridgeErrorHandler(
                String bridgeErrorHandler) {
            setProperty("bridgeErrorHandler", bridgeErrorHandler);
            return this;
        }
        /**
         * Sets the delay between polls within the Consumer. Default is 500ms.
         * The option is a <code>long</code> type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder delay(long delay) {
            setProperty("delay", delay);
            return this;
        }
        /**
         * Sets the delay between polls within the Consumer. Default is 500ms.
         * The option will be converted to a <code>long</code> type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder delay(String delay) {
            setProperty("delay", delay);
            return this;
        }
        /**
         * If the QueryType uses a FileAttribute, this sets the name of the
         * attribute that is used. Default is camel-processed.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder fileAttributeName(
                String fileAttributeName) {
            setProperty("fileAttributeName", fileAttributeName);
            return this;
        }
        /**
         * Sets the initialDelay before the consumer will start polling. Default
         * is 1000ms.
         * The option is a <code>long</code> type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder initialDelay(long initialDelay) {
            setProperty("initialDelay", initialDelay);
            return this;
        }
        /**
         * Sets the initialDelay before the consumer will start polling. Default
         * is 1000ms.
         * The option will be converted to a <code>long</code> type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder initialDelay(String initialDelay) {
            setProperty("initialDelay", initialDelay);
            return this;
        }
        /**
         * If the QueryType uses a persistent timestamp, this sets the name of
         * the collection within the DB to store the timestamp.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder persistentTSCollection(
                String persistentTSCollection) {
            setProperty("persistentTSCollection", persistentTSCollection);
            return this;
        }
        /**
         * If the QueryType uses a persistent timestamp, this is the ID of the
         * object in the collection to store the timestamp.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder persistentTSObject(
                String persistentTSObject) {
            setProperty("persistentTSObject", persistentTSObject);
            return this;
        }
        /**
         * Additional query parameters (in JSON) that are used to configure the
         * query used for finding files in the GridFsConsumer.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder query(String query) {
            setProperty("query", query);
            return this;
        }
        /**
         * Sets the QueryStrategy that is used for polling for new files.
         * Default is Timestamp.
         * The option is a
         * <code>org.apache.camel.component.mongodb.gridfs.QueryStrategy</code>
         * type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder queryStrategy(
                QueryStrategy queryStrategy) {
            setProperty("queryStrategy", queryStrategy);
            return this;
        }
        /**
         * Sets the QueryStrategy that is used for polling for new files.
         * Default is Timestamp.
         * The option will be converted to a
         * <code>org.apache.camel.component.mongodb.gridfs.QueryStrategy</code>
         * type.
         * @group consumer
         */
        default GridFsEndpointConsumerBuilder queryStrategy(String queryStrategy) {
            setProperty("queryStrategy", queryStrategy);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint consumers for the MongoDB GridFS component.
     */
    public interface AdvancedGridFsEndpointConsumerBuilder
            extends
                EndpointConsumerBuilder {
        default GridFsEndpointConsumerBuilder basic() {
            return (GridFsEndpointConsumerBuilder) this;
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
        default AdvancedGridFsEndpointConsumerBuilder exceptionHandler(
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
        default AdvancedGridFsEndpointConsumerBuilder exceptionHandler(
                String exceptionHandler) {
            setProperty("exceptionHandler", exceptionHandler);
            return this;
        }
        /**
         * Sets the exchange pattern when the consumer creates an exchange.
         * The option is a <code>org.apache.camel.ExchangePattern</code> type.
         * @group consumer (advanced)
         */
        default AdvancedGridFsEndpointConsumerBuilder exchangePattern(
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
        default AdvancedGridFsEndpointConsumerBuilder exchangePattern(
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
        default AdvancedGridFsEndpointConsumerBuilder basicPropertyBinding(
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
        default AdvancedGridFsEndpointConsumerBuilder basicPropertyBinding(
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
        default AdvancedGridFsEndpointConsumerBuilder synchronous(
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
        default AdvancedGridFsEndpointConsumerBuilder synchronous(
                String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Builder for endpoint producers for the MongoDB GridFS component.
     */
    public interface GridFsEndpointProducerBuilder
            extends
                EndpointProducerBuilder {
        default AdvancedGridFsEndpointProducerBuilder advanced() {
            return (AdvancedGridFsEndpointProducerBuilder) this;
        }
        /**
         * Name of com.mongodb.Mongo to use.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GridFsEndpointProducerBuilder connectionBean(
                String connectionBean) {
            setProperty("connectionBean", connectionBean);
            return this;
        }
        /**
         * Sets the name of the GridFS bucket within the database. Default is
         * fs.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GridFsEndpointProducerBuilder bucket(String bucket) {
            setProperty("bucket", bucket);
            return this;
        }
        /**
         * Sets the name of the MongoDB database to target.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GridFsEndpointProducerBuilder database(String database) {
            setProperty("database", database);
            return this;
        }
        /**
         * Sets a MongoDB ReadPreference on the Mongo connection. Read
         * preferences set directly on the connection will be overridden by this
         * setting. The com.mongodb.ReadPreference#valueOf(String) utility
         * method is used to resolve the passed readPreference value. Some
         * examples for the possible values are nearest, primary or secondary
         * etc.
         * The option is a <code>com.mongodb.ReadPreference</code> type.
         * @group common
         */
        default GridFsEndpointProducerBuilder readPreference(
                Object readPreference) {
            setProperty("readPreference", readPreference);
            return this;
        }
        /**
         * Sets a MongoDB ReadPreference on the Mongo connection. Read
         * preferences set directly on the connection will be overridden by this
         * setting. The com.mongodb.ReadPreference#valueOf(String) utility
         * method is used to resolve the passed readPreference value. Some
         * examples for the possible values are nearest, primary or secondary
         * etc.
         * The option will be converted to a
         * <code>com.mongodb.ReadPreference</code> type.
         * @group common
         */
        default GridFsEndpointProducerBuilder readPreference(
                String readPreference) {
            setProperty("readPreference", readPreference);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB using the
         * standard ones. Resolved from the fields of the WriteConcern class by
         * calling the WriteConcern#valueOf(String) method.
         * The option is a <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointProducerBuilder writeConcern(Object writeConcern) {
            setProperty("writeConcern", writeConcern);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB using the
         * standard ones. Resolved from the fields of the WriteConcern class by
         * calling the WriteConcern#valueOf(String) method.
         * The option will be converted to a
         * <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointProducerBuilder writeConcern(String writeConcern) {
            setProperty("writeConcern", writeConcern);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB, passing in the
         * bean ref to a custom WriteConcern which exists in the Registry. You
         * can also use standard WriteConcerns by passing in their key. See the
         * {link #setWriteConcern(String) setWriteConcern} method.
         * The option is a <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointProducerBuilder writeConcernRef(
                Object writeConcernRef) {
            setProperty("writeConcernRef", writeConcernRef);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB, passing in the
         * bean ref to a custom WriteConcern which exists in the Registry. You
         * can also use standard WriteConcerns by passing in their key. See the
         * {link #setWriteConcern(String) setWriteConcern} method.
         * The option will be converted to a
         * <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointProducerBuilder writeConcernRef(
                String writeConcernRef) {
            setProperty("writeConcernRef", writeConcernRef);
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
        default GridFsEndpointProducerBuilder lazyStartProducer(
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
        default GridFsEndpointProducerBuilder lazyStartProducer(
                String lazyStartProducer) {
            setProperty("lazyStartProducer", lazyStartProducer);
            return this;
        }
        /**
         * Sets the operation this endpoint will execute against GridRS.
         * The option is a <code>java.lang.String</code> type.
         * @group producer
         */
        default GridFsEndpointProducerBuilder operation(String operation) {
            setProperty("operation", operation);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint producers for the MongoDB GridFS component.
     */
    public interface AdvancedGridFsEndpointProducerBuilder
            extends
                EndpointProducerBuilder {
        default GridFsEndpointProducerBuilder basic() {
            return (GridFsEndpointProducerBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGridFsEndpointProducerBuilder basicPropertyBinding(
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
        default AdvancedGridFsEndpointProducerBuilder basicPropertyBinding(
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
        default AdvancedGridFsEndpointProducerBuilder synchronous(
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
        default AdvancedGridFsEndpointProducerBuilder synchronous(
                String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Builder for endpoint for the MongoDB GridFS component.
     */
    public interface GridFsEndpointBuilder
            extends
                GridFsEndpointConsumerBuilder, GridFsEndpointProducerBuilder {
        default AdvancedGridFsEndpointBuilder advanced() {
            return (AdvancedGridFsEndpointBuilder) this;
        }
        /**
         * Name of com.mongodb.Mongo to use.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GridFsEndpointBuilder connectionBean(String connectionBean) {
            setProperty("connectionBean", connectionBean);
            return this;
        }
        /**
         * Sets the name of the GridFS bucket within the database. Default is
         * fs.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GridFsEndpointBuilder bucket(String bucket) {
            setProperty("bucket", bucket);
            return this;
        }
        /**
         * Sets the name of the MongoDB database to target.
         * The option is a <code>java.lang.String</code> type.
         * @group common
         */
        default GridFsEndpointBuilder database(String database) {
            setProperty("database", database);
            return this;
        }
        /**
         * Sets a MongoDB ReadPreference on the Mongo connection. Read
         * preferences set directly on the connection will be overridden by this
         * setting. The com.mongodb.ReadPreference#valueOf(String) utility
         * method is used to resolve the passed readPreference value. Some
         * examples for the possible values are nearest, primary or secondary
         * etc.
         * The option is a <code>com.mongodb.ReadPreference</code> type.
         * @group common
         */
        default GridFsEndpointBuilder readPreference(Object readPreference) {
            setProperty("readPreference", readPreference);
            return this;
        }
        /**
         * Sets a MongoDB ReadPreference on the Mongo connection. Read
         * preferences set directly on the connection will be overridden by this
         * setting. The com.mongodb.ReadPreference#valueOf(String) utility
         * method is used to resolve the passed readPreference value. Some
         * examples for the possible values are nearest, primary or secondary
         * etc.
         * The option will be converted to a
         * <code>com.mongodb.ReadPreference</code> type.
         * @group common
         */
        default GridFsEndpointBuilder readPreference(String readPreference) {
            setProperty("readPreference", readPreference);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB using the
         * standard ones. Resolved from the fields of the WriteConcern class by
         * calling the WriteConcern#valueOf(String) method.
         * The option is a <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointBuilder writeConcern(Object writeConcern) {
            setProperty("writeConcern", writeConcern);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB using the
         * standard ones. Resolved from the fields of the WriteConcern class by
         * calling the WriteConcern#valueOf(String) method.
         * The option will be converted to a
         * <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointBuilder writeConcern(String writeConcern) {
            setProperty("writeConcern", writeConcern);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB, passing in the
         * bean ref to a custom WriteConcern which exists in the Registry. You
         * can also use standard WriteConcerns by passing in their key. See the
         * {link #setWriteConcern(String) setWriteConcern} method.
         * The option is a <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointBuilder writeConcernRef(Object writeConcernRef) {
            setProperty("writeConcernRef", writeConcernRef);
            return this;
        }
        /**
         * Set the WriteConcern for write operations on MongoDB, passing in the
         * bean ref to a custom WriteConcern which exists in the Registry. You
         * can also use standard WriteConcerns by passing in their key. See the
         * {link #setWriteConcern(String) setWriteConcern} method.
         * The option will be converted to a
         * <code>com.mongodb.WriteConcern</code> type.
         * @group common
         */
        default GridFsEndpointBuilder writeConcernRef(String writeConcernRef) {
            setProperty("writeConcernRef", writeConcernRef);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint for the MongoDB GridFS component.
     */
    public interface AdvancedGridFsEndpointBuilder
            extends
                AdvancedGridFsEndpointConsumerBuilder, AdvancedGridFsEndpointProducerBuilder {
        default GridFsEndpointBuilder basic() {
            return (GridFsEndpointBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGridFsEndpointBuilder basicPropertyBinding(
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
        default AdvancedGridFsEndpointBuilder basicPropertyBinding(
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
        default AdvancedGridFsEndpointBuilder synchronous(boolean synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGridFsEndpointBuilder synchronous(String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Proxy enum for
     * <code>org.apache.camel.component.mongodb.gridfs.QueryStrategy</code>
     * enum.
     */
    enum QueryStrategy {
        TimeStamp, PersistentTimestamp, FileAttribute, TimeStampAndFileAttribute, PersistentTimestampAndFileAttribute;
    }
    /**
     * Component for working with MongoDB GridFS. Creates a builder to build
     * endpoints for the MongoDB GridFS component.
     */
    default GridFsEndpointBuilder gridFs(String path) {
        class GridFsEndpointBuilderImpl extends AbstractEndpointBuilder implements GridFsEndpointBuilder, AdvancedGridFsEndpointBuilder {
            public GridFsEndpointBuilderImpl(String path) {
                super("mongodb-gridfs", path);
            }
        }
        return new GridFsEndpointBuilderImpl(path);
    }
}