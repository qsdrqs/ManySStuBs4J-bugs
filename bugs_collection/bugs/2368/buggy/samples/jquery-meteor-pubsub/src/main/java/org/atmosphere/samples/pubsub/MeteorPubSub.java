/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
 * or jersey/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at jersey/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.atmosphere.samples.pubsub;

import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.HeaderConfig;
import org.atmosphere.cpr.Meteor;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple PubSub resource that demonstrate many functionality supported by
 * Atmosphere JQuery Plugin and Atmosphere Meteor extension.
 *
 * @author Jeanfrancois Arcand
 */

public class MeteorPubSub extends HttpServlet {

    private ConcurrentHashMap<String, Meteor> meteors = new ConcurrentHashMap<String, Meteor>();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Meteor m = Meteor.build(req);

        // Log all events on the console.
        m.addListener(new WebSocketEventListenerAdapter());

        String trackingId = trackingId(req);

        meteors.put(trackingId, m);

        res.setContentType("text/html;charset=ISO-8859-1");

        String[] decodedPath = req.getPathInfo().split("/");
        Broadcaster b = BroadcasterFactory.getDefault().lookup(decodedPath[decodedPath.length - 1], true);
        m.setBroadcaster(b);

        if (req.getHeader(HeaderConfig.X_ATMOSPHERE_TRANSPORT).equalsIgnoreCase(HeaderConfig.LONG_POLLING_TRANSPORT)) {
            req.setAttribute(ApplicationConfig.RESUME_ON_BROADCAST, Boolean.TRUE);
            m.suspend(-1, false);
        } else {
            m.suspend(-1);
        }

    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Meteor m = meteors.get(trackingId(req));
        res.setCharacterEncoding("UTF-8");

        String message = req.getReader().readLine();

        if (message != null && message.indexOf("message") != -1) {
            m.getBroadcaster().broadcast(message.substring("message=".length()));
        }
    }

    String trackingId(HttpServletRequest req) {
        String trackingId = req.getAttribute(HeaderConfig.X_ATMOSPHERE_TRACKING_ID) != null ?
                (String) req.getAttribute(HeaderConfig.X_ATMOSPHERE_TRACKING_ID) : req.getParameter(HeaderConfig.X_ATMOSPHERE_TRACKING_ID);
        return trackingId;
    }
}
