/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package lib;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ning.http.client.*;
import lib.security.Graylog2ServerUnavailableException;
import models.*;
import models.api.requests.ApiRequest;
import models.api.responses.EmptyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.F;
import play.mvc.Http;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
class ApiClientImpl implements ApiClient {
    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    private AsyncHttpClient client;
    private final ServerNodes serverNodes;
    private Thread shutdownHook;

    @Inject
    private ApiClientImpl(ServerNodes serverNodes) {
        this.serverNodes = serverNodes;
    }

    @Override
    public void start() {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnection(false);
        client = new AsyncHttpClient(builder.build());

        shutdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                client.close();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    @Override
    public void stop() {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            // ignore race at shutdown.
        }
        client.close();
    }

    // default visibility for access from tests (overrides the effects of initialize())
    @Override
    public void setHttpClient(AsyncHttpClient client) {
        this.client = client;
    }

    @Override
    public <T> lib.ApiRequestBuilder<T> get(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.GET, responseClass);
    }

    @Override
    public <T> lib.ApiRequestBuilder<T> post(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.POST, responseClass);
    }

    @Override
    public lib.ApiRequestBuilder<EmptyResponse> post() {
        return post(EmptyResponse.class);
    }

    @Override
    public <T> lib.ApiRequestBuilder<T> put(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.PUT, responseClass);
    }

    @Override
    public lib.ApiRequestBuilder<EmptyResponse> put() {
        return put(EmptyResponse.class);
    }

    @Override
    public <T> lib.ApiRequestBuilder<T> delete(Class<T> responseClass) {
        return new ApiRequestBuilder<>(Method.DELETE, responseClass);
    }

    @Override
    public lib.ApiRequestBuilder<EmptyResponse> delete() {
        return delete(EmptyResponse.class);
    }

    private static void applyBasicAuthentication(AsyncHttpClient.BoundRequestBuilder requestBuilder, String userInfo) {
        if (userInfo != null) {
            final String[] userPass = userInfo.split(":", 2);
            if (userPass[0] != null && userPass[1] != null) {
                requestBuilder.setRealm(new Realm.RealmBuilder()
                        .setPrincipal(userPass[0])
                        .setPassword(userPass[1])
                        .setUsePreemptiveAuth(true)
                        .setScheme(Realm.AuthScheme.BASIC)
                        .build());
            }
        }
    }

    private static <T> T deserializeJson(Response response, Class<T> responseClass) throws IOException {
        final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
        return gson.fromJson(response.getResponseBody("UTF-8"), responseClass);
    }


    public class ApiRequestBuilder<T> implements lib.ApiRequestBuilder<T> {
        private String pathTemplate;
        private Node node;
        private Radio radio;
        private Collection<Node> nodes;
        private final Method method;
        private ApiRequest body;
        private final Class<T> responseClass;
        private final ArrayList<Object> pathParams = Lists.newArrayList();
        private final ArrayList<F.Tuple<String, String>> queryParams = Lists.newArrayList();
        private Set<Integer> expectedResponseCodes = Sets.newHashSet();
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;
        private int timeoutValue = 5;
        private boolean unauthenticated = false;
        private MediaType mediaType = MediaType.JSON_UTF_8;
        private String sessionId;

        public ApiRequestBuilder(Method method, Class<T> responseClass) {
            this.method = method;
            this.responseClass = responseClass;
        }

        @Override
        public ApiRequestBuilder<T> path(String pathTemplate) {
            this.pathTemplate = pathTemplate;
            return this;
        }

        // convenience
        @Override
        public lib.ApiRequestBuilder<T> path(String pathTemplate, Object... params) {
            path(pathTemplate);
            pathParams(params);
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> pathParams(Object... params) {
            Collections.addAll(pathParams, params);
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> pathParam(Object param) {
            return pathParams(param);
        }

        @Override
        public ApiRequestBuilder<T> node(Node node) {
            this.node = node;
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> radio(Radio radio) {
            this.radio = radio;
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> clusterEntity(ClusterEntity entity) {
            if (entity instanceof Radio) {
                this.radio = (Radio) entity;
            } else if (entity instanceof Node) {
                this.node = (Node) entity;
            } else {
                log.warn("You passed a ClusterEntity that is not of type Node or Radio. Selected nothing.");
            }
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> nodes(Node... nodes) {
            if (this.nodes != null) {
                // TODO makes this sane
                throw new IllegalStateException();
            }
            this.nodes = Lists.newArrayList(nodes);
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> nodes(Collection<Node> nodes) {
            if (this.nodes != null) {
                // TODO makes this sane
                throw new IllegalStateException();
            }
            this.nodes = Lists.newArrayList(nodes);
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> fromAllNodes() {
            this.nodes = serverNodes.all();
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> onlyMasterNode() {
            this.node = serverNodes.master();
            return this;
        }

        @Override
        public ApiRequestBuilder<T> queryParam(String name, String value) {
            queryParams.add(F.Tuple(name, value));
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> queryParam(String name, int value) {
            return queryParam(name, Integer.toString(value));
        }

        @Override
        public lib.ApiRequestBuilder<T> queryParams(Map<String, String> params) {
            for (Map.Entry<String, String> p : params.entrySet()) {
                queryParam(p.getKey(), p.getValue());
            }

            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> session(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> unauthenticated() {
            this.unauthenticated = true;
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> body(ApiRequest body) {
            this.body = body;
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> expect(int... httpStatusCodes) {
            for (int code : httpStatusCodes) {
                this.expectedResponseCodes.add(code);
            }

            return this;
        }

        @Override
        public ApiRequestBuilder<T> timeout(int value, TimeUnit unit) {
            this.timeoutValue = value;
            this.timeoutUnit = unit;
            return this;
        }

        @Override
        public lib.ApiRequestBuilder<T> accept(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        @Override
        public T execute() throws APIException, IOException {
            if (radio != null && (node != null || nodes != null)) {
                throw new RuntimeException("You set both and a Node and a Radio as target. This is not possible.");
            }

            final ClusterEntity target;

            if (radio == null) {
                if (node == null) {
                    if (nodes != null) {
                        log.error("Multiple nodes are set, but execute() was called. This is most likely a bug and you meant to call executeOnAll()!");
                    }
                    node(serverNodes.any());
                }

                target = node;
            } else {
                target = radio;
            }

            if (!unauthenticated && sessionId == null) {
                final User user = UserService.current();
                if (user != null) {
                    session(user.getSessionId());
                } else {
                    log.warn("You did not add unauthenticated() nor session() but also don't have a current user. You probably meant unauthenticated(). This is a bug!");
                }
            }
            final URL url = prepareUrl(target);
            final AsyncHttpClient.BoundRequestBuilder requestBuilder = requestBuilderForUrl(url);
            requestBuilder.addHeader(Http.HeaderNames.ACCEPT, mediaType.toString());

            final Request request = requestBuilder.build();
            if (log.isDebugEnabled()) {
                log.debug("API Request: {}", request.toString());
            }

            // Set 200 OK as standard if not defined.
            if (expectedResponseCodes.isEmpty()) {
                expectedResponseCodes.add(Http.Status.OK);
            }

            try {
                // TODO implement streaming responses
                Response response = requestBuilder.execute().get(timeoutValue, timeoutUnit);

                target.touch();

                // TODO this is wrong, shouldn't it accept some callback instead of throwing an exception?
                if (!expectedResponseCodes.contains(response.getStatusCode())) {
                    throw new APIException(request, response);
                }

                // TODO: once we switch to jackson we can take the media type into account automatically
                final MediaType responseContentType;
                if (response.getContentType() == null) {
                    responseContentType = MediaType.JSON_UTF_8;
                } else {
                    responseContentType = MediaType.parse(response.getContentType());
                }

                if (!responseContentType.is(mediaType.withoutParameters())) {
                    log.warn("We said we'd accept {} but got {} back, let's see how that's going to work out...", mediaType, responseContentType);
                }
                if (responseClass.equals(String.class)) {
                    return responseClass.cast(response.getResponseBody("UTF-8"));
                }

                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    T result;
                    try {
                        if (responseContentType.is(MediaType.JSON_UTF_8.withoutParameters())) {
                            result = deserializeJson(response, responseClass);
                        } else {
                            log.error("Don't know how to deserialize objects with content in {}, expected {}, failing.", responseContentType, mediaType);
                            throw new APIException(request, response);
                        }

                        return result;
                    } catch (Exception e) {
                        log.error("Caught Exception while deserializing JSON request: " + e);
                        log.debug("Response from backend was: " + response.getResponseBody("UTF-8"));

                        throw new APIException(request, response);
                    }
                } else {
                    return null;
                }
            } catch (InterruptedException e) {
                // TODO
                target.markFailure();
            } catch (MalformedURLException e) {
                log.error("Malformed URL", e);
                throw new RuntimeException("Malformed URL.", e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ConnectException) {
                    log.warn("Graylog2 server unavailable. Connection refused.");
                    target.markFailure();
                    throw new Graylog2ServerUnavailableException(e);
                }
                log.error("REST call failed", e.getCause());
                throw new APIException(request, e);
            } catch (IOException e) {
                // TODO
                log.error("unhandled IOException", e);
                target.markFailure();
                throw e;
            } catch (TimeoutException e) {
                log.warn("Timed out requesting {}", request);
                target.markFailure();
            }
            // TODO should this throw an exception instead?
            return null;
        }

        @Override
        public Map<Node, T> executeOnAll() {
            HashMap<Node, T> results = Maps.newHashMap();
            if (node == null && nodes == null) {
                nodes = serverNodes.all();
            }

            Collection<F.Tuple<ListenableFuture<Response>, Node>> requests = Lists.newArrayList();
            final Collection<Response> responses = Lists.newArrayList();
            for (Node currentNode : nodes) {
                final URL url = prepareUrl(currentNode);
                try {
                    final AsyncHttpClient.BoundRequestBuilder requestBuilder = requestBuilderForUrl(url);
                    requestBuilder.addHeader(Http.HeaderNames.ACCEPT, mediaType.toString());
                    if (log.isDebugEnabled()) {
                        log.debug("API Request: {}", requestBuilder.build().toString());
                    }
                    final ListenableFuture<Response> future = requestBuilder.execute(new AsyncCompletionHandler<Response>() {
                        @Override
                        public Response onCompleted(Response response) throws Exception {
                            responses.add(response);
                            return response;
                        }
                    });
                    requests.add(new F.Tuple<>(future, currentNode));
                } catch (IOException e) {
                    log.error("Cannot execute request", e);
                    currentNode.markFailure();
                }
            }
            for (F.Tuple<ListenableFuture<Response>, Node> requestAndNode : requests) {
                final ListenableFuture<Response> request = requestAndNode._1;
                final Node node = requestAndNode._2;
                try {
                    final Response response = request.get(timeoutValue, timeoutUnit);
                    node.touch();
                    results.put(node, deserializeJson(response, responseClass));
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    node.markFailure();
                } catch (ExecutionException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    node.markFailure();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    node.markFailure();
                } catch (TimeoutException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    node.markFailure();
                }
            }


            return results;
        }

        private AsyncHttpClient.BoundRequestBuilder requestBuilderForUrl(URL url) {
            // *sigh* the generic requestBuilder methods are protected/private making this verbose :(
            final AsyncHttpClient.BoundRequestBuilder requestBuilder;
            final String userInfo = url.getUserInfo();
            // have to hack around here, because the userInfo will unescape the @ in usernames :(
            try {
                url = UriBuilder.fromUri(url.toURI()).userInfo(null).build().toURL();
            } catch (URISyntaxException | MalformedURLException ignore) {
                // cannot happen, because it was a valid url before
            }

            switch (method) {
                case GET:
                    requestBuilder = client.prepareGet(url.toString());
                    break;
                case POST:
                    requestBuilder = client.preparePost(url.toString());
                    break;
                case PUT:
                    requestBuilder = client.preparePut(url.toString());
                    break;
                case DELETE:
                    requestBuilder = client.prepareDelete(url.toString());
                    break;
                default:
                    throw new IllegalStateException("Illegal method " + method.toString());
            }

            applyBasicAuthentication(requestBuilder, userInfo);
            requestBuilder.setPerRequestConfig(new PerRequestConfig(null, (int) timeoutUnit.toMillis(timeoutValue)));

            if (body != null) {
                if (method != Method.PUT && method != Method.POST) {
                    throw new IllegalArgumentException("Cannot set request body on non-PUT or POST requests.");
                }
                requestBuilder.addHeader("Content-Type", "application/json; charset=utf-8");
                requestBuilder.setBodyEncoding("UTF-8");
                requestBuilder.setBody(body.toJson());
            } else if (method == Method.POST) {
                log.warn("POST without body, this doesn't make sense,", new IllegalStateException());
            }
            // TODO: should we always insist on things being wrapped in json?
            if (!responseClass.equals(String.class)) {
                requestBuilder.addHeader("Accept", "application/json");
            }
            requestBuilder.addHeader("Accept-Charset", "utf-8");
            return requestBuilder;
        }

        // default visibility for tests
        public URL prepareUrl(ClusterEntity target) {
            // if this is null there's not much we can do anyway...
            Preconditions.checkNotNull(pathTemplate, "path() needs to be set to a non-null value.");

            URI builtUrl;
            try {
                String path = MessageFormat.format(pathTemplate, pathParams.toArray());
                final UriBuilder uriBuilder = UriBuilder.fromUri(target.getTransportAddress());
                uriBuilder.path(path);
                for (F.Tuple<String, String> queryParam : queryParams) {
                    uriBuilder.queryParam(queryParam._1, queryParam._2);
                }

                if (unauthenticated && sessionId != null) {
                    log.error("Both session() and unauthenticated() are set for this request, this is a bug, using session id.");
                }
                if (sessionId != null) {
                    // pass the current session id via basic auth and special "password"
                    uriBuilder.userInfo(sessionId + ":session");
                }
                builtUrl = uriBuilder.build();
                return builtUrl.toURL();
            } catch (MalformedURLException e) {
                // TODO handle this properly
                log.error("Could not build target URL", e);
                throw new RuntimeException(e);
            }
        }
    }
}
