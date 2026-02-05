package org.jerome;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class LRUCacheTest {

    @Test
    public void getReturnsNodeValue() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");

        String value = cache.get(1);

        assertEquals("A", value);
    }

    @Test
    public void putExistingKey_updatesNodeValue() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.put(1, "A2");

        assertEquals("A2", cache.get(1));
    }

    @Test
    public void evictionRemovesNodeFromMap() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.put(2, "B");
        cache.get(1);        // 2 becomes LRU
        cache.put(3, "C");  // evict 2

        assertNull(cache.get(2));
        assertEquals("A", cache.get(1));
        assertEquals("C", cache.get(3));
    }

    @Test
    public void concurrentPut_sameKeys_exposesNodeRaces() throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(10);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        for (int t = 0; t < 4; t++) {
            executor.submit(() -> {
                for (int i = 0; i < 1_000; i++) {
                    cache.put(1, i); // same key, same node
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        // Value should be one of the written values — but without locks,
        // you may see inconsistent behavior or crashes
        Integer value = cache.get(1);
        assertNotNull(value);
    }

    @Test
    public void concurrentGetDuringEviction_exposesDanglingNode() throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(3);

        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executor.submit(() -> {
            for (int i = 0; i < 10_000; i++) {
                cache.get(1); // may be evicted concurrently later
            }
            latch.countDown();
        });

        executor.submit(() -> {
            for (int i = 4; i < 10_004; i++) {
                cache.put(i, i); // forces eviction
            }
            latch.countDown();
        });

        latch.await();
        executor.shutdown();
    }










}