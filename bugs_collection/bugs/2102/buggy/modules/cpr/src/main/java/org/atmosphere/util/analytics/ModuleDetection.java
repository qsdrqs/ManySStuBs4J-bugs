/*
 * Copyright 2013 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.util.analytics;

import java.util.HashMap;
import java.util.Map;

public class ModuleDetection {

    private final static HashMap<String, String> modules = new HashMap<String, String>();

    static {
        modules.put("jersey", "org.atmosphere.jersey.AtmosphereFilter");
        modules.put("gwt", "org.atmosphere.gwt.serve.AtmosphereGwtHandler");
        modules.put("cometd", "org.atmosphere.cometd.CometdServlet");
        modules.put("socketio", "org.atmosphere.socketio.cpr.SocketIOAtmosphereHandler");
        modules.put("weblogic", "org.atmosphere.weblogic.AtmosphereWebLogicServlet");
    }

    public final static String detect() {
        for (Map.Entry<String, String> e : modules.entrySet()) {
            if (check(e.getValue())) {
                e.getKey();
            }
        }
        return "runtime";
    }

    static boolean check(String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
