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
 * The ldif component allows you to do updates on an LDAP server from a LDIF
 * body content.
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointDslMojo")
public interface LdifEndpointBuilderFactory {


    /**
     * Builder for endpoint for the LDIF component.
     */
    public static interface LdifEndpointBuilder
            extends
                EndpointProducerBuilder {
        default AdvancedLdifEndpointBuilder advanced() {
            return (AdvancedLdifEndpointBuilder) this;
        }
        /**
         * The name of the LdapConnection bean to pull from the registry. Note
         * that this must be of scope prototype to avoid it being shared among
         * threads or using a connection that has timed out.
         * The option is a <code>java.lang.String</code> type.
         * @group producer
         */
        default LdifEndpointBuilder ldapConnectionName(String ldapConnectionName) {
            setProperty("ldapConnectionName", ldapConnectionName);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint for the LDIF component.
     */
    public static interface AdvancedLdifEndpointBuilder
            extends
                EndpointProducerBuilder {
        default LdifEndpointBuilder basic() {
            return (LdifEndpointBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedLdifEndpointBuilder basicPropertyBinding(
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
        default AdvancedLdifEndpointBuilder basicPropertyBinding(
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
        default AdvancedLdifEndpointBuilder synchronous(boolean synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
        /**
         * Sets whether synchronous processing should be strictly used, or Camel
         * is allowed to use asynchronous processing (if supported).
         * The option will be converted to a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedLdifEndpointBuilder synchronous(String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }
    /**
     * The ldif component allows you to do updates on an LDAP server from a LDIF
     * body content. Creates a builder to build endpoints for the LDIF
     * component.
     */
    default LdifEndpointBuilder ldif(String path) {
        class LdifEndpointBuilderImpl extends AbstractEndpointBuilder implements LdifEndpointBuilder, AdvancedLdifEndpointBuilder {
            public LdifEndpointBuilderImpl(String path) {
                super("ldif", path);
            }
        }
        return new LdifEndpointBuilderImpl(path);
    }
}