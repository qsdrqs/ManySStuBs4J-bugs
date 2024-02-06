/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
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
 *
 */

package org.atmosphere.cpr;

import org.apache.catalina.CometEvent;
import org.apache.catalina.CometProcessor;
import org.atmosphere.container.BlockingIOCometSupport;
import org.atmosphere.container.GoogleAppEngineCometSupport;
import org.atmosphere.container.JBossWebCometSupport;
import org.atmosphere.container.TomcatCometSupport;
import org.atmosphere.container.WebLogicCometSupport;
import org.atmosphere.handler.ReflectorServletProcessor;
import org.atmosphere.util.AtmosphereConfigReader;
import org.atmosphere.util.AtmosphereConfigReader.Property;
import org.atmosphere.util.IntrospectionUtils;
import org.atmosphere.util.LoggerUtils;
import org.atmosphere.util.Version;
import org.atmosphere.util.gae.GAEBroadcasterConfig;
import org.atmosphere.websocket.JettyWebSocketSupport;
import org.atmosphere.websocket.WebSocketAtmosphereHandler;
import org.eclipse.jetty.websocket.WebSocket;
import org.jboss.servlet.http.HttpEvent;
import org.jboss.servlet.http.HttpEventServlet;
import weblogic.servlet.http.AbstractAsyncServlet;
import weblogic.servlet.http.RequestResponseKey;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link AtmosphereServlet} acts as a dispatcher for {@link AtmosphereHandler}
 * defined in META-INF/atmosphere.xml, or if atmosphere.xml is missing, all classes
 * that implements {@link AtmosphereHandler} will be discovered and mapped using
 * the class's name.
 * <p/>
 * This {@link Servlet} can be defined inside an application's web.xml using the following:
 * <p><pre><code>
 *  &lt;servlet&gt;
 *      &lt;description&gt;AtmosphereServlet&lt;/description&gt;
 *      &lt;servlet-name&gt;AtmosphereServlet&lt;/servlet-name&gt;
 *      &lt;servlet-class&gt;org.atmosphere.cpr.AtmosphereServlet&lt;/servlet-class&gt;
 *      &lt;load-on-startup&gt;0 &lt;/load-on-startup&gt;
 *  &lt;/servlet&gt;
 *  &lt;servlet-mapping&gt;
 *      &lt;servlet-name&gt;AtmosphereServlet&lt;/servlet-name&gt;
 *      &lt;url-pattern&gt;/Atmosphere &lt;/url-pattern&gt;
 *  &lt;/servlet-mapping&gt;
 * </code></pre></p>
 * You can force this Servlet to use native API of the Web Server instead of
 * the Servlet 3.0 Async API you are deploying on by adding
 * <p><pre><code>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.useNative&lt;/param-name&gt;
 *      &lt;param-value&gt;true&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </code></pre></p>
 * You can force this Servlet to use one Thread per connection instead of
 * native API of the Web Server you are deploying on by adding
 * <p><pre><code>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.useBlocking&lt;/param-name&gt;
 *      &lt;param-value&gt;true&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </code></pre></p>
 * You can also define {@link Broadcaster}by adding:
 * <p><pre><code>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.cpr.broadcasterClass&lt;/param-name&gt;
 *      &lt;param-value&gt;class-name&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </code></pre></p>
 * You can also for Atmosphere to use {@link java.io.OutputStream} for all write operations.
 * <p><pre><code>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.useStream&lt;/param-name&gt;
 *      &lt;param-value&gt;true&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </code></pre></p>
 * You can also configure {@link org.atmosphere.cpr.BroadcasterCache} that persist message when Browser is disconnected.
 * <p><pre><code>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.cpr.broadcasterCacheClass&lt;/param-name&gt;
 *      &lt;param-value&gt;class-name&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </code></pre></p>
 * You can also configure Atmosphere to use http session or not
 * <p><pre><code>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.cpr.sessionSupport&lt;/param-name&gt;
 *      &lt;param-value&gt;false&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </code></pre></p>
 * You can also configure {@link BroadcastFilter} that will be applied at all newly created {@link Broadcaster}
 * <p><pre><code>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.cpr.broadcastFilterClasses&lt;/param-name&gt;
 *      &lt;param-value&gt;BroadcastFilter class name separated by coma&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </code></pre></p>
 * The Atmosphere Framework can also be used as a Servlet Filter ({@link AtmosphereFilter}).
 * <p/>
 * If you are planning to use JSP, Servlet or JSF, you can instead use the
 * {@link MeteorServlet}, which allow the use of {@link Meteor} inside those
 * components.
 *
 * @author Jeanfrancois Arcand
 */
public class AtmosphereServlet extends AbstractAsyncServlet implements CometProcessor, HttpEventServlet {
    public final static String JERSEY_BROADCASTER = "org.atmosphere.jersey.JerseyBroadcaster";
    public final static String JERSEY_CONTAINER = "com.sun.jersey.spi.container.servlet.ServletContainer";
    public final static String GAE_BROADCASTER = org.atmosphere.util.gae.GAEDefaultBroadcaster.class.getName();
    public final static String PROPERTY_SERVLET_MAPPING = "org.atmosphere.jersey.servlet-mapping";
    public final static String PROPERTY_BLOCKING_COMETSUPPORT = "org.atmosphere.useBlocking";
    public final static String PROPERTY_NATIVE_COMETSUPPORT = "org.atmosphere.useNative";
    public final static String WEBSOCKET_SUPPORT = "org.atmosphere.useWebSocket";
    public final static String PROPERTY_USE_STREAM = "org.atmosphere.useStream";
    public final static String BROADCASTER_FACTORY = "org.atmosphere.cpr.broadcasterFactory";
    public final static String BROADCASTER_CLASS = "org.atmosphere.cpr.broadcasterClass";
    public final static String BROADCASTER_CACHE = "org.atmosphere.cpr.broadcasterCacheClass";
    public final static String PROPERTY_COMET_SUPPORT = "org.atmosphere.cpr.cometSupport";
    public final static String PROPERTY_SESSION_SUPPORT = "org.atmosphere.cpr.sessionSupport";
    public final static String PRIMEFACES_SERVLET = "org.primefaces.comet.PrimeFacesCometServlet";
    public final static String DISABLE_ONSTATE_EVENT = "org.atmosphere.disableOnStateEvent";
    public final static String WEB_INF = "/WEB-INF/classes/";
    public final static String RESUME_ON_BROADCAST = "org.atmosphere.resumeOnBroadcast";
    public final static String ATMOSPHERE_SERVLET = AtmosphereServlet.class.getName();
    public final static String ATMOSPHERE_RESOURCE = AtmosphereResource.class.getName();
    public final static String SUPPORT_SESSION = "org.atmosphere.cpr.AsynchronousProcessor.supportSession";
    public final static String ATMOSPHERE_HANDLER = AtmosphereHandler.class.getName();
    public final static String WEBSOCKET_ATMOSPHEREHANDLER = WebSocketAtmosphereHandler.class.getName();
    public final static Logger logger = LoggerUtils.getLogger();
    public final static String RESUME_AND_KEEPALIVE = AtmosphereServlet.class.getName() + ".resumeAndKeepAlive";
    public final static String RESUMED_ON_TIMEOUT = AtmosphereServlet.class.getName() + ".resumedOnTimeout";
    public final static String DEFAULT_NAMED_DISPATCHER = "default";
    public final static String BROADCAST_FILTER_CLASSES = "org.atmosphere.cpr.broadcastFilterClasses";
   
    protected final ArrayList<String> possibleAtmosphereHandlersCandidate = new ArrayList<String>();
    protected final HashMap<String, String> initParams = new HashMap<String, String>();
    protected final AtmosphereConfig config = new AtmosphereConfig();
    protected final AtomicBoolean isCometSupportConfigured = new AtomicBoolean(false);
    protected final boolean isFilter;
    /**
     * The list of {@link AtmosphereHandler} and their associated mapping.
     */
    private final Map<String, AtmosphereHandlerWrapper> atmosphereHandlers =
            new ConcurrentHashMap<String, AtmosphereHandlerWrapper>();

    // If we detect Servlet 3.0, should we still use the default
    // native Comet API.
    protected boolean useNativeImplementation = false;
    protected boolean useBlockingImplementation = false;
    protected boolean useStreamForFlushingComments = false;
    protected CometSupport cometSupport;
    protected static String broadcasterClassName = DefaultBroadcaster.class.getName();
    protected boolean isCometSupportSpecified = false;
    protected boolean isBroadcasterSpecified = false;
    protected boolean isSessionSupportSpecified = false;
    private BroadcasterFactory broadcasterFactory;
    private static BroadcasterConfig broadcasterConfig = new BroadcasterConfig();
    private String broadcasterCacheClassName;
    private boolean webSocketEnabled = false;

    public final static class AtmosphereHandlerWrapper {

        public final AtmosphereHandler atmosphereHandler;
        public Broadcaster broadcaster;

        public AtmosphereHandlerWrapper(AtmosphereHandler atmosphereHandler) {
            this.atmosphereHandler = atmosphereHandler;
            try {
                this.broadcaster = BroadcasterFactory.getDefault().get();
            } catch (Exception t) {
                throw new RuntimeException(t);
            }
        }

        public AtmosphereHandlerWrapper(AtmosphereHandler atmosphereHandler, Broadcaster broadcaster) {
            this.atmosphereHandler = atmosphereHandler;
            this.broadcaster = broadcaster;
        }
    }

    /**
     * Return a configured instance of {@link AtmosphereConfig}
     *
     * @return a configured instance of {@link AtmosphereConfig}
     */
    public AtmosphereConfig getAtmosphereConfig() {
        return config;
    }

    public class AtmosphereConfig {

        private boolean supportSession = true;
        private BroadcasterFactory broadcasterFactory;
        private String dispatcherName = DEFAULT_NAMED_DISPATCHER;

        protected Map<String, AtmosphereHandlerWrapper> handlers() {
            return AtmosphereServlet.this.atmosphereHandlers;
        }

        public ServletContext getServletContext() {
            return AtmosphereServlet.this.getServletContext();
        }

        public String getDispatcherName() {
            return dispatcherName;
        }

        public void setDispatcherName(String dispatcherName) {
            this.dispatcherName = dispatcherName;
        }

        public String getInitParameter(String name) {
            // First looks locally
            String s = initParams.get(name);
            if (s != null) {
                return s;
            }

            return AtmosphereServlet.this.getInitParameter(name);
        }

        public Enumeration getInitParameterNames() {
            return AtmosphereServlet.this.getInitParameterNames();
        }

        public ServletConfig getServletConfig() {
            return AtmosphereServlet.this.getServletConfig();
        }

        public String getWebServerName() {
            return AtmosphereServlet.this.cometSupport.getContainerName();
        }

        public boolean mapBroadcasterToAtmosphereHandler(Broadcaster bc, AtmosphereHandlerWrapper ahw) {
            if (atmosphereHandlers.get(bc.getID()) == null) {
                atmosphereHandlers.put(bc.getID(), ahw);
                return true;
            }
            return false;
        }

        /**
         * Return the {@link AtmosphereHandler} associated with this {@link Broadcaster}.
         *
         * @param bc The {@link Broadcaster}
         * @return the {@link AtmosphereHandler} associated with this {@link Broadcaster}.
         */
        public AtmosphereHandler getAtmosphereHandler(Broadcaster bc) {
            AtmosphereHandler h = atmosphereHandlers.get(bc.getID()).atmosphereHandler;
            if (h == null) {
                for (AtmosphereHandlerWrapper ah : atmosphereHandlers.values()) {
                    if (ah.broadcaster == bc) {
                        atmosphereHandlers.put(bc.getID(), ah);
                        return ah.atmosphereHandler;
                    }
                }
                throw new IllegalStateException("Unable to find associated AtmosphereHandler");
            } else {
                return h;
            }
        }

        /**
         * Return an instance of a {@link DefaultBroadcasterFactory}
         *
         * @return an instance of a {@link DefaultBroadcasterFactory}
         */
        public BroadcasterFactory getBroadcasterFactory() {
            return broadcasterFactory;
        }

        public boolean isSupportSession() {
            return supportSession;
        }

        public void setSupportSession(boolean supportSession) {
            this.supportSession = supportSession;
        }

        public AtmosphereServlet getServlet() {
            return AtmosphereServlet.this;
        }
    }

    /**
     * Simple class/struck that hold the current state.
     */
    public static class Action {

        public enum TYPE {
            SUSPEND, RESUME, TIMEOUT, CANCELLED, KEEP_ALIVED
        }

        public long timeout = -1L;

        public TYPE type;

        public Action() {
            type = TYPE.CANCELLED;
        }

        public Action(TYPE type) {
            this.type = type;
        }

        public Action(TYPE type, long timeout) {
            this.timeout = timeout;
            this.type = type;
        }
    }

    /**
     * Create an Atmosphere Servlet.
     */
    public AtmosphereServlet() {
        this(false);
    }

    /**
     * Create an Atmosphere Servlet.
     *
     * @param isFilter true if this instance is used as an {@link AtmosphereFilter}
     */
    public AtmosphereServlet(boolean isFilter) {
        this.isFilter = isFilter;
        readSystemProperties();
    }

    /**
     * Add an {@link AtmosphereHandler} serviced by the {@link Servlet}
     * This API is exposed to allow embedding an Atmosphere application.
     *
     * @param mapping The servlet mapping (servlet path)
     * @param h       implementation of an {@link AtmosphereHandler}
     */
    public void addAtmosphereHandler(String mapping, AtmosphereHandler h) {
        if (!mapping.startsWith("/")) {
            mapping = "/" + mapping;
        }

        AtmosphereHandlerWrapper w = new AtmosphereHandlerWrapper(h);
        atmosphereHandlers.put(mapping, w);
    }

    /**
     * Add an {@link AtmosphereHandler} serviced by the {@link Servlet}
     * This API is exposed to allow embedding an Atmosphere application.
     *
     * @param mapping       The servlet mapping (servlet path)
     * @param h             implementation of an {@link AtmosphereHandler}
     * @param broadcasterId The {@link Broadcaster#getID} value.
     */
    public void addAtmosphereHandler(String mapping, AtmosphereHandler h, String broadcasterId) {
        if (!mapping.startsWith("/")) {
            mapping = "/" + mapping;
        }

        AtmosphereHandlerWrapper w = new AtmosphereHandlerWrapper(h);
        w.broadcaster.setID(broadcasterId);
        atmosphereHandlers.put(mapping, w);
    }

    /**
     * Add an {@link AtmosphereHandler} serviced by the {@link Servlet}
     * This API is exposed to allow embedding an Atmosphere application.
     *
     * @param mapping     The servlet mapping (servlet path)
     * @param h           implementation of an {@link AtmosphereHandler}
     * @param broadcaster The {@link Broadcaster} associated with AtmosphereHandler.
     */
    public void addAtmosphereHandler(String mapping, AtmosphereHandler h, Broadcaster broadcaster) {
        if (!mapping.startsWith("/")) {
            mapping = "/" + mapping;
        }

        AtmosphereHandlerWrapper w = new AtmosphereHandlerWrapper(h, broadcaster);
        atmosphereHandlers.put(mapping, w);
    }

    /**
     * Remove an {@link AtmosphereHandler}
     *
     * @param mapping the mapping used when invoking {@link #addAtmosphereHandler(String, AtmosphereHandler)};
     * @return true if removed
     */
    public boolean removeAtmosphereHandler(String mapping) {
        return atmosphereHandlers.remove(mapping) == null ? false : true;
    }

    /**
     * Remove all {@link AtmosphereHandler}
     */
    public void removeAllAtmosphereHandler() {
        atmosphereHandlers.clear();
    }

    /**
     * Remove all init parameters.
     */
    public void removeAllInitParams() {
        initParams.clear();
    }

    /**
     * Add init-param like if they were defined in web.xml
     *
     * @param name  The name
     * @param value The value
     */
    public void addInitParameter(String name, String value) {
        initParams.put(name, value);
    }

    protected void readSystemProperties() {
        if (System.getProperty(PROPERTY_NATIVE_COMETSUPPORT) != null) {
            useNativeImplementation = Boolean
                    .parseBoolean(System.getProperty(PROPERTY_NATIVE_COMETSUPPORT));
            isCometSupportSpecified = true;
        }

        if (System.getProperty(PROPERTY_BLOCKING_COMETSUPPORT) != null) {
            useBlockingImplementation = Boolean
                    .parseBoolean(System.getProperty(PROPERTY_BLOCKING_COMETSUPPORT));
            isCometSupportSpecified = true;
        }

        if (System.getProperty(DISABLE_ONSTATE_EVENT) != null) {
            initParams.put(DISABLE_ONSTATE_EVENT, System.getProperty(DISABLE_ONSTATE_EVENT));
        }
    }

    /**
     * Load the {@link AtmosphereHandler} associated with this AtmosphereServlet.
     *
     * @param sc the {@link ServletContext}
     */
    @Override
    public void init(final ServletConfig sc) throws ServletException {
        try {
            super.init(sc);

            ServletConfig scFacade = new ServletConfig() {

                public String getServletName() {
                    return sc.getServletName();
                }

                public ServletContext getServletContext() {
                    return sc.getServletContext();
                }

                public String getInitParameter(String name) {
                    String param = sc.getInitParameter(name);
                    if (param == null) {
                        return initParams.get(name);
                    }
                    return param;
                }

                public Enumeration<String> getInitParameterNames() {
                    return sc.getInitParameterNames();
                }
            };

            doInitParams(scFacade);
            detectGoogleAppEngine(scFacade);
            loadConfiguration(scFacade);

            autoDetectContainer();
            configureBroadcaster();
            cometSupport.init(scFacade);
            initAtmosphereServletProcessor(scFacade);
            logger.log(Level.INFO, "Atmosphere Framework " + Version.getRawVersion() + " started.");
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "", t);
            if (t instanceof ServletException)
                throw (ServletException) t;

            throw new ServletException(t.getCause());
        }
    }

    protected void configureBroadcaster() throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        if (broadcasterFactory == null) {
            broadcasterFactory = new DefaultBroadcasterFactory((Class<Broadcaster>)
                    Thread.currentThread().getContextClassLoader().loadClass(broadcasterClassName), broadcasterConfig);
            config.broadcasterFactory = broadcasterFactory;
        }

        Iterator<Entry<String, AtmosphereHandlerWrapper>> i = atmosphereHandlers.entrySet().iterator();
        AtmosphereHandlerWrapper w;
        Entry<String, AtmosphereHandlerWrapper> e;
        while (i.hasNext()) {
            e = i.next();
            w = e.getValue();
            if (w.broadcaster == null) {
                w.broadcaster = broadcasterFactory.get();
            } else {
                w.broadcaster.setBroadcasterConfig(broadcasterConfig);
            }
            w.broadcaster.setID(e.getKey());
        }

        if (broadcasterCacheClassName != null) {
            broadcasterConfig.setBroadcasterCache((BroadcasterCache)
                    Thread.currentThread().getContextClassLoader().loadClass(broadcasterCacheClassName).newInstance());
        }

        logger.info("Using " + broadcasterClassName);
    }

    /**
     * Read init param from web.xml and apply them.
     *
     * @param sc {@link ServletConfig}
     */
    protected void doInitParams(ServletConfig sc) {
        String s = sc.getInitParameter(PROPERTY_NATIVE_COMETSUPPORT);
        if (s != null) {
            useNativeImplementation = Boolean.parseBoolean(s);
            if (useNativeImplementation) isCometSupportSpecified = true;
        }
        s = sc.getInitParameter(PROPERTY_BLOCKING_COMETSUPPORT);
        if (s != null) {
            useBlockingImplementation = Boolean.parseBoolean(s);
            if (useBlockingImplementation) isCometSupportSpecified = true;
        }
        s = sc.getInitParameter(PROPERTY_USE_STREAM);
        if (s != null) {
            useStreamForFlushingComments = Boolean.parseBoolean(s);
        }
        s = sc.getInitParameter(PROPERTY_COMET_SUPPORT);
        if (s != null) {
            cometSupport = new DefaultCometSupportResolver(config).newCometSupport(s);
            isCometSupportSpecified = true;
        }
        s = sc.getInitParameter(BROADCASTER_CLASS);
        if (s != null) {
            broadcasterClassName = s;
            isBroadcasterSpecified = true;
        }
        s = sc.getInitParameter(BROADCASTER_CACHE);
        if (s != null) {
            broadcasterCacheClassName = s;
        }
        s = sc.getInitParameter(PROPERTY_SESSION_SUPPORT);
        if (s != null) {
            config.supportSession = Boolean.valueOf(s);
            isSessionSupportSpecified = true;
        }
        s = sc.getInitParameter(WEBSOCKET_ATMOSPHEREHANDLER);
        if (s != null) {
            addAtmosphereHandler("/*", new WebSocketAtmosphereHandler());
            webSocketEnabled = true;
            sessionSupport(false);
        }
        s = sc.getInitParameter(WEBSOCKET_SUPPORT);
        if (s != null) {
            webSocketEnabled = true;
            sessionSupport(false);
        }
        s = sc.getInitParameter(DISABLE_ONSTATE_EVENT);
        if (s != null) {
            initParams.put(DISABLE_ONSTATE_EVENT, s);
        } else {
            initParams.put(DISABLE_ONSTATE_EVENT, "false");
        }
        s = sc.getInitParameter(RESUME_AND_KEEPALIVE);
        if (s != null) {
            initParams.put(RESUME_AND_KEEPALIVE, s);
        }
        s = sc.getInitParameter(BROADCAST_FILTER_CLASSES);
        if (s != null) {
            configureBroadcasterFilter(s.split(","));
        }
    }

    protected void loadConfiguration(ServletConfig sc) throws ServletException {
        try {
            //TODO -> Add support for WEB-INF/lib/*.jar
            URL url = sc.getServletContext().getResource("/WEB-INF/classes/");
            URLClassLoader urlC = new URLClassLoader(new URL[]{url},
                    Thread.currentThread().getContextClassLoader());
            loadAtmosphereDotXml(sc.getServletContext().
                    getResourceAsStream("/META-INF/atmosphere.xml"), urlC);
            if (atmosphereHandlers.size() == 0) {
                autoDetectAtmosphereHandlers(sc.getServletContext(), urlC);

                if (atmosphereHandlers.size() == 0) {
                    detectSupportedFramework(sc);
                }
            }
        } catch (Throwable t) {
            throw new ServletException(t);
        }
    }

    void configureBroadcasterFilter(String[] list){
        for (String broadcastFilter: list) {
            try {
                broadcasterConfig.addFilter(BroadcastFilter.class.cast(
                    Thread.currentThread().getContextClassLoader().loadClass(broadcastFilter).newInstance()));
            } catch (InstantiationException e) {
                logger.log(Level.WARNING,String.format("Error trying to instanciate BroadcastFilter %s",broadcastFilter),e);
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING,String.format("Error trying to instanciate BroadcastFilter %s",broadcastFilter),e);
            } catch (ClassNotFoundException e) {
                logger.log(Level.WARNING,String.format("Error trying to instanciate BroadcastFilter %s",broadcastFilter),e);
            }
        }
    }

    /**
     * Auto-detect Jersey when no atmosphere.xml file are specified.
     *
     * @param sc {@link ServletConfig}
     * @return true if Jersey classes are detected
     * @throws ClassNotFoundException
     */
    protected boolean detectSupportedFramework(ServletConfig sc) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {

        // If Primefaces is detected, never starts Jersey.
        // TODO: Remove this hack once properly implemented in PrimeFaces
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass(PRIMEFACES_SERVLET);
            return false;
        } catch (Throwable ignored) {
        }

        try {
            cl.loadClass(JERSEY_CONTAINER);
            useStreamForFlushingComments = true;
        } catch (Throwable t) {
            return false;
        }

        logger.warning("Missing META-INF/atmosphere.xml but found the Jersey runtime. Starting Jersey");
        ReflectorServletProcessor rsp = new ReflectorServletProcessor();
        if (!isBroadcasterSpecified) broadcasterClassName = JERSEY_BROADCASTER;
        rsp.setServletClassName(JERSEY_CONTAINER);
        sessionSupport(false);
        initParams.put(DISABLE_ONSTATE_EVENT, "true");

        String mapping = sc.getInitParameter(PROPERTY_SERVLET_MAPPING);
        if (mapping == null) {
            mapping = "/*";
        }
        Class<? extends Broadcaster> bc = (Class<? extends Broadcaster>) cl.loadClass(broadcasterClassName);

        Broadcaster b = bc.getDeclaredConstructor(new Class[]{String.class}).newInstance(mapping);

        addAtmosphereHandler(mapping, rsp, b);
        return true;
    }

    protected void sessionSupport(boolean sessionSupport) {
        if (!isSessionSupportSpecified) {
            config.supportSession = sessionSupport;
        }
    }

    /**
     * Auto-Detect Google App Engine.
     *
     * @param sc (@link ServletConfig}
     * @return true if detected
     */
    boolean detectGoogleAppEngine(ServletConfig sc) {
        if (sc.getServletContext().getServerInfo().startsWith("Google")) {
            broadcasterClassName = GAE_BROADCASTER;
            isBroadcasterSpecified = true;
            cometSupport = new GoogleAppEngineCometSupport(config);
            broadcasterConfig = new GAEBroadcasterConfig();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Initialize {@link AtmosphereServletProcessor}
     *
     * @param sc the {@link ServletConfig}
     * @throws javax.servlet.ServletException
     */
    void initAtmosphereServletProcessor(ServletConfig sc) throws ServletException {
        AtmosphereHandler a;
        for (Entry<String, AtmosphereHandlerWrapper> h : atmosphereHandlers.entrySet()) {
            a = h.getValue().atmosphereHandler;
            if (a instanceof AtmosphereServletProcessor) {
                ((AtmosphereServletProcessor) a).init(sc);
            }
        }
    }

    @Override
    public void destroy() {
        if (cometSupport != null && AsynchronousProcessor.class.isAssignableFrom(cometSupport.getClass())) {
            ((AsynchronousProcessor) cometSupport).shutdown();
        }

        AtmosphereHandler a;
        for (Entry<String, AtmosphereHandlerWrapper> h : atmosphereHandlers.entrySet()) {
            a = h.getValue().atmosphereHandler;
            if (a instanceof AtmosphereServletProcessor) {
                ((AtmosphereServletProcessor) a).destroy();
            }
            h.getValue().broadcaster.destroy();
        }
        BroadcasterFactory.factory = null;
        BroadcasterFactory.getDefault().destroy();
        broadcasterConfig = new BroadcasterConfig();
    }

    /**
     * Load AtmosphereHandler defined under META-INF/atmosphere.xml
     *
     * @param stream The input stream we read from.
     * @param c      The classloader
     */
    protected void loadAtmosphereDotXml(InputStream stream, URLClassLoader c)
            throws IOException, ServletException {

        if (stream == null) {
            return;
        }

        AtmosphereConfigReader reader = new AtmosphereConfigReader(stream);

        Map<String, String> atmosphereHandlerNames = reader.getAtmosphereHandlers();
        Set<Entry<String, String>> entries = atmosphereHandlerNames.entrySet();
        for (Entry<String, String> entry : entries) {
            AtmosphereHandler g;
            try {
                if (!entry.getValue().equals(ReflectorServletProcessor.class.getName())) {
                    g = (AtmosphereHandler) c.loadClass(entry.getValue()).newInstance();
                } else {
                    g = new ReflectorServletProcessor();
                }
                logger.info("Sucessfully loaded " + g
                        + " mapped to context-path " + entry.getKey());

                AtmosphereHandlerWrapper wrapper = new AtmosphereHandlerWrapper(g);
                atmosphereHandlers.put(entry.getKey(), wrapper);
                boolean isJersey = false;
                for (Property p : reader.getProperty(entry.getKey())) {
                    if (p.value != null && p.value.indexOf("jersey") != -1) {
                        isJersey = true;
                        initParams.put(DISABLE_ONSTATE_EVENT, "true");
                        useStreamForFlushingComments = true;
                    }
                    IntrospectionUtils.setProperty(g, p.name, p.value);
                }

                config.supportSession = !isJersey;

                if (!reader.supportSession().equals("")) {
                    sessionSupport(Boolean.valueOf(reader.supportSession()));
                }

                for (Property p : reader.getProperty(entry.getKey())) {
                    IntrospectionUtils.addProperty(g, p.name, p.value);
                }

                String broadcasterClass = reader.getBroadcasterClass(entry.getKey());
                /**
                 * If there is more than one AtmosphereHandler defined, their Broadcaster
                 * may clash each other with the BroadcasterFactory. In that case we will use the
                 * last one defined.
                 */
                if (broadcasterClass != null) {
                    broadcasterClassName = broadcasterClass;
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Class<? extends Broadcaster> bc = (Class<? extends Broadcaster>) cl.loadClass(broadcasterClassName);
                    wrapper.broadcaster = BroadcasterFactory.getDefault().get(bc, entry.getKey());
                }

                String bc = reader.getBroadcasterCache(entry.getKey());
                if (bc != null) {
                    broadcasterCacheClassName = bc;
                }

                if (reader.getCometSupportClass() != null) {
                    cometSupport = (CometSupport)
                            c.loadClass(reader.getCometSupportClass()).newInstance();
                }

                if (reader.getBroadcastFilterClasses() != null){
                    configureBroadcasterFilter(reader.getBroadcastFilterClasses());
                }

            } catch (Throwable t) {
                logger.log(Level.WARNING, "Unable to load AtmosphereHandler class: "
                        + entry.getValue(), t);
                throw new ServletException(t);
            }
        }
    }

    /**
     * Set the {@link CometSupport} implementation. Make sure you don't set
     * an implementation that only works on some Container. See {@link BlockingIOCometSupport}
     * for an example.
     *
     * @param cometSupport
     */
    public void setCometSupport(CometSupport cometSupport) {
        this.cometSupport = cometSupport;
    }

    /**
     * Return the current {@link CometSupport}
     *
     * @return the current {@link CometSupport}
     */
    public CometSupport getCometSupport() {
        return cometSupport;
    }

    /**
     * Returns an instance of CometSupportResolver {@link CometSupportResolver}
     *
     * @return CometSupportResolver
     */
    protected CometSupportResolver createCometSupportResolver() {
        return new DefaultCometSupportResolver(config);
    }


    /**
     * Auto detect the underlying Servlet Container we are running on.
     */
    protected void autoDetectContainer() {
        // Was defined in atmosphere.xml
        if (getCometSupport() == null) {
            setCometSupport(createCometSupportResolver().resolve(useNativeImplementation, useBlockingImplementation, webSocketEnabled));
        }
        logger.info("Atmosphere is using for CometSupport: "
                + getCometSupport().getClass().getName() + " running under container "
                + cometSupport.getContainerName());
    }

    /**
     * Auto detect instance of {@link AtmosphereHandler} in case META-INF/atmosphere.xml
     * is missing.
     *
     * @param sc {@link ServletContext}
     * @param c  {@link URLClassLoader} to load the class.
     * @throws java.net.MalformedURLException
     * @throws java.net.URISyntaxException
     */
    protected void autoDetectAtmosphereHandlers(ServletContext sc, URLClassLoader c)
            throws MalformedURLException, URISyntaxException {
        String s = sc.getRealPath(WEB_INF);

        // Weblogic bug
        if (s == null) {
            URL u = sc.getResource(WEB_INF);
            if (u == null) return;
            s = u.getPath();
        }

        File f = new File(s);
        if (f.isDirectory()) {
            getFiles(f);
            for (String className : possibleAtmosphereHandlersCandidate) {
                try {
                    className = className.replace('\\', '/');
                    className = className.substring(className.indexOf(WEB_INF)
                            + WEB_INF.length(), className.lastIndexOf(".")).replace('/', '.');
                    Class<?> clazz = c.loadClass(className);
                    if (AtmosphereHandler.class.isAssignableFrom(clazz)) {
                        AtmosphereHandler g = (AtmosphereHandler) clazz.newInstance();

                        logger.info("Successfully loaded " + g
                                + " mapped to context-path " + g.getClass().getSimpleName());
                        atmosphereHandlers.put("/" + g.getClass().getSimpleName(),
                                new AtmosphereHandlerWrapper(g, null));
                    }
                } catch (Throwable t) {
                    logger.finest(className + " is not a AtmosphereHandler");
                }
            }
        }
        logger.info("Atmosphere using Broadcaster " + broadcasterClassName);
    }

    /**
     * Get the list of possible candidate to load as {@link AtmosphereHandler}
     *
     * @param f the real path {@link File}
     */
    protected void getFiles(File f) {
        File[] files = f.listFiles();
        for (File test : files) {
            if (test.isDirectory()) {
                getFiles(test);
            } else {
                String clazz = test.getAbsolutePath();
                if (clazz.endsWith(".class")) {
                    possibleAtmosphereHandlersCandidate.add(clazz);
                }
            }
        }
    }

    /**
     * Delegate the request processing to an instance of {@link CometSupport}
     *
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doHead(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of {@link CometSupport}
     *
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doOptions(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of {@link CometSupport}
     *
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doTrace(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of {@link CometSupport}
     *
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of {@link CometSupport}
     *
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of {@link CometSupport}
     *
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of {@link CometSupport}
     *
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        doCometSupport(req, res);
    }

    /**
     * Invoke the proprietary {@link CometSupport}
     *
     * @param req
     * @param res
     * @return an {@link Action}
     * @throws IOException
     * @throws ServletException
     */
    protected Action doCometSupport(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        req.setAttribute(BROADCASTER_FACTORY, broadcasterFactory);
        req.setAttribute(PROPERTY_USE_STREAM, useStreamForFlushingComments);
        try {
            return cometSupport.service(req, res);
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Tomcat failed")) {
                if (!isFilter) {
                    logger.warning(ex.getMessage());
                    logger.warning("Using the BlockingIOCometSupport.");
                }
                cometSupport = new BlockingIOCometSupport(config);
                service(req, res);
            } else {
                logger.log(Level.SEVERE, "AtmosphereServlet exception", ex);
                throw ex;
            }
        }
        return null;
    }

    /**
     * Hack to support Tomcat AIO like other WebServer. This method is invoked
     * by Tomcat when it detect a {@link Servlet} implements the interface
     * {@link CometProcessor} without invoking {@link Servlet#service}
     *
     * @param cometEvent the {@link CometEvent}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    public void event(CometEvent cometEvent) throws IOException, ServletException {
        HttpServletRequest req = cometEvent.getHttpServletRequest();
        HttpServletResponse res = cometEvent.getHttpServletResponse();
        req.setAttribute(TomcatCometSupport.COMET_EVENT, cometEvent);

        if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
            synchronized (cometSupport) {
                if (!cometSupport.getClass().equals(TomcatCometSupport.class)) {
                    logger.warning("TomcatCometSupport is enabled, switching to it");
                    cometSupport = new TomcatCometSupport(config);
                }
            }
        }

        doCometSupport(req, res);
    }

    /**
     * Hack to support JBossWeb AIO like other WebServer. This method is invoked
     * by Tomcat when it detect a {@link Servlet} implements the interface
     * {@link HttpEventServlet} without invoking {@link Servlet#service}
     *
     * @param httpEvent the {@link CometEvent}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    public void event(HttpEvent httpEvent) throws IOException, ServletException {
        HttpServletRequest req = httpEvent.getHttpServletRequest();
        HttpServletResponse res = httpEvent.getHttpServletResponse();
        req.setAttribute(JBossWebCometSupport.HTTP_EVENT, httpEvent);
        if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
            synchronized (cometSupport) {
                if (!cometSupport.getClass().equals(JBossWebCometSupport.class)) {
                    logger.warning("JBossWebCometSupport is enabled, switching to it");
                    cometSupport = new JBossWebCometSupport(config);
                }
            }
        }
        doCometSupport(req, res);
    }

    /**
     * Weblogic specific comet based implementation.
     *
     * @param rrk
     * @return true if suspended
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    protected boolean doRequest(RequestResponseKey rrk) throws IOException, ServletException {
        try {
            rrk.getRequest().getSession().setAttribute(WebLogicCometSupport.RRK, rrk);
            Action action = doCometSupport(rrk.getRequest(), rrk.getResponse());
            if (action.type == Action.TYPE.SUSPEND) {
                if (action.timeout == -1) {
                    rrk.setTimeout(Integer.MAX_VALUE);
                } else {
                    rrk.setTimeout((int) action.timeout);
                }
            }
            return action.type == Action.TYPE.SUSPEND;
        } catch (IllegalStateException ex) {
            logger.log(Level.SEVERE, "AtmosphereServlet.doRequest exception", ex);
            throw ex;
        }
    }

    /**
     * Weblogic specific comet based implementation.
     *
     * @param rrk
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    protected void doResponse(RequestResponseKey rrk, Object context)
            throws IOException, ServletException {
        rrk.getResponse().flushBuffer();
    }

    /**
     * Weblogic specific comet based implementation.
     *
     * @param rrk
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    protected void doTimeout(RequestResponseKey rrk) throws IOException, ServletException {
        ((AsynchronousProcessor) cometSupport).timedout(rrk.getRequest(), rrk.getResponse());
    }

    /**
     * Return the default {@link Broadcaster} class name.
     *
     * @return the broadcasterClassName
     */
    public static String getDefaultBroadcasterClassName() {
        return broadcasterClassName;
    }

    /**
     * Set the default {@link Broadcaster} class name
     *
     * @param broadcasterClassName the broadcasterClassName to set
     */
    public static void setDefaultBroadcasterClassName(String broadcasterClassName) {
        broadcasterClassName = broadcasterClassName;
    }

    /**
     * <tt>true</tt> if Atmosphere uses {@link HttpServletResponse#getOutputStream()}
     * by default for write operation.
     *
     * @return the useStreamForFlushingComments
     */
    public boolean isUseStreamForFlushingComments() {
        return useStreamForFlushingComments;
    }

    /**
     * Set to <tt>true</tt> so Atmosphere uses {@link HttpServletResponse#getOutputStream()}
     * by default for write operation. Default is false.
     *
     * @param useStreamForFlushingComments the useStreamForFlushingComments to set
     */
    public void setUseStreamForFlushingComments(boolean useStreamForFlushingComments) {
        this.useStreamForFlushingComments = useStreamForFlushingComments;
    }

    /**
     * Get the {@link BroadcasterFactory} which is used by Atmosphere to construct
     * {@link Broadcaster}
     *
     * @return {@link BroadcasterFactory}
     */
    public BroadcasterFactory getBroadcasterFactory() {
        return broadcasterFactory;
    }

    /**
     * Set the {@link BroadcasterFactory} which is used by Atmosphere to construct
     * {@link Broadcaster}
     *
     * @return {@link BroadcasterFactory}
     */
    public AtmosphereServlet setBroadcasterFactory(final BroadcasterFactory broadcasterFactory) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this.broadcasterFactory = broadcasterFactory;
        configureBroadcaster();
        return this;
    }

    /**
     * Return the {@link BroadcasterConfig} used by this instance.
     *
     * @return {@link BroadcasterConfig}
     */
    public static BroadcasterConfig getBroadcasterConfig() {
        return broadcasterConfig;
    }

    /**
     * Return the {@link org.atmosphere.cpr.BroadcasterCache} class name.
     *
     * @return the {@link org.atmosphere.cpr.BroadcasterCache} class name.
     */
    public String getBroadcasterCacheClassName() {
        return broadcasterCacheClassName;
    }

    /**
     * Set the {@link org.atmosphere.cpr.BroadcasterCache} class name.
     *
     * @param broadcasterCacheClassName
     */
    public void setBroadcasterCacheClassName(String broadcasterCacheClassName) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this.broadcasterCacheClassName = broadcasterCacheClassName;
        configureBroadcaster();
    }

    /**
     * Jetty 7 and up WebSocket support.
     *
     * @param request
     * @param protocol
     * @return a {@link WebSocket}}
     */
    @Override
    protected WebSocket doWebSocketConnect(final HttpServletRequest request, final String protocol) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("WebSocket upgrade requested");
        }
        return new WebSocket() {
            private WebSocketProcessor webSocketProcessor;

            public void onConnect(WebSocket.Outbound outbound) {
                webSocketProcessor = new WebSocketProcessor(AtmosphereServlet.this, new JettyWebSocketSupport(outbound));
                try {
                    webSocketProcessor.connect(request);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "", e);
                }
            }

            public void onMessage(byte frame, String data) {
                webSocketProcessor.broadcast(frame, data);
            }

            public void onMessage(byte frame, byte[] data, int offset, int length) {
                webSocketProcessor.broadcast(frame, new String(data, offset, length));
            }

            public void onDisconnect() {
                webSocketProcessor.close();
            }
        };
    }

}
