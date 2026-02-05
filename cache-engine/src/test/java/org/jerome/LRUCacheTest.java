package org.jerome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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






}