/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.rest;

import com.google.common.eventbus.EventBus;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamAssignmentService;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@Api(value = "Pipeline/Streams", description = "Stream assignment of processing pipelines")
@Path("/system/pipelines/streams")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PipelineStreamResource extends RestResource implements PluginRestResource {

    private final PipelineStreamAssignmentService assignmentService;
    private final PipelineService pipelineService;
    private final StreamService streamService;
    private final EventBus clusterBus;

    @Inject
    public PipelineStreamResource(PipelineStreamAssignmentService assignmentService,
                                  PipelineService pipelineService,
                                  StreamService streamService,
                                  @ClusterEventBus EventBus clusterBus) {
        this.assignmentService = assignmentService;
        this.pipelineService = pipelineService;
        this.streamService = streamService;
        this.clusterBus = clusterBus;
    }

    @ApiOperation(value = "Attach a processing pipeline to a stream", notes = "")
    @POST
    public PipelineStreamAssignment assignPipelines(@ApiParam(name = "Json body", required = true) @NotNull PipelineStreamAssignment assignment) throws NotFoundException {
        final String streamId = assignment.streamId();
        // the default stream doesn't exist as an entity
        if (!streamId.equalsIgnoreCase("default")) {
            streamService.load(streamId);
        }
        // verify the pipelines exist
        for (String s : assignment.pipelineIds()) {
            pipelineService.load(s);
        }
        final PipelineStreamAssignment save = assignmentService.save(assignment);
        clusterBus.post(save);
        return save;
    }

    @ApiOperation("Get pipeline attachments for the given stream")
    @GET
    @Path("/{streamId}")
    public PipelineStreamAssignment getPipelinesForStream(@ApiParam(name = "streamId") @PathParam("streamId") String streamId) throws NotFoundException {
        return assignmentService.load(streamId);
    }

    @ApiOperation("Get all pipeline attachments")
    @GET
    public Set<PipelineStreamAssignment> getAllAttachments() throws NotFoundException {
        return assignmentService.loadAll();
    }

}
