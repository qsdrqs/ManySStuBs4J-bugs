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
package controllers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.*;
import models.api.results.MessageAnalyzeResult;
import models.api.results.MessageResult;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static lib.security.RestPermissions.INPUTS_READ;
import static lib.security.RestPermissions.STREAMS_READ;
import static views.helpers.Permissions.isPermitted;

public class MessagesController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;

    @Inject
    private StreamService streamService;

    @Inject
    private MessagesService messagesService;

    public Result show(String index, String id) {
        try {
            MessageResult message = messagesService.getMessage(index, id);
            Node sourceNode = getSourceNode(message);
            Radio sourceRadio = getSourceRadio(message);

            List<Stream> messageInStreams = Lists.newArrayList();

            for (String streamId : message.getStreamIds()) {
                if (isPermitted(STREAMS_READ, streamId)) {
                    try {
                        messageInStreams.add(streamService.get(streamId));
                    } catch(APIException e) {
                        //  We get a 404 if the stream no longer exists.
                        Logger.debug("Skipping stream of message", e);
                        continue;
                    }
                }
            }

            return ok(views.html.messages.show.render(currentUser(), message, messageInStreams, getSourceInput(sourceNode, message), sourceNode, sourceRadio, getSourceInput(sourceRadio, message)));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not get message. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

	public Result partial(String index, String id) {
		try {
            MessageResult message = messagesService.getMessage(index, id);
            Node sourceNode = getSourceNode(message);
            Radio sourceRadio = getSourceRadio(message);
            List<Stream> messageInStreams = Lists.newArrayList();

            for (String streamId : message.getStreamIds()) {
                if (isPermitted(STREAMS_READ, streamId)) {
                    try {
                        messageInStreams.add(streamService.get(streamId));
                    } catch(APIException e) {
                        //  We get a 404 if the stream no longer exists.
                        Logger.debug("Skipping stream of message", e);
                        continue;
                    }
                }
            }

            return ok(views.html.messages.show_as_partial.render(
                    message,
                    messageInStreams,
                    getSourceInput(sourceNode, message),
                    sourceNode,
                    sourceRadio,
                    getSourceInput(sourceRadio, message),
                    streamService.all())
            );
		} catch (IOException e) {
			return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "Could not get message. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(500, views.html.errors.error.render(message, e, request()));
		}
	}

    // TODO move this to an API controller.
    public Result single(String index, String id) {
        return single(index, id, false);
    }

    public Result single(String index, String id, Boolean filtered) {
        try {

            MessageResult message = messagesService.getMessage(index, id);

            Map<String, Object> result = Maps.newHashMap();
            result.put("id", message.getId());
            if (filtered)
                result.put("fields", message.getFormattedFields());
            else
                result.put("fields", message.getFields());

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return status(500);
        } catch (APIException e) {
            return status(e.getHttpCode());
        }
    }

	public Result analyze(String index, String id, String field) {
		try {
			MessageResult message = messagesService.getMessage(index, id);
			
			String analyzeField = (String) message.getFilteredFields().get(field);
			if (analyzeField == null || analyzeField.isEmpty()) {
				return status(404, "Message does not have requested field " + field);
			}
			
			MessageAnalyzeResult result = messagesService.analyze(index, analyzeField);
			return ok(new Gson().toJson(result.getTokens())).as("application/json");
		} catch (IOException e) {
			return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(500, views.html.errors.error.render(message, e, request()));
		}
	}

    private Node getSourceNode(MessageResult m) {
        try {
            return nodeService.loadNode(m.getSourceNodeId());
        } catch(Exception e) {
            Logger.warn("Could not derive source node from message <" + m.getId() + ">.", e);
        }

        return null;
    }


    private Radio getSourceRadio(MessageResult m) {
        if (m.viaRadio()) {
            try {
                return nodeService.loadRadio(m.getSourceRadioId());
            } catch(Exception e) {
                Logger.warn("Could not derive source radio from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }

    private static Input getSourceInput(Node node, MessageResult m) {
        if (node != null && isPermitted(INPUTS_READ, m.getSourceInputId())) {
            try {
                return node.getInput(m.getSourceInputId());
            } catch(Exception e) {
                Logger.warn("Could not derive source input from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }

    private static Input getSourceInput(Radio radio, MessageResult m) {
        if (radio != null) {
            try {
                return radio.getInput(m.getSourceRadioInputId());
            } catch(Exception e) {
                Logger.warn("Could not derive source radio input from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }
	
}
