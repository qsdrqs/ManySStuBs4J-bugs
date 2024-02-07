/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.exec.spark;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.Context;
import org.apache.hadoop.hive.ql.DriverContext;
import org.apache.hadoop.hive.ql.exec.Operator;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.exec.mr.ExecMapper;
import org.apache.hadoop.hive.ql.io.HiveInputFormat;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.plan.MapWork;
import org.apache.hadoop.hive.ql.plan.OperatorDesc;
import org.apache.hadoop.hive.ql.plan.ReduceWork;
import org.apache.hadoop.hive.ql.plan.SparkWork;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.spark.HashPartitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class SparkClient implements Serializable {
  private static final long serialVersionUID = 1L;

  private static String masterUrl = "local";

  private static String appName = "Hive-Spark";

  private static String sparkHome = "/home/xzhang/apache/spark";
  
  private static int reducerCount = 5;
  
  private static String execMem = "1g";
  private static String execJvmOpts = "";

  static {
    String envSparkHome = System.getenv("SPARK_HOME");
    if (envSparkHome != null) {
      sparkHome = envSparkHome;
    }

    String envMaster = System.getenv("MASTER");
    if (envMaster != null) {
      masterUrl = envMaster;
    }

    String reducers = System.getenv("REDUCERS");
    if (reducers != null) {
      reducerCount = Integer.valueOf(reducers);
    }

    String mem = System.getenv("spark_executor_memory");
    if (mem != null) {
      execMem = mem;
    }

    String jopts = System.getenv("spark_executor_extraJavaOptions");
    if (jopts != null) {
      execJvmOpts = jopts;
    }

  }
  
  private static SparkClient client = new SparkClient();
  
  public static SparkClient getInstance() {
    return client;
  }
  
  private JavaSparkContext sc;

  private SparkClient() {
    SparkConf sparkConf = new SparkConf().setAppName(appName).setMaster(masterUrl).setSparkHome(sparkHome);
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
    sparkConf.set("spark.default.parallelism", "1");
    sparkConf.set("spark.executor.memory", execMem);
    sparkConf.set("spark.executor.extraJavaOptions", execJvmOpts);
    sc = new JavaSparkContext(sparkConf);
    addJars();
  }

  public int execute(DriverContext driverContext, SparkWork sparkWork) {
    int rc = 1;
//    System.out.println("classpath=\n"+System.getProperty("java.class.path") + "\n");
    MapWork mapWork = sparkWork.getMapWork();
    ReduceWork redWork = sparkWork.getReduceWork();
    
    Configuration hiveConf = driverContext.getCtx().getConf();
    // TODO: need to combine spark conf and hive conf
    JobConf jobConf = new JobConf(hiveConf);

    Context ctx = driverContext.getCtx();
    Path emptyScratchDir;
    try {
      if (ctx == null) {
        ctx = new Context(jobConf);
      }

      emptyScratchDir = ctx.getMRTmpPath();
      FileSystem fs = emptyScratchDir.getFileSystem(jobConf);
      fs.mkdirs(emptyScratchDir);
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Error launching map-reduce job" + "\n"
        + org.apache.hadoop.util.StringUtils.stringifyException(e));
      return 5;
    }

    List<Path> inputPaths;
    try {
      inputPaths = Utilities.getInputPaths(jobConf, mapWork, emptyScratchDir, ctx);
    } catch (Exception e2) {
      e2.printStackTrace();
      return -1;
    }
    Utilities.setInputPaths(jobConf, inputPaths);
    Utilities.setMapWork(jobConf, mapWork, emptyScratchDir, true);
    if (redWork != null)
      Utilities.setReduceWork(jobConf, redWork, emptyScratchDir, true);

    try {
      Utilities.createTmpDirs(jobConf, mapWork);
      Utilities.createTmpDirs(jobConf, redWork);
    } catch (IOException e1) {
      e1.printStackTrace();
    }

    try {
      Path planPath = new Path(jobConf.getWorkingDirectory(), "plan.xml");
      System.out.println("Serializing plan to path: " + planPath);
      OutputStream os2 = planPath.getFileSystem(jobConf).create(planPath);
      Utilities.serializePlan(mapWork, os2, jobConf);
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      return 1;
    }
    
    JavaPairRDD rdd = createRDD(sc, jobConf, mapWork);
    byte[] confBytes = KryoSerializer.serializeJobConf(jobConf);
    HiveMapFunction mf = new HiveMapFunction(confBytes);
    JavaPairRDD rdd2 = rdd.mapPartitionsToPair(mf);
    if (redWork == null) {
      rdd2.foreach(HiveVoidFunction.getInstance());
      if (mapWork.getAliasToWork() != null) {
        for (Operator<? extends OperatorDesc> op : mapWork.getAliasToWork().values()) {
          try {
            op.jobClose(jobConf, true);
          } catch (HiveException e) {
            System.out.println("Calling jobClose() failed: " + e);
            e.printStackTrace();
          }
        }
      }
    } else {
      JavaPairRDD rdd3 = rdd2.partitionBy(new HashPartitioner(reducerCount/*redWork.getNumReduceTasks()*/)); // Two partitions.
      HiveReduceFunction rf = new HiveReduceFunction(confBytes);
      JavaPairRDD rdd4 = rdd3.mapPartitionsToPair(rf);
      rdd4.foreach(HiveVoidFunction.getInstance());
      try {
        redWork.getReducer().jobClose(jobConf, true);
      } catch (HiveException e) {
        System.out.println("Calling jobClose() failed: " + e);
        e.printStackTrace();
      }
    }
    
    return 0;
  }
  
  private JavaPairRDD createRDD(JavaSparkContext sc, JobConf jobConf, MapWork mapWork) {
    Class ifClass = HiveInputFormat.class;

    // The mapper class is expected by the HiveInputFormat.
    jobConf.set("mapred.mapper.class", ExecMapper.class.getName());
    return sc.hadoopRDD(jobConf, ifClass, WritableComparable.class, Writable.class);
  }

  private void addJars() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();

    System.out.println("-----------------------------------------------------");
    URL[] urls = ((URLClassLoader)cl).getURLs();

    for(URL url: urls){
      java.io.File file = new java.io.File(url.getFile());
      if (file.exists() && file.isFile()) {
        if (file.getName().contains("guava")) {
          System.out.println("** skipping guava jar **: " + url.getFile());
        } else {
          System.out.println("adding jar: " + url.getFile());
          sc.addJar(url.getFile());
        }
      }
    }

    System.out.println("---------------------------------------------- ------");
  }

}
