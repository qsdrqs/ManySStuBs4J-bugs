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
 * The aws-ec2 is used for managing Amazon EC2 instances.
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointDslMojo")
public interface EC2EndpointBuilderFactory {


    /**
     * Builder for endpoint for the AWS EC2 component.
     */
    public interface EC2EndpointBuilder extends EndpointProducerBuilder {
        default AdvancedEC2EndpointBuilder advanced() {
            return (AdvancedEC2EndpointBuilder) this;
        }
        /**
         * Logical name.
         * The option is a <code>java.lang.String</code> type.
         * @group producer
         */
        default EC2EndpointBuilder label(String label) {
            setProperty("label", label);
            return this;
        }
        /**
         * The region in which EC2 client needs to work.
         * The option is a <code>java.lang.String</code> type.
         * @group producer
         */
        default EC2EndpointBuilder region(String region) {
            setProperty("region", region);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint for the AWS EC2 component.
     */
    public interface AdvancedEC2EndpointBuilder
            extends
                EndpointProducerBuilder {
        default EC2EndpointBuilder basic() {
            return (EC2EndpointBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedEC2EndpointBuilder basicPropertyBinding(
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
        default AdvancedEC2EndpointBuilder basicPropertyBinding(
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
        default AdvancedEC2EndpointBuilder synchronous(boolean synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedEC2EndpointBuilder synchronous(String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }

    /**
     * Proxy enum for
     * <code>org.apache.camel.component.aws.ec2.EC2Operations</code> enum.
     */
    public static enum EC2Operations {
        createAndRunInstances, startInstances, stopInstances, terminateInstances, describeInstances, describeInstancesStatus, rebootInstances, monitorInstances, unmonitorInstances, createTags, deleteTags;
    }
    /**
     * The aws-ec2 is used for managing Amazon EC2 instances. Creates a builder
     * to build endpoints for the AWS EC2 component.
     */
    default EC2EndpointBuilder eC2(String path) {
        class EC2EndpointBuilderImpl extends AbstractEndpointBuilder implements EC2EndpointBuilder, AdvancedEC2EndpointBuilder {
            public EC2EndpointBuilderImpl(String path) {
                super("aws-ec2", path);
            }
        }
        return new EC2EndpointBuilderImpl(path);
    }
}