/*
 * Copyright (C) 2013,2014 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari.pool;

import static com.zaxxer.hikari.pool.HikariMBeanElf.registerMBeans;
import static com.zaxxer.hikari.pool.HikariMBeanElf.unregisterMBeans;
import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_IN_USE;
import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_NOT_IN_USE;
import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_REMOVED;
import static com.zaxxer.hikari.util.UtilityElf.createThreadPoolExecutor;
import static com.zaxxer.hikari.util.UtilityElf.elapsedTimeMs;
import static com.zaxxer.hikari.util.UtilityElf.getTransactionIsolation;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.metrics.CodaHaleMetricsTracker;
import com.zaxxer.hikari.metrics.CodahaleHealthChecker;
import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTracker.MetricsContext;
import com.zaxxer.hikari.proxy.ConnectionProxy;
import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.proxy.ProxyFactory;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.DefaultThreadFactory;
import com.zaxxer.hikari.util.IBagStateListener;

/**
 * This is the primary connection pool class that provides the basic
 * pooling behavior for HikariCP.
 *
 * @author Brett Wooldridge
 */
public class HikariPool implements HikariPoolMBean, IBagStateListener
{
   protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
   private static final long ALIVE_BYPASS_WINDOW = Long.getLong("com.zaxxer.hikari.aliveBypassWindow", 1000L);

   protected static final int POOL_RUNNING = 0;
   protected static final int POOL_SUSPENDED = 1;
   protected static final int POOL_SHUTDOWN = 2;

   public final String catalog;
   public final boolean isReadOnly;
   public final boolean isAutoCommit;
   public int transactionIsolation;

   protected final PoolUtilities poolUtils;
   protected final HikariConfig configuration;
   protected final AtomicInteger totalConnections;
   protected final ConcurrentBag<PoolBagEntry> connectionBag;
   protected final ThreadPoolExecutor addConnectionExecutor;
   protected final ThreadPoolExecutor closeConnectionExecutor;
   protected final ScheduledThreadPoolExecutor houseKeepingExecutorService;

   protected final boolean isUseJdbc4Validation;
   protected final boolean isIsolateInternalQueries;

   protected volatile int poolState;
   protected volatile long connectionTimeout;
   protected volatile long validationTimeout;
   
   private final LeakTask leakTask;
   private final DataSource dataSource;
   private final GlobalPoolLock suspendResumeLock;
   private final AtomicReference<Throwable> lastConnectionFailure;

   private final String username;
   private final String password;

   private volatile MetricsTracker metricsTracker;
   private volatile boolean isRecordMetrics;

   /**
    * Construct a HikariPool with the specified configuration.
    *
    * @param config a HikariConfig instance
    */
   public HikariPool(HikariConfig config)
    {
      this.configuration = config;
      this.username = config.getUsername();
      this.password = config.getPassword();

      this.poolUtils = new PoolUtilities(config);
      this.dataSource = poolUtils.initializeDataSource(config.getDataSourceClassName(), config.getDataSource(), config.getDataSourceProperties(), config.getDriverClassName(), config.getJdbcUrl(), username, password);

      this.connectionBag = new ConcurrentBag<PoolBagEntry>(this);
      this.totalConnections = new AtomicInteger();
      this.connectionTimeout = config.getConnectionTimeout();
      this.validationTimeout = config.getValidationTimeout();
      this.lastConnectionFailure = new AtomicReference<Throwable>();

      this.isReadOnly = config.isReadOnly();
      this.isAutoCommit = config.isAutoCommit();

      this.suspendResumeLock = config.isAllowPoolSuspension() ? new GlobalPoolLock(true) : GlobalPoolLock.FAUX_LOCK;

      this.catalog = config.getCatalog();
      this.transactionIsolation = getTransactionIsolation(config.getTransactionIsolation());
      this.isIsolateInternalQueries = config.isIsolateInternalQueries();
      this.isUseJdbc4Validation = config.getConnectionTestQuery() == null;
      
      setMetricRegistry(config.getMetricRegistry());
      setHealthCheckRegistry(config.getHealthCheckRegistry());

      this.addConnectionExecutor = createThreadPoolExecutor(config.getMaximumPoolSize(), "HikariCP connection filler (pool " + config.getPoolName() + ")", config.getThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
      this.closeConnectionExecutor = createThreadPoolExecutor(4, "HikariCP connection closer (pool " + config.getPoolName() + ")", config.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

      long delayPeriod = Long.getLong("com.zaxxer.hikari.housekeeping.periodMs", TimeUnit.SECONDS.toMillis(30L));
      ThreadFactory threadFactory = config.getThreadFactory() != null ? config.getThreadFactory() : new DefaultThreadFactory("Hikari Housekeeping Timer (pool " + config.getPoolName() + ")", true);
      this.houseKeepingExecutorService = new ScheduledThreadPoolExecutor(1, threadFactory, new ThreadPoolExecutor.DiscardPolicy());
      this.houseKeepingExecutorService.scheduleAtFixedRate(new HouseKeeper(), delayPeriod, delayPeriod, TimeUnit.MILLISECONDS);
      this.houseKeepingExecutorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      this.houseKeepingExecutorService.setRemoveOnCancelPolicy(true);
      this.leakTask = (config.getLeakDetectionThreshold() == 0) ? LeakTask.NO_LEAK : new LeakTask(config.getLeakDetectionThreshold(), houseKeepingExecutorService);

      poolUtils.setLoginTimeout(dataSource, connectionTimeout);
      registerMBeans(config, this);
      initializeConnections();
   }

   /**
    * Get a connection from the pool, or timeout after connectionTimeout milliseconds.
    *
    * @return a java.sql.Connection instance
    * @throws SQLException thrown if a timeout occurs trying to obtain a connection
    */
   public final Connection getConnection() throws SQLException
   {
      return getConnection(connectionTimeout);
   }

   /**
    * Get a connection from the pool, or timeout after the specified number of milliseconds.
    *
    * @param hardTimeout the maximum time to wait for a connection from the pool
    * @return a java.sql.Connection instance
    * @throws SQLException thrown if a timeout occurs trying to obtain a connection
    */
   public final Connection getConnection(final long hardTimeout) throws SQLException
   {
      suspendResumeLock.acquire();
      long timeout = hardTimeout;
      final long start = System.currentTimeMillis();
      final MetricsContext metricsContext = (isRecordMetrics ? metricsTracker.recordConnectionRequest(start) : MetricsTracker.NO_CONTEXT);

      try {
         do {
            final PoolBagEntry bagEntry = connectionBag.borrow(timeout, TimeUnit.MILLISECONDS);
            if (bagEntry == null) {
               break; // We timed out... break and throw exception
            }

            final long now = System.currentTimeMillis();
            if (bagEntry.evicted || (now - bagEntry.lastAccess > ALIVE_BYPASS_WINDOW && !isConnectionAlive(bagEntry.connection))) {
               closeConnection(bagEntry, "connection evicted or dead"); // Throw away the dead connection and try again
               timeout = hardTimeout - elapsedTimeMs(start);
            }
            else {
               metricsContext.setConnectionLastOpen(bagEntry, now);
               metricsContext.stop();
               return ProxyFactory.getProxyConnection(this, bagEntry, leakTask.start(bagEntry));
            }
         }
         while (timeout > 0L);
      }
      catch (InterruptedException e) {
         throw new SQLException("Interrupted during connection acquisition", e);
      }
      finally {
         suspendResumeLock.release();
      }

      logPoolState("Timeout failure ");
      throw new SQLTimeoutException(String.format("Timeout after %dms of waiting for a connection.", elapsedTimeMs(start)), lastConnectionFailure.getAndSet(null));
   }

   /**
    * Release a connection back to the pool, or permanently close it if it is broken.
    *
    * @param bagEntry the PoolBagEntry to release back to the pool
    */
   public final void releaseConnection(final PoolBagEntry bagEntry)
   {
      metricsTracker.recordConnectionUsage(bagEntry);

      if (bagEntry.evicted) {
         closeConnection(bagEntry, "connection broken or evicted");
      }
      else {
         connectionBag.requite(bagEntry);
      }
   }

   /**
    * Shutdown the pool, closing all idle connections and aborting or closing
    * active connections.
    *
    * @throws InterruptedException thrown if the thread is interrupted during shutdown
    */
   public final void shutdown() throws InterruptedException
   {
      if (poolState != POOL_SHUTDOWN) {
         poolState = POOL_SHUTDOWN;
         LOGGER.info("HikariCP pool {} is shutting down.", configuration.getPoolName());

         logPoolState("Before shutdown ");
         connectionBag.close();
         softEvictConnections();
         houseKeepingExecutorService.shutdown();
         addConnectionExecutor.shutdown();
         houseKeepingExecutorService.awaitTermination(5L, TimeUnit.SECONDS);
         addConnectionExecutor.awaitTermination(5L, TimeUnit.SECONDS);

         final ExecutorService assassinExecutor = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection assassin",
                                                                     configuration.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
         final long start = System.currentTimeMillis();
         do {
            softEvictConnections();
            abortActiveConnections(assassinExecutor);
         }
         while (getTotalConnections() > 0 && elapsedTimeMs(start) < TimeUnit.SECONDS.toMillis(5));

         assassinExecutor.shutdown();
         assassinExecutor.awaitTermination(5L, TimeUnit.SECONDS);
         closeConnectionExecutor.shutdown();
         closeConnectionExecutor.awaitTermination(5L, TimeUnit.SECONDS);
         logPoolState("After shutdown ");

         unregisterMBeans(configuration, this);
         metricsTracker.close();
      }
   }

   /**
    * Evict a connection from the pool.
    *
    * @param proxyConnection the connection to evict
    */
   public final void evictConnection(IHikariConnectionProxy proxyConnection)
   {
      closeConnection(proxyConnection.getPoolBagEntry(), "connection evicted by user");
   }

   /**
    * Get the wrapped DataSource.
    *
    * @return the wrapped DataSource
    */
   public final DataSource getDataSource()
   {
      return dataSource;
   }

   /**
    * Get the pool configuration object.
    *
    * @return the {@link HikariConfig} for this pool
    */
   public final HikariConfig getConfiguration()
   {
      return configuration;
   }

   @Override
   public String toString()
   {
      return configuration.getPoolName();
   }

   // ***********************************************************************
   //                        IBagStateListener callback
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public void addBagItem()
   {
      class AddConnection implements Runnable
      {
         public void run()
         {
            long sleepBackoff = 200L;
            final int maxPoolSize = configuration.getMaximumPoolSize();
            while (poolState == POOL_RUNNING && totalConnections.get() < maxPoolSize && !addConnection()) {
               // If we got into the loop, addConnection() failed, so we sleep and retry
               quietlySleep(sleepBackoff);
               sleepBackoff = Math.min(connectionTimeout / 2, (long) ((double) sleepBackoff * 1.5));
            }
         }
      }

      addConnectionExecutor.execute(new AddConnection());
   }

   // ***********************************************************************
   //                        HikariPoolMBean methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public final int getActiveConnections()
   {
      return connectionBag.getCount(STATE_IN_USE);
   }

   /** {@inheritDoc} */
   @Override
   public final int getIdleConnections()
   {
      return connectionBag.getCount(STATE_NOT_IN_USE);
   }

   /** {@inheritDoc} */
   @Override
   public final int getTotalConnections()
   {
      return connectionBag.size() - connectionBag.getCount(STATE_REMOVED);
   }

   /** {@inheritDoc} */
   @Override
   public final int getThreadsAwaitingConnection()
   {
      return connectionBag.getPendingQueue();
   }

   /** {@inheritDoc} */
   @Override
   public void softEvictConnections()
   {
      for (PoolBagEntry bagEntry : connectionBag.values(STATE_IN_USE)) {
         bagEntry.evicted = true;
      }

      for (PoolBagEntry bagEntry : connectionBag.values(STATE_NOT_IN_USE)) {
         if (connectionBag.reserve(bagEntry)) {
            closeConnection(bagEntry, "connection evicted by user");
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public final void suspendPool()
   {
      if (suspendResumeLock == GlobalPoolLock.FAUX_LOCK) {
         throw new IllegalStateException("Pool " + configuration.getPoolName() + " is not suspendable");
      }
      else if (poolState != POOL_SUSPENDED) {
         suspendResumeLock.suspend();
         poolState = POOL_SUSPENDED;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final void resumePool()
   {
      if (poolState == POOL_SUSPENDED) {
         poolState = POOL_RUNNING;
         addBagItem(); // re-populate the pool
         suspendResumeLock.resume();
      }
   }

   public void setMetricRegistry(Object metricRegistry)
   {
      this.isRecordMetrics = metricRegistry != null;
      if (isRecordMetrics) {
         this.metricsTracker = new CodaHaleMetricsTracker(this, (MetricRegistry) metricRegistry);
      }
      else {
         this.metricsTracker = new MetricsTracker(this);
      }
   }

   public void setHealthCheckRegistry(Object healthCheckRegistry)
   {
      if (healthCheckRegistry != null) {
         CodahaleHealthChecker.registerHealthChecks(this, (HealthCheckRegistry) healthCheckRegistry);
      }
   }

   // ***********************************************************************
   //                           Protected methods
   // ***********************************************************************

   /**
    * Create and add a single connection to the pool.
    */
   protected final boolean addConnection()
   {
      // Speculative increment of totalConnections with expectation of success
      if (totalConnections.incrementAndGet() > configuration.getMaximumPoolSize()) {
         totalConnections.decrementAndGet(); // Pool is maxed out, so undo speculative increment of totalConnections
         lastConnectionFailure.set(new SQLException(String.format("HikariCP pool %s is at maximum capacity", configuration.getPoolName())));
         return true;
      }

      Connection connection = null;
      try {
         connection = (username == null && password == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);
         
         if (isUseJdbc4Validation && !poolUtils.isJdbc4ValidationSupported(connection)) {
            throw new SQLException("JDBC4 Connection.isValid() method not supported, connection test query must be configured");
         }
         
         final int originalTimeout = poolUtils.getAndSetNetworkTimeout(connection, connectionTimeout);
         
         transactionIsolation = (transactionIsolation < 0 ? connection.getTransactionIsolation() : transactionIsolation);
         
         poolUtils.setupConnection(connection, isAutoCommit, isReadOnly, transactionIsolation, catalog);
         poolUtils.executeSql(connection, configuration.getConnectionInitSql(), isAutoCommit);
         poolUtils.setNetworkTimeout(connection, originalTimeout);
         
         connectionBag.add(new PoolBagEntry(connection, this));
         lastConnectionFailure.set(null);
         LOGGER.debug("Connection {} added to pool {} ", connection, configuration.getPoolName());
         return true;
      }
      catch (Exception e) {
         totalConnections.decrementAndGet(); // We failed so undo speculative increment of totalConnections
         lastConnectionFailure.set(e);
         LOGGER.debug("Connection attempt to database in pool {} failed: {}", configuration.getPoolName(), e.getMessage(), e);
         poolUtils.quietlyCloseConnection(connection, "exception during connection creation");
         return false;
      }
   }

   /**
    * Fill pool up from current idle connections (as they are perceived at the point of execution) to minimumIdle connections.
    */
   protected void fillPool()
   {
      final int connectionsToAdd = configuration.getMinimumIdle() - getIdleConnections();
      for (int i = 0; i < connectionsToAdd; i++) {
         addBagItem();
      }

      if (connectionsToAdd > 0 && LOGGER.isDebugEnabled()) {
         addConnectionExecutor.execute(new Runnable() {
            public void run() {
               logPoolState("After fill ");
            }
         });
      }
   }

   /**
    * Permanently close the real (underlying) connection (eat any exception).
    *
    * @param connectionProxy the connection to actually close
    */
   protected void closeConnection(final PoolBagEntry bagEntry, final String closureReason)
   {
      final Connection connection = bagEntry.connection;
      bagEntry.connection = null;
      bagEntry.cancelMaxLifeTermination();
      if (connectionBag.remove(bagEntry)) {
         final int tc = totalConnections.decrementAndGet();
         if (tc < 0) {
            LOGGER.warn("Internal accounting inconsistency, totalConnections={}", tc, new Exception());
         }
         
         closeConnectionExecutor.execute(new Runnable() {
            public void run() {
               poolUtils.quietlyCloseConnection(connection, closureReason);
            }
         });
      }
   }

   /**
    * Check whether the connection is alive or not.
    *
    * @param connection the connection to test
    * @param timeoutMs the timeout before we consider the test a failure
    * @return true if the connection is alive, false if it is not alive or we timed out
    */
   protected boolean isConnectionAlive(final Connection connection)
   {
      try {
         int timeoutSec = (int) TimeUnit.MILLISECONDS.toSeconds(validationTimeout);

         if (isUseJdbc4Validation) {
            return connection.isValid(timeoutSec);
         }

         final int originalTimeout = poolUtils.getAndSetNetworkTimeout(connection, validationTimeout);

         try (Statement statement = connection.createStatement()) {
            poolUtils.setQueryTimeout(statement, timeoutSec);
            statement.executeQuery(configuration.getConnectionTestQuery());
         }

         if (isIsolateInternalQueries && !isAutoCommit) {
            connection.rollback();
         }

         poolUtils.setNetworkTimeout(connection, originalTimeout);

         return true;
      }
      catch (SQLException e) {
         LOGGER.warn("Exception during keep alive check, that means the connection ({}) must be dead.", connection, e);
         return false;
      }
   }

   // ***********************************************************************
   //                           Private methods
   // ***********************************************************************

   /**
    * Attempt to abort() active connections, or close() them.
    *
    * @throws InterruptedException 
    */
   private void abortActiveConnections(final ExecutorService assassinExecutor) throws InterruptedException
   {
      for (PoolBagEntry bagEntry : connectionBag.values(STATE_IN_USE)) {
         try {
            bagEntry.aborted = bagEntry.evicted = true;
            bagEntry.connection.abort(assassinExecutor);
         }
         catch (Throwable e) {
            if (e instanceof InterruptedException) {
               throw (InterruptedException) e;
            }
            poolUtils.quietlyCloseConnection(bagEntry.connection, "connection aborted during shutdown");
         }
         finally {
            bagEntry.connection = null;
            if (connectionBag.remove(bagEntry)) {
               totalConnections.decrementAndGet();
            }
         }
      }
   }
   
   /**
    * Fill the pool up to the minimum size.
    */
   private void initializeConnections()
   {
      if (configuration.isInitializationFailFast()) {
         try {
            try {
               if (!addConnection()) {
                  shutdown();
                  throw new PoolInitializationException(lastConnectionFailure.getAndSet(null));
               }
   
               ConnectionProxy connection = (ConnectionProxy) getConnection();
               connection.getPoolBagEntry().evicted = (configuration.getMinimumIdle() == 0);
               connection.close();
            }
            catch (SQLException e) {
               shutdown();
               throw new PoolInitializationException(e);
            }
         }
         catch (InterruptedException ie) {
            throw new PoolInitializationException(ie);
         }
      }

      fillPool();
   }

   public final void logPoolState(String... prefix)
   {
      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug("{}pool stats {} (total={}, inUse={}, avail={}, waiting={})",
                      (prefix.length > 0 ? prefix[0] : ""), configuration.getPoolName(),
                      getTotalConnections(), getActiveConnections(), getIdleConnections(), getThreadsAwaitingConnection());
      }
   }

   // ***********************************************************************
   //                      Non-anonymous Inner-classes
   // ***********************************************************************

   /**
    * The house keeping task to retire idle connections.
    */
   private class HouseKeeper implements Runnable
   {
      @Override
      public void run()
      {
         logPoolState("Before cleanup ");

         connectionTimeout = configuration.getConnectionTimeout(); // refresh member in case it changed

         final long now = System.currentTimeMillis();
         final long idleTimeout = configuration.getIdleTimeout();

         for (PoolBagEntry bagEntry : connectionBag.values(STATE_NOT_IN_USE)) {
            if (connectionBag.reserve(bagEntry)) {
               if (bagEntry.evicted) {
                  closeConnection(bagEntry, "connection evicted");
               }
               else if (idleTimeout > 0L && now > bagEntry.lastAccess + idleTimeout) {
                  closeConnection(bagEntry, "connection passed idleTimeout");
               }
               else {
                  connectionBag.unreserve(bagEntry);
               }
            }
         }
         
         logPoolState("After cleanup ");

         fillPool(); // Try to maintain minimum connections
      }
   }
}
