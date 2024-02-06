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
import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.EndpointProducerBuilder;
import org.apache.camel.builder.endpoint.AbstractEndpointBuilder;

/**
 * The Ignite Sets endpoint is one of camel-ignite endpoints which allows you to
 * interact with Ignite Set data structures.
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointDslMojo")
public interface IgniteSetEndpointBuilderFactory {


    /**
     * Builder for endpoint for the Ignite Sets component.
     */
    public interface IgniteSetEndpointBuilder extends EndpointProducerBuilder {
        default AdvancedIgniteSetEndpointBuilder advanced() {
            return (AdvancedIgniteSetEndpointBuilder) this;
        }
        /**
         * The set name.
         * The option is a <code>java.lang.String</code> type.
         * @group producer
         */
        default IgniteSetEndpointBuilder name(String name) {
            setProperty("name", name);
            return this;
        }
        /**
         * Sets whether to propagate the incoming body if the return type of the
         * underlying Ignite operation is void.
         * The option is a <code>boolean</code> type.
         * @group producer
         */
        default IgniteSetEndpointBuilder propagateIncomingBodyIfNoReturnValue(
                boolean propagateIncomingBodyIfNoReturnValue) {
            setProperty("propagateIncomingBodyIfNoReturnValue", propagateIncomingBodyIfNoReturnValue);
            return this;
        }
        /**
         * Sets whether to propagate the incoming body if the return type of the
         * underlying Ignite operation is void.
         * The option will be converted to a <code>boolean</code> type.
         * @group producer
         */
        default IgniteSetEndpointBuilder propagateIncomingBodyIfNoReturnValue(
                String propagateIncomingBodyIfNoReturnValue) {
            setProperty("propagateIncomingBodyIfNoReturnValue", propagateIncomingBodyIfNoReturnValue);
            return this;
        }
        /**
         * Sets whether to treat Collections as cache objects or as Collections
         * of items to insert/update/compute, etc.
         * The option is a <code>boolean</code> type.
         * @group producer
         */
        default IgniteSetEndpointBuilder treatCollectionsAsCacheObjects(
                boolean treatCollectionsAsCacheObjects) {
            setProperty("treatCollectionsAsCacheObjects", treatCollectionsAsCacheObjects);
            return this;
        }
        /**
         * Sets whether to treat Collections as cache objects or as Collections
         * of items to insert/update/compute, etc.
         * The option will be converted to a <code>boolean</code> type.
         * @group producer
         */
        default IgniteSetEndpointBuilder treatCollectionsAsCacheObjects(
                String treatCollectionsAsCacheObjects) {
            setProperty("treatCollectionsAsCacheObjects", treatCollectionsAsCacheObjects);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint for the Ignite Sets component.
     */
    public interface AdvancedIgniteSetEndpointBuilder
            extends
                EndpointProducerBuilder {
        default IgniteSetEndpointBuilder basic() {
            return (IgniteSetEndpointBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedIgniteSetEndpointBuilder basicPropertyBinding(
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
        default AdvancedIgniteSetEndpointBuilder basicPropertyBinding(
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
        default AdvancedIgniteSetEndpointBuilder synchronous(boolean synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedIgniteSetEndpointBuilder synchronous(String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Proxy enum for
     * <code>org.apache.camel.component.ignite.set.IgniteSetOperation</code>
     * enum.
     */
    public static enum IgniteSetOperation {
        CONTAINS, ADD, SIZE, REMOVE, ITERATOR, CLEAR, RETAIN_ALL, ARRAY;
    }
    /**
     * The Ignite Sets endpoint is one of camel-ignite endpoints which allows
     * you to interact with Ignite Set data structures. Creates a builder to
     * build endpoints for the Ignite Sets component.
     */
    default IgniteSetEndpointBuilder igniteSet(String path) {
        class IgniteSetEndpointBuilderImpl extends AbstractEndpointBuilder implements IgniteSetEndpointBuilder, AdvancedIgniteSetEndpointBuilder {
            public IgniteSetEndpointBuilderImpl(String path) {
                super("ignite-set", path);
            }
        }
        return new IgniteSetEndpointBuilderImpl(path);
    }
}