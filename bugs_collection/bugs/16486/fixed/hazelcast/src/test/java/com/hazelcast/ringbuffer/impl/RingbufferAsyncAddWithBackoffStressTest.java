package com.hazelcast.ringbuffer.impl;

import com.hazelcast.config.Config;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestThread;
import com.hazelcast.test.annotation.NightlyTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static com.hazelcast.ringbuffer.OverflowPolicy.FAIL;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

@RunWith(HazelcastSerialClassRunner.class)
@Category(NightlyTest.class)
public class RingbufferAsyncAddWithBackoffStressTest extends HazelcastTestSupport {
    private volatile boolean stop;
    private Ringbuffer<Long> ringbuffer;

    @Test
    public void whenNoTTL() throws Exception {
        RingbufferConfig ringbufferConfig = new RingbufferConfig("foo")
                .setCapacity(200 * 1000)
                .setTimeToLiveSeconds(0);
        test(ringbufferConfig);
    }

    @Test
    public void whenTTLEnabled() throws Exception {
        RingbufferConfig ringbufferConfig = new RingbufferConfig("foo")
                .setCapacity(200 * 1000)
                .setTimeToLiveSeconds(1);
        test(ringbufferConfig);
    }

    @Test
    public void whenLongTTLAndSmallBuffer() throws Exception {
        RingbufferConfig ringbufferConfig = new RingbufferConfig("foo")
                .setCapacity(1000)
                .setTimeToLiveSeconds(1);
        test(ringbufferConfig);
    }

    @Test
    public void whenShortTTLAndBigBuffer() throws Exception {
        RingbufferConfig ringbufferConfig = new RingbufferConfig("foo")
                .setCapacity(20 * 1000 * 1000)
                .setTimeToLiveSeconds(1);
        test(ringbufferConfig);
    }

    public void test(RingbufferConfig ringbufferConfig) throws Exception {
        Config config = new Config();
        config.addRingBufferConfig(ringbufferConfig);
        HazelcastInstance[] instances = createHazelcastInstanceFactory(2).newInstances(config);
        ringbuffer = instances[0].getRingbuffer(ringbufferConfig.getName());

        ConsumeThread consumer1 = new ConsumeThread(1);
        consumer1.start();

        ConsumeThread consumer2 = new ConsumeThread(2);
        consumer2.start();

        ProduceThread producer = new ProduceThread();
        producer.start();

        SECONDS.sleep(5 * 60);
        stop = true;
        System.out.println("Waiting fo completion");

        producer.assertSucceedsEventually();
        consumer1.assertSucceedsEventually();
        consumer2.assertSucceedsEventually();

        System.out.println("producer.produced:" + producer.produced);

        assertEquals(producer.produced, consumer1.seq);
        assertEquals(producer.produced, consumer2.seq);
    }

    class ProduceThread extends TestThread {
        private volatile long produced;

        public ProduceThread() {
            super("ProduceThread");
        }

        @Override
        public void doRun() throws Throwable {
            long lastMs = System.currentTimeMillis();
            while (!stop) {
                long sleepMs = 100;
                for (; ; ) {
                    long result = ringbuffer.addAsync(produced, FAIL).get();
                    if (result != -1) {
                        break;
                    }
                    TimeUnit.MILLISECONDS.sleep(sleepMs);
                    sleepMs = sleepMs * 2;
                    if (sleepMs > 1000) {
                        sleepMs = 1000;
                    }
                }

                produced++;

                long now = System.currentTimeMillis();
                if (now > lastMs + 2000) {
                    lastMs = now;
                    System.out.println(getName() + " at " + produced);
                }
            }

            ringbuffer.add(Long.MIN_VALUE);
        }
    }

    class ConsumeThread extends TestThread {
        volatile long seq;

        public ConsumeThread(int id) {
            super("ConsumeThread-" + id);
        }

        @Override
        public void doRun() throws Throwable {
            long lastMs = System.currentTimeMillis();
            seq = ringbuffer.headSequence();

            for (; ; ) {
                Long item = ringbuffer.readOne(seq);
                if (item.equals(Long.MIN_VALUE)) {
                    break;
                }

                assertEquals(new Long(seq), item);

                seq++;

                long now = System.currentTimeMillis();
                if (now > lastMs + 2000) {
                    lastMs = now;
                    System.out.println(getName() + " at " + seq);
                }
            }
        }
    }
}