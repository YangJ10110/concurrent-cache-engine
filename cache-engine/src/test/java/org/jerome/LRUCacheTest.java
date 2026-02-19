package org.jerome;

import org.junit.Test;

import java.util.Optional;
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

    @Test
    public void singleThreadedInvariants() {
        LRUCache<Integer, Integer> cache = new LRUCache<>(10);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        cache.assertInvariants();

        cache.get(1);
        cache.assertInvariants();

        cache.put(4, 4);
        cache.assertInvariants();
        
        
    }   


    @Test
    public void multiThreadedInvariants() throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(10);

        int testIterate = 1_000_000;

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);


        executor.submit(() -> {
            try {
                for (int j = 0; j < testIterate; j++) {
                    cache.put(j % 20, j);
                }
            } finally {
                latch.countDown(); // <-- completion signal (CORRECT PLACE)
            }
        });

        executor.submit(() -> {
            try {
                for (int j = 0; j < testIterate; j++) {
                    cache.put(j % 20, j);
                }
            } finally {
                latch.countDown(); // <-- completion signal (CORRECT PLACE)
            }
        });


//        // 2 writer threads
//        for (int t = 0; t < 2; t++) {
//            executor.submit(() -> {
//                try {
//                    for (int j = 0; j < 1_000; j++) {
//                        cache.put(j % 20, j);
//                    }
//                } finally {
//                    latch.countDown(); // <-- completion signal (CORRECT PLACE)
//                }
//            });
//        }

        // 2 reader threads

        executor.submit(() -> {
            try {
                for (int j = 0; j < testIterate; j++) {
                    cache.get(j % 20);
                }
            } finally {
                latch.countDown(); // <-- completion signal
            }
        });

        executor.submit(() -> {
            try {
                for (int j = 0; j < testIterate; j++) {
                    cache.get(j % 20);
                }
            } finally {
                latch.countDown(); // <-- completion signal
            }
        });



//        for (int t = 0; t < 2; t++) {
//            executor.submit(() -> {
//                try {
//                    for (int j = 0; j < 1_000; j++) {
//                        cache.get(j % 20);
//                    }
//                } finally {
//                    latch.countDown(); // <-- completion signal
//                }
//            });
//        }

        latch.await();        // waits for ALL 4 threads to FINISH
        executor.shutdown();  // cleanup
        cache.assertInvariants();


/**
 Attempt 1 to write a multithreaded test to check invariants
        LRUCache<Integer, Integer> cache = new LRUCache<>(10);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
         // write threads
        for (int i = 0; i < 2; i++) {
            for (int j = 1_000;j>0;j--) {
                int finalN = j;
                executor.submit(() -> {

                    cache.put(finalN % 2, finalN);
                });
            }
            latch.countDown();
            // read threads
            for (int j = 1_000;j>0;j--) {
                int finalN = j;
                executor.submit(() -> {
                    cache.get(finalN % 2);
                });
            }
            latch.countDown();
        }
        latch.await();
        executor.shutdown();
        cache.assertInvariants();


this implementation is inaccurate in testing the invariants because"

1. the count down is called by the main thread after submitting the threads, so the main thread is not waiting for the threads to finish
2. the await() waits for task submission instead of task completion
3. assertInvariants() is called by the main thread after submitting the threads, so the main thread is not waiting for the threads to finish

Task Submission: latch.countDown()
Task Completion: latch.await()


 **/

    }

    @Test
    public void multiThrededInvariantsTest_EvictionHeavy() throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(3);

        int testIterate = 1_000_000;

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 3 Writer

        executor.submit(() -> {
            try {
                for (int j = 0; j < testIterate; j++) {
                    cache.put(j % 1000, j);
                }
            } finally {
                latch.countDown(); // <-- completion signal (CORRECT PLACE)
            }
        });

        executor.submit(() -> {
            try {
                for (int j = 0; j < testIterate; j++) {
                    cache.put(j % 1000, j);
                }
            } finally {
                latch.countDown(); // <-- completion signal (CORRECT PLACE)
            }
        });

        executor.submit(() -> {
            try {
                for (int j = 0; j < testIterate; j++) {
                    cache.put(j % 1000, j);
                }
            } finally {
                latch.countDown(); // <-- completion signal (CORRECT PLACE)
            }
        });


        // 1 Reader

        executor.submit(() -> {
            try {
                for (int j = 0; j < testIterate; j++) {
                    cache.get(j % 1000);
                }
            } finally {
                latch.countDown(); // <-- completion signal
            }
        });

        latch.await();        // waits for ALL 4 threads to FINISH
        executor.shutdown();  // cleanup
        cache.assertInvariants();

    }

    @Test
    public void edgeCaseTest_CapacityOne() {
        LRUCache<Integer, Integer> cache = new LRUCache<>(1);
        cache.put(1, 1);
        cache.assertInvariants();
        cache.put(2, 2);
        cache.assertInvariants();
        long result_1= cache.get(2);
        assertEquals(2,result_1);
        assertNull(cache.get(1));
    }

}