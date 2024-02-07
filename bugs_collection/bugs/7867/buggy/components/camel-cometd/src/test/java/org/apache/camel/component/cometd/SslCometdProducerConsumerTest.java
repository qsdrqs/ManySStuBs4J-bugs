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
package org.apache.camel.component.cometd;

import java.net.URL;
import java.util.List;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

/**
 * Unit testing for using a CometdProducer and a CometdConsumer
 */
public class SslCometdProducerConsumerTest extends ContextTestSupport {

    private static final String URI = "cometds://0.0.0.0:8080/service/test?resourceBase=./target/test-classes/webapp&"
            + "timeout=240000&interval=0&maxInterval=30000&multiFrameInterval=1500&jsonCommented=true&logLevel=2";

    protected String pwd = "changeit";
    
    public void testProducer() throws Exception {
        Person person = new Person("David", "Greco");
        template.requestBody("direct:input", person);
        MockEndpoint ep = (MockEndpoint) context.getEndpoint("mock:test");
        List<Exchange> exchanges = ep.getReceivedExchanges();
        for (Exchange exchange : exchanges) {
            Person person1 = (Person) exchange.getIn().getBody();
            assertEquals("David", person1.getName());
            assertEquals("Greco", person1.getSurname());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        stopCamelContext();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                CometdComponent component = (CometdComponent) context.getComponent("cometds");
                component.setSslPassword(pwd);
                component.setSslKeyPassword(pwd);
                URL keyStoreUrl = this.getClass().getClassLoader().getResource("jsse/localhost.ks");
                component.setSslKeystore(keyStoreUrl.getPath());

                from("direct:input").to(URI);
                from(URI).to("mock:test");
            }
        };
    }
    
    public static class Person {

        private String name;
        private String surname;
        
        Person(String name, String surname) {
            this.name = name;
            this.surname = surname;
        }
        
        public String getName() {
            return name;
        }
        public String getSurname() {
            return surname;
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setSurname(String surname) {
            this.surname = surname;
        }
    }
}