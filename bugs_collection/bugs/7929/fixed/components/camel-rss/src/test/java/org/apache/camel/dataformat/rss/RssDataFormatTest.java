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
package org.apache.camel.dataformat.rss;

import java.util.List;

import com.sun.syndication.feed.synd.SyndFeed;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.rss.RssEndpoint;
import org.apache.camel.component.rss.RssUtils;

public class RssDataFormatTest extends ContextTestSupport {
    private String feedXml;
    private SyndFeed feed;

    public void testMarshalling() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:marshal");
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(byte[].class);
        mock.message(0).bodyAs(String.class).contains(feedXml);
        mock.assertIsSatisfied();
    }

    public void testUnmarshalling() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:unmarshal");
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(SyndFeed.class);
        mock.message(0).bodyAs(SyndFeed.class).equals(feed);
        mock.assertIsSatisfied();
    }    
    
    @Override
    protected void setUp() throws Exception {
        feed = RssUtils.createFeed("file:src/test/data/rss20.xml");
        feedXml = RssConverter.feedToXml(feed);
        super.setUp();
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                // START SNIPPET: ex
                from("rss:file:src/test/data/rss20.xml?splitEntries=false&consumer.delay=1000").marshal().rss().to("mock:marshal");
                // END SNIPPET: ex
                from("rss:file:src/test/data/rss20.xml?splitEntries=false&consumer.delay=1000").marshal().rss().unmarshal().rss().to("mock:unmarshal");
            }
        };
    }

}