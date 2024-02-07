/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2RestOperationsConfiguration.OAuth2ClientIdCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.OAuth2ClientConfiguration;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.util.StringUtils;

/**
 * Configuration for OAuth2 Single Sign On REST operations.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(EnableOAuth2Client.class)
@Conditional(OAuth2ClientIdCondition.class)
public class OAuth2RestOperationsConfiguration {

	@Configuration
	@ConditionalOnClientCredentials
	protected static class SingletonScopedConfiguration {

		@Bean
		@ConfigurationProperties("security.oauth2.client")
		@Primary
		public ClientCredentialsResourceDetails oauth2RemoteResource() {
			ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
			return details;
		}

		@Bean
		public DefaultOAuth2ClientContext oauth2ClientContext() {
			return new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest());
		}

	}

	@Configuration
	@ConditionalOnBean(OAuth2ClientConfiguration.class)
	@ConditionalOnNotClientCredentials
	@Import(OAuth2ProtectedResourceDetailsConfiguration.class)
	protected static class SessionScopedConfiguration {

		@Bean
		public FilterRegistrationBean oauth2ClientFilterRegistration(
				OAuth2ClientContextFilter filter, SecurityProperties security) {
			FilterRegistrationBean registration = new FilterRegistrationBean();
			registration.setFilter(filter);
			registration.setOrder(security.getFilterOrder() - 10);
			return registration;
		}

		@Configuration
		protected static class ClientContextConfiguration {

			@Resource
			@Qualifier("accessTokenRequest")
			protected AccessTokenRequest accessTokenRequest;

			@Bean
			@Scope(value = "session", proxyMode = ScopedProxyMode.INTERFACES)
			public DefaultOAuth2ClientContext oauth2ClientContext() {
				return new DefaultOAuth2ClientContext(this.accessTokenRequest);
			}

		}

	}

	/*
	 * When the authentication is per cookie but the stored token is an oauth2 one, we can
	 * pass that on to a client that wants to call downstream. We don't even need an
	 * OAuth2ClientContextFilter until we need to refresh the access token. To handle
	 * refresh tokens you need to {@code @EnableOAuth2Client}
	 */
	@Configuration
	@ConditionalOnMissingBean(OAuth2ClientConfiguration.class)
	@ConditionalOnNotClientCredentials
	@Import(OAuth2ProtectedResourceDetailsConfiguration.class)
	protected static class RequestScopedConfiguration {

		@Bean
		@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
		public DefaultOAuth2ClientContext oauth2ClientContext() {
			DefaultOAuth2ClientContext context = new DefaultOAuth2ClientContext(
					new DefaultAccessTokenRequest());
			Authentication principal = SecurityContextHolder.getContext()
					.getAuthentication();
			if (principal instanceof OAuth2Authentication) {
				OAuth2Authentication authentication = (OAuth2Authentication) principal;
				Object details = authentication.getDetails();
				if (details instanceof OAuth2AuthenticationDetails) {
					OAuth2AuthenticationDetails oauthsDetails = (OAuth2AuthenticationDetails) details;
					String token = oauthsDetails.getTokenValue();
					context.setAccessToken(new DefaultOAuth2AccessToken(token));
				}
			}
			return context;
		}

	}

	/**
	 * Condition to check if a {@code security.oauth2.client.client-id} is specified.
	 */
	static class OAuth2ClientIdCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			PropertyResolver resolver = new RelaxedPropertyResolver(
					context.getEnvironment(), "security.oauth2.client.");
			String clientId = resolver.getProperty("client-id");
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("OAuth Client ID");
			if (StringUtils.hasLength(clientId)) {
				return ConditionOutcome.match(message
						.foundExactly("security.oauth2.client.client-id property"));
			}
			return ConditionOutcome.match(message
					.didNotFind("security.oauth2.client.client-id property").atAll());
		}

	}

	@Conditional(ClientCredentialsCondition.class)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public static @interface ConditionalOnClientCredentials {

	}

	@Conditional(NotClientCredentialsCondition.class)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public static @interface ConditionalOnNotClientCredentials {

	}

	static class ClientCredentialsCondition extends AnyNestedCondition {

		public ClientCredentialsCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = "security.oauth2.client", name = "grant-type", havingValue = "client_credentials", matchIfMissing = false)
		static class ClientCredentialsConfigured {
		}

		@ConditionalOnNotWebApplication
		static class NoWebApplication {
		}
	}

	static class NotClientCredentialsCondition extends NoneNestedConditions {

		public NotClientCredentialsCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnClientCredentials
		static class ClientCredentialsActivated {
		}

	}

}