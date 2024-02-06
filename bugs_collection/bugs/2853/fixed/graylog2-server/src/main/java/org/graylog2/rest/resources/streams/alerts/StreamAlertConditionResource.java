package org.graylog2.rest.resources.streams.alerts;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.AlertService;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.streams.alerts.requests.CreateConditionRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@RequiresAuthentication
@Api(value = "AlertConditions", description = "Manage stream alert conditions")
@Path("/streams/{streamId}/alerts/conditions")
public class StreamAlertConditionResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(StreamAlertConditionResource.class);

    private final StreamService streamService;
    private final AlertService alertService;

    @Inject
    public StreamAlertConditionResource(StreamService streamService, AlertService alertService) {
        this.streamService = streamService;
        this.alertService = alertService;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create an alert condition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response create(@ApiParam(title = "streamId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
                           @ApiParam(title = "JSON body", required = true) String body) {
        CreateConditionRequest ccr;
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        try {
            ccr = objectMapper.readValue(body, CreateConditionRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        final AlertCondition alertCondition;
        try {
            alertCondition = alertService.fromRequest(ccr, stream);
        } catch (AbstractAlertCondition.NoSuchAlertConditionTypeException e) {
            LOG.error("Invalid alarm condition type.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        try {
            streamService.addAlertCondition(stream, alertCondition);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("alert_condition_id", alertCondition.getId());

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get all alert conditions of this stream")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response list(@ApiParam(title = "streamId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        List<Map<String, Object>> conditions = Lists.newArrayList();
        for (AlertCondition alertCondition : streamService.getAlertConditions(stream)) {
            conditions.add(alertService.asMap(alertCondition));
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("conditions", conditions);
        result.put("total", conditions.size());

        return Response.status(Response.Status.OK).entity(json(result)).build();
    }

    @DELETE @Timed
    @Path("{conditionId}")
    @ApiOperation(value = "Delete an alert condition")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response delete(@ApiParam(title = "streamId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
                         @ApiParam(title = "conditionId", description = "The stream id this new alert condition belongs to.", required = true) @PathParam("conditionId") String conditionId) {
        checkPermission(RestPermissions.STREAMS_READ, streamid);

        Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        streamService.removeAlertCondition(stream, conditionId);

        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
