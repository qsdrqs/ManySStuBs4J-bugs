/*
 * Druid - a distributed column store.
 * Copyright (C) 2012, 2013  Metamarkets Group Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.druid.indexer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.metamx.common.logger.Logger;
import io.druid.timeline.DataSegment;

import java.util.List;

/**
 */
public class HadoopDruidIndexerJob implements Jobby
{
  private static final Logger log = new Logger(HadoopDruidIndexerJob.class);
  private final HadoopDruidIndexerConfig config;
  private final MetadataStorageUpdaterJob metadataStorageUpdaterJob;
  private IndexGeneratorJob indexJob;
  private volatile List<DataSegment> publishedSegments = null;

  @Inject
  public HadoopDruidIndexerJob(
      HadoopDruidIndexerConfig config,
      MetadataStorageUpdaterJobHandler handler
  )
  {
    config.verify();
    this.config = config;

    Preconditions.checkArgument(
        !config.isUpdaterJobSpecSet() || handler != null,
        "MetadataStorageUpdaterJobHandler must not be null if ioConfig.metadataUpdateSpec is specified in "
    );

    if (config.isUpdaterJobSpecSet()) {
      metadataStorageUpdaterJob = new MetadataStorageUpdaterJob(
          config,
          handler
      );
    } else {
      metadataStorageUpdaterJob = null;
    }
  }

  @Override
  public boolean run()
  {
    List<Jobby> jobs = Lists.newArrayList();
    JobHelper.ensurePaths(config);

    if (config.isPersistInHeap()) {
      indexJob = new IndexGeneratorJob(config);
    } else {
      indexJob = new LegacyIndexGeneratorJob(config);
    }
    jobs.add(indexJob);

    if (metadataStorageUpdaterJob != null) {
      jobs.add(metadataStorageUpdaterJob);
    } else {
      log.info("No updaterJobSpec set, not uploading to database");
    }

    jobs.add(
        new Jobby()
        {
          @Override
          public boolean run()
          {
            publishedSegments = IndexGeneratorJob.getPublishedSegments(config);
            return true;
          }
        }
    );


    JobHelper.runJobs(jobs, config);
    return true;
  }

  public List<DataSegment> getPublishedSegments()
  {
    if (publishedSegments == null) {
      throw new IllegalStateException("Job hasn't run yet. No segments have been published yet.");
    }
    return publishedSegments;
  }

  public IndexGeneratorJob.IndexGeneratorStats getIndexJobStats()
  {
    return indexJob.getJobStats();
  }
}
