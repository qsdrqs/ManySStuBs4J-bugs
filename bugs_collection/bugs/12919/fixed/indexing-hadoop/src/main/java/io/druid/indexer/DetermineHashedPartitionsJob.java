/*
 * Druid - a distributed column store.
 * Copyright (C) 2014  Metamarkets Group Inc.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Closeables;
import com.metamx.common.ISE;
import com.metamx.common.logger.Logger;
import io.druid.data.input.InputRow;
import io.druid.data.input.Rows;
import io.druid.granularity.QueryGranularity;
import io.druid.indexer.granularity.UniformGranularitySpec;
import io.druid.query.aggregation.hyperloglog.HyperLogLogCollector;
import io.druid.timeline.partition.HashBasedNumberedShardSpec;
import io.druid.timeline.partition.NoneShardSpec;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Interval;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Determines appropriate ShardSpecs for a job by determining approximate cardinality of data set using HyperLogLog
 */
public class DetermineHashedPartitionsJob implements Jobby
{
  private static final Logger log = new Logger(DetermineHashedPartitionsJob.class);
  private final HadoopDruidIndexerConfig config;

  public DetermineHashedPartitionsJob(
      HadoopDruidIndexerConfig config
  )
  {
    this.config = config;
  }

  public boolean run()
  {
    try {
      /*
       * Group by (timestamp, dimensions) so we can correctly count dimension values as they would appear
       * in the final segment.
       */
      long startTime = System.currentTimeMillis();
      final Job groupByJob = new Job(
          new Configuration(),
          String.format("%s-determine_partitions_hashed-%s", config.getDataSource(), config.getIntervals())
      );

      JobHelper.injectSystemProperties(groupByJob);
      groupByJob.setInputFormatClass(TextInputFormat.class);
      groupByJob.setMapperClass(DetermineCardinalityMapper.class);
      groupByJob.setMapOutputKeyClass(LongWritable.class);
      groupByJob.setMapOutputValueClass(BytesWritable.class);
      groupByJob.setReducerClass(DetermineCardinalityReducer.class);
      groupByJob.setOutputKeyClass(NullWritable.class);
      groupByJob.setOutputValueClass(NullWritable.class);
      groupByJob.setOutputFormatClass(SequenceFileOutputFormat.class);
      groupByJob.setPartitionerClass(DetermineHashedPartitionsPartitioner.class);
      if (!config.getSegmentGranularIntervals().isPresent()) {
        groupByJob.setNumReduceTasks(1);
      } else {
        groupByJob.setNumReduceTasks(config.getSegmentGranularIntervals().get().size());
      }
      JobHelper.setupClasspath(config, groupByJob);

      config.addInputPaths(groupByJob);
      config.intoConfiguration(groupByJob);
      FileOutputFormat.setOutputPath(groupByJob, config.makeGroupedDataDir());

      groupByJob.submit();
      log.info("Job %s submitted, status available at: %s", groupByJob.getJobName(), groupByJob.getTrackingURL());

      if (!groupByJob.waitForCompletion(true)) {
        log.error("Job failed: %s", groupByJob.getJobID());
        return false;
      }

      /*
       * Load partitions and intervals determined by the previous job.
       */

      log.info("Job completed, loading up partitions for intervals[%s].", config.getSegmentGranularIntervals());
      FileSystem fileSystem = null;
      if (!config.getSegmentGranularIntervals().isPresent()) {
        final Path intervalInfoPath = config.makeIntervalInfoPath();
        fileSystem = intervalInfoPath.getFileSystem(groupByJob.getConfiguration());
        List<Interval> intervals = config.jsonMapper.readValue(
            Utils.openInputStream(groupByJob, intervalInfoPath), new TypeReference<List<Interval>>()
        {
        }
        );
        config.setGranularitySpec(new UniformGranularitySpec(config.getGranularitySpec().getGranularity(), intervals));
        log.info("Determined Intervals for Job [%s]" + config.getSegmentGranularIntervals());
      }
      Map<DateTime, List<HadoopyShardSpec>> shardSpecs = Maps.newTreeMap(DateTimeComparator.getInstance());
      int shardCount = 0;
      for (Interval segmentGranularity : config.getSegmentGranularIntervals().get()) {
        DateTime bucket = segmentGranularity.getStart();

        final Path partitionInfoPath = config.makeSegmentPartitionInfoPath(segmentGranularity);
        if (fileSystem == null) {
          fileSystem = partitionInfoPath.getFileSystem(groupByJob.getConfiguration());
        }
        final Long cardinality = config.jsonMapper.readValue(
            Utils.openInputStream(groupByJob, partitionInfoPath), new TypeReference<Long>()
        {
        }
        );
        final int numberOfShards = (int) Math.ceil((double) cardinality / config.getTargetPartitionSize());

        List<HadoopyShardSpec> actualSpecs = Lists.newArrayListWithExpectedSize(numberOfShards);
        if (numberOfShards == 1) {
          actualSpecs.add(new HadoopyShardSpec(new NoneShardSpec(), shardCount++));
        } else {
          for (int i = 0; i < numberOfShards; ++i) {
            actualSpecs.add(new HadoopyShardSpec(new HashBasedNumberedShardSpec(i, numberOfShards), shardCount++));
            log.info("DateTime[%s], partition[%d], spec[%s]", bucket, i, actualSpecs.get(i));
          }
        }

        shardSpecs.put(bucket, actualSpecs);

      }
      config.setShardSpecs(shardSpecs);
      log.info(
          "DetermineHashedPartitionsJob took %d millis",
          (System.currentTimeMillis() - startTime)
      );

      return true;
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public static class DetermineCardinalityMapper extends HadoopDruidIndexerMapper<LongWritable, BytesWritable>
  {
    private static HashFunction hashFunction = Hashing.murmur3_128();
    private QueryGranularity rollupGranularity = null;
    private Map<Interval, HyperLogLogCollector> hyperLogLogs;
    private HadoopDruidIndexerConfig config;
    private boolean determineIntervals;

    @Override
    protected void setup(Context context)
        throws IOException, InterruptedException
    {
      super.setup(context);
      rollupGranularity = getConfig().getRollupSpec().getRollupGranularity();
      config = HadoopDruidIndexerConfigBuilder.fromConfiguration(context.getConfiguration());
      Optional<Set<Interval>> intervals = config.getSegmentGranularIntervals();
      if (intervals.isPresent()) {
        determineIntervals = false;
        final ImmutableMap.Builder<Interval, HyperLogLogCollector> builder = ImmutableMap.builder();
        for (final Interval bucketInterval : intervals.get()) {
          builder.put(bucketInterval, HyperLogLogCollector.makeLatestCollector());
        }
        hyperLogLogs = builder.build();
      } else {
        determineIntervals = true;
        hyperLogLogs = Maps.newHashMap();
      }
    }

    @Override
    protected void innerMap(
        InputRow inputRow,
        Text text,
        Context context
    ) throws IOException, InterruptedException
    {

      final List<Object> groupKey = Rows.toGroupKey(
          rollupGranularity.truncate(inputRow.getTimestampFromEpoch()),
          inputRow
      );
      Interval interval;
      if (determineIntervals) {
        interval = config.getGranularitySpec().getGranularity().bucket(new DateTime(inputRow.getTimestampFromEpoch()));

        if (!hyperLogLogs.containsKey(interval)) {
          hyperLogLogs.put(interval, HyperLogLogCollector.makeLatestCollector());
        }
      } else {
        final Optional<Interval> maybeInterval = config.getGranularitySpec()
                                                       .bucketInterval(new DateTime(inputRow.getTimestampFromEpoch()));

        if (!maybeInterval.isPresent()) {
          throw new ISE("WTF?! No bucket found for timestamp: %s", inputRow.getTimestampFromEpoch());
        }
        interval = maybeInterval.get();
      }
      hyperLogLogs.get(interval)
                  .add(
                      hashFunction.hashBytes(HadoopDruidIndexerConfig.jsonMapper.writeValueAsBytes(groupKey))
                                  .asBytes()
                  );
    }

    @Override
    public void run(Context context) throws IOException, InterruptedException
    {
      setup(context);

      while (context.nextKeyValue()) {
        map(context.getCurrentKey(), context.getCurrentValue(), context);
      }

      for (Map.Entry<Interval, HyperLogLogCollector> entry : hyperLogLogs.entrySet()) {
        context.write(
            new LongWritable(entry.getKey().getStartMillis()),
            new BytesWritable(entry.getValue().toByteArray())
        );
      }
      cleanup(context);
    }

  }

  public static class DetermineCardinalityReducer
      extends Reducer<LongWritable, BytesWritable, NullWritable, NullWritable>
  {
    private final List<Interval> intervals = Lists.newArrayList();
    protected HadoopDruidIndexerConfig config = null;

    @Override
    protected void setup(Context context)
        throws IOException, InterruptedException
    {
      config = HadoopDruidIndexerConfigBuilder.fromConfiguration(context.getConfiguration());
    }

    @Override
    protected void reduce(
        LongWritable key,
        Iterable<BytesWritable> values,
        Context context
    ) throws IOException, InterruptedException
    {
      HyperLogLogCollector aggregate = HyperLogLogCollector.makeLatestCollector();
      for (BytesWritable value : values) {
        aggregate.fold(ByteBuffer.wrap(value.getBytes(), 0, value.getLength()));
      }
      Interval interval = config.getGranularitySpec().getGranularity().bucket(new DateTime(key.get()));
      intervals.add(interval);
      final Path outPath = config.makeSegmentPartitionInfoPath(interval);
      final OutputStream out = Utils.makePathAndOutputStream(
          context, outPath, config.isOverwriteFiles()
      );

      try {
        HadoopDruidIndexerConfig.jsonMapper.writerWithType(
            new TypeReference<Long>()
            {
            }
        ).writeValue(
            out,
            new Double(aggregate.estimateCardinality()).longValue()
        );
      }
      finally {
        Closeables.close(out, false);
      }
    }

    @Override
    public void run(Context context)
        throws IOException, InterruptedException
    {
      super.run(context);
      if (!config.getSegmentGranularIntervals().isPresent()) {
        final Path outPath = config.makeIntervalInfoPath();
        final OutputStream out = Utils.makePathAndOutputStream(
            context, outPath, config.isOverwriteFiles()
        );

        try {
          HadoopDruidIndexerConfig.jsonMapper.writerWithType(
              new TypeReference<List<Interval>>()
              {
              }
          ).writeValue(
              out,
              intervals
          );
        }
        finally {
          Closeables.close(out, false);
        }
      }
    }
  }

  public static class DetermineHashedPartitionsPartitioner
      extends Partitioner<LongWritable, BytesWritable> implements Configurable
  {
    private Configuration config;
    private boolean determineIntervals;
    private Map<LongWritable, Integer> reducerLookup;

    @Override
    public int getPartition(LongWritable interval, BytesWritable text, int numPartitions)
    {

      if (config.get("mapred.job.tracker").equals("local") || determineIntervals) {
        return 0;
      } else {
        return reducerLookup.get(interval);
      }
    }

    @Override
    public Configuration getConf()
    {
      return config;
    }

    @Override
    public void setConf(Configuration config)
    {
      this.config = config;
      HadoopDruidIndexerConfig hadoopConfig = HadoopDruidIndexerConfigBuilder.fromConfiguration(config);
      if (hadoopConfig.getSegmentGranularIntervals().isPresent()) {
        determineIntervals = false;
        int reducerNumber = 0;
        ImmutableMap.Builder<LongWritable, Integer> builder = ImmutableMap.builder();
        for (Interval interval : hadoopConfig.getSegmentGranularIntervals().get()) {
          builder.put(new LongWritable(interval.getStartMillis()), reducerNumber++);
        }
        reducerLookup = builder.build();
      } else {
        determineIntervals = true;
      }
    }
  }

}


