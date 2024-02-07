/**
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
package org.apache.camel.spring;

import org.apache.camel.Endpoint;
import org.apache.camel.component.bean.BeanProcessor;
import org.apache.camel.component.event.EventComponent;
import org.apache.camel.component.event.EventEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.ProcessorEndpoint;
import org.apache.camel.spi.Injector;
import org.apache.camel.spi.Registry;
import org.apache.camel.spring.spi.ApplicationContextRegistry;
import org.apache.camel.spring.spi.SpringInjector;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.apache.camel.util.ObjectHelper.wrapRuntimeCamelException;

/**
 * A Spring aware implementation of {@link org.apache.camel.CamelContext} which
 * will automatically register itself with Springs lifecycle methods plus allows
 * spring to be used to customize a any <a
 * href="http://camel.apache.org/type-converter.html">Type Converters</a>
 * as well as supporting accessing components and beans via the Spring
 * {@link ApplicationContext}
 *
 * @version $Revision$
 */
public class SpringCamelContext extends DefaultCamelContext implements InitializingBean, DisposableBean,
        ApplicationContextAware, ApplicationListener {

    private static final transient Log LOG = LogFactory.getLog(SpringCamelContext.class);
    private ApplicationContext applicationContext;
    private EventEndpoint eventEndpoint;
    private boolean shouldStartContext = ObjectHelper.getSystemProperty("shouldStartContext", Boolean.TRUE);

    public SpringCamelContext() {
    }

    public SpringCamelContext(ApplicationContext applicationContext) {
        setApplicationContext(applicationContext);
    }

    public static SpringCamelContext springCamelContext(ApplicationContext applicationContext) throws Exception {
        // lets try and look up a configured camel context in the context
        String[] names = applicationContext.getBeanNamesForType(SpringCamelContext.class);
        if (names.length == 1) {
            return (SpringCamelContext)applicationContext.getBean(names[0], SpringCamelContext.class);
        }
        SpringCamelContext answer = new SpringCamelContext();
        answer.setApplicationContext(applicationContext);
        answer.afterPropertiesSet();
        return answer;
    }

    public static SpringCamelContext springCamelContext(String configLocations) throws Exception {
        return springCamelContext(new ClassPathXmlApplicationContext(configLocations));
    }

    public void afterPropertiesSet() throws Exception {
        maybeStart();
    }

    private void maybeStart() throws Exception {
        if (!getShouldStartContext()) {
            LOG.info("Not starting Apache Camel as property ShouldStartContext is false");
            return;
        }

        if (!isStarted()) {
            LOG.info("Starting Apache Camel as property ShouldStartContext is true");
            start();
        } else {
            // ignore as Camel is already started
            LOG.trace("Ignoring maybeStart() as Apache Camel is already started");
        }
    }

    public void destroy() throws Exception {
        stop();
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onApplicationEvent: " + event);
        }

        if (event instanceof ContextRefreshedEvent) {
            // now lets start the CamelContext so that all its possible
            // dependencies are initialized
            try {
                maybeStart();
            } catch (Exception e) {
                throw wrapRuntimeCamelException(e);
            }
        }

        if (eventEndpoint != null) {
            eventEndpoint.onApplicationEvent(event);
        } else {
            LOG.warn("No spring-event endpoint enabled to handle event: " + event);
        }
    }

    // Properties
    // -----------------------------------------------------------------------

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

        if (applicationContext instanceof ConfigurableApplicationContext) {
            addComponent("spring-event", new EventComponent(applicationContext));
        }
    }

    public EventEndpoint getEventEndpoint() {
        return eventEndpoint;
    }

    public void setEventEndpoint(EventEndpoint eventEndpoint) {
        this.eventEndpoint = eventEndpoint;
    }

    // Implementation methods
    // -----------------------------------------------------------------------

    @Override
    protected void doStart() throws Exception {
        maybeDoStart();
    }

    protected void maybeDoStart() throws Exception {
        if (getShouldStartContext()) {
            super.doStart();
            if (eventEndpoint == null) {
                eventEndpoint = createEventEndpoint();
            }
        }
    }

    @Override
    protected Injector createInjector() {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            return new SpringInjector((ConfigurableApplicationContext)applicationContext);
        } else {
            LOG.warn("Cannot use SpringInjector as applicationContext is not a ConfigurableApplicationContext as its: "
                      + applicationContext);
            return super.createInjector();
        }
    }

    protected EventEndpoint createEventEndpoint() {
        return getEndpoint("spring-event:default", EventEndpoint.class);
    }

    protected Endpoint convertBeanToEndpoint(String uri, Object bean) {
        // We will use the type convert to build the endpoint first
        Endpoint endpoint = getTypeConverter().convertTo(Endpoint.class, bean);
        if (endpoint != null) {
            endpoint.setCamelContext(this);
            return endpoint;
        }

        return new ProcessorEndpoint(uri, this, new BeanProcessor(bean, this));
    }

    @Override
    protected Registry createRegistry() {
        return new ApplicationContextRegistry(getApplicationContext());
    }

    public void setShouldStartContext(boolean shouldStartContext) {
        this.shouldStartContext = shouldStartContext;
    }

    public boolean getShouldStartContext() {
        return shouldStartContext;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SpringCamelContext(").append(getName()).append(")");
        if (applicationContext != null) {
            sb.append(" with spring id ").append(applicationContext.getId());
        }
        return sb.toString();
    }

}