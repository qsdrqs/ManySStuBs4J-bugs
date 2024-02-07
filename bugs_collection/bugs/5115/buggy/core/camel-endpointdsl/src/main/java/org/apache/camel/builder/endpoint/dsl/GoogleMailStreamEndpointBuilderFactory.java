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
 * The google-mail component provides access to Google Mail.
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.maven.packaging.EndpointDslMojo")
public interface GoogleMailStreamEndpointBuilderFactory {


    /**
     * Builder for endpoint for the Google Mail Stream component.
     */
    public static interface GoogleMailStreamEndpointBuilder
            extends
                EndpointConsumerBuilder {
        default AdvancedGoogleMailStreamEndpointBuilder advanced() {
            return (AdvancedGoogleMailStreamEndpointBuilder) this;
        }
        /**
         * Specifies an index for the endpoint.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder index(String index) {
            setProperty("index", index);
            return this;
        }
        /**
         * OAuth 2 access token. This typically expires after an hour so
         * refreshToken is recommended for long term usage.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder accessToken(String accessToken) {
            setProperty("accessToken", accessToken);
            return this;
        }
        /**
         * Google mail application name. Example would be camel-google-mail/1.0.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder applicationName(
                String applicationName) {
            setProperty("applicationName", applicationName);
            return this;
        }
        /**
         * Client ID of the mail application.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder clientId(String clientId) {
            setProperty("clientId", clientId);
            return this;
        }
        /**
         * Client secret of the mail application.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder clientSecret(String clientSecret) {
            setProperty("clientSecret", clientSecret);
            return this;
        }
        /**
         * Comma separated list of labels to take into account.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder labels(String labels) {
            setProperty("labels", labels);
            return this;
        }
        /**
         * Mark the message as read once it has been consumed.
         * The option is a <code>boolean</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder markAsRead(boolean markAsRead) {
            setProperty("markAsRead", markAsRead);
            return this;
        }
        /**
         * Mark the message as read once it has been consumed.
         * The option will be converted to a <code>boolean</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder markAsRead(String markAsRead) {
            setProperty("markAsRead", markAsRead);
            return this;
        }
        /**
         * Max results to be returned.
         * The option is a <code>long</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder maxResults(long maxResults) {
            setProperty("maxResults", maxResults);
            return this;
        }
        /**
         * Max results to be returned.
         * The option will be converted to a <code>long</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder maxResults(String maxResults) {
            setProperty("maxResults", maxResults);
            return this;
        }
        /**
         * The query to execute on gmail box.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder query(String query) {
            setProperty("query", query);
            return this;
        }
        /**
         * OAuth 2 refresh token. Using this, the Google Calendar component can
         * obtain a new accessToken whenever the current one expires - a
         * necessity if the application is long-lived.
         * The option is a <code>java.lang.String</code> type.
         * @group consumer
         */
        default GoogleMailStreamEndpointBuilder refreshToken(String refreshToken) {
            setProperty("refreshToken", refreshToken);
            return this;
        }
    }

    /**
     * Advanced builder for endpoint for the Google Mail Stream component.
     */
    public static interface AdvancedGoogleMailStreamEndpointBuilder
            extends
                EndpointConsumerBuilder {
        default GoogleMailStreamEndpointBuilder basic() {
            return (GoogleMailStreamEndpointBuilder) this;
        }
        /**
         * Whether the endpoint should use basic property binding (Camel 2.x) or
         * the newer property binding with additional capabilities.
         * The option is a <code>boolean</code> type.
         * @group advanced
         */
        default AdvancedGoogleMailStreamEndpointBuilder basicPropertyBinding(
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
        default AdvancedGoogleMailStreamEndpointBuilder basicPropertyBinding(
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
        default AdvancedGoogleMailStreamEndpointBuilder synchronous(
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
        default AdvancedGoogleMailStreamEndpointBuilder synchronous(
                String synchronous) {
            setProperty("synchronous", synchronous);
            return this;
        }
    }
    /**
     * The google-mail component provides access to Google Mail. Creates a
     * builder to build endpoints for the Google Mail Stream component.
     */
    default GoogleMailStreamEndpointBuilder googleMailStream(String path) {
        class GoogleMailStreamEndpointBuilderImpl extends AbstractEndpointBuilder implements GoogleMailStreamEndpointBuilder, AdvancedGoogleMailStreamEndpointBuilder {
            public GoogleMailStreamEndpointBuilderImpl(String path) {
                super("google-mail-stream", path);
            }
        }
        return new GoogleMailStreamEndpointBuilderImpl(path);
    }
}