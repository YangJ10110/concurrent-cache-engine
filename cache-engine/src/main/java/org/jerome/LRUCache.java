package org.jerome;
/*
INVARIANTS:
        1. Every key in the map has exactly one node in the DLL.
        2. DLL order represents recency (head = MRU, tail = LRU).
        3. Eviction removes from DLL first, then map.
        4. Map lock is always acquired before DLL lock.
*/

/**
 * Thread-safe LRU cache with relaxed recency semantics.
 *
 * Recency updates on read (get) are best-effort:
 * - If the recency list is contended, the cache may skip
 *   moving an entry to the head.
 * - This avoids blocking readers and improves throughput
 *   under concurrent access.
 *
 * Write operations (put, eviction) enforce strict ordering
 * and always update recency.
 *
 * This design favors performance and scalability over
 * perfectly accurate LRU ordering.
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<K, V> {
    HashMap<K, Node<K,V> > map = new HashMap<>();
    DoublyLinkedList<K,V> dll = new DoublyLinkedList<>();
    ReentrantLock mapLock = new ReentrantLock();
    ReentrantLock listLock = new ReentrantLock();
    int capacity;
    int capacityMin = 1;
    long capacityMax = 1_000_000L;

    public LRUCache(int capacity){
        if (capacity < capacityMin || capacity > capacityMax){
            throw new IllegalArgumentException("Capacity must be greater than 1 and not greather than" + capacityMax);
        }
        this.capacity = capacity;

    }

    public V get(K key){
        Node<K,V> node;
        mapLock.lock();
        try{
            node = map.get(key);
        } finally {
            mapLock.unlock();
        }

        if (node == null) return null;

        if( listLock.tryLock()){
            try{
                dll.moveNodeToHead(node);
            } finally {
                listLock.unlock();
            }
        }

        return node.value;
    }

     public void put(K key, V value){
        // check if the key exists

         Node<K,V> node;
         mapLock.lock();
         try{
             node = map.get(key);
             listLock.lock();
             try{
                 if (node == null){
                     if (map.size() >= capacity) {
                         Node<K,V> removedTail = dll.removeTail();
                         if (removedTail != null) map.remove(removedTail.key);
                         // Only add if we actually freed a slot (avoids map size > capacity if
                         // removeTail() returned null or the key was already missing from map).
                         if (map.size() >= capacity) return;
                     }

                     Node<K, V> newNode = dll.addToHead(key, value);
                     map.put(newNode.key, newNode);
                     return;

                 }
                 node.value = value;
                 dll.moveNodeToHead(node);
             } finally {
                listLock.unlock();
             }

         } finally {
             mapLock.unlock();
         }

    }

    void assertInvariants() {
        mapLock.lock();
        try {
            int mapSize = map.size();
            listLock.lock();
            try {
                // 1. Count nodes and collect keys in single pass
                int listSize = 0;
                Set<K> listKeys = new HashSet<>();
                for (Node<K,V> node = dll.getHead(); node != null; node = node.next) {
                    if (node.key != null) {
                        listSize++;
                        listKeys.add(node.key);
                    }
                }
                
                // 2. Verify map and list sizes match
                if (mapSize != listSize) {
                    throw new IllegalStateException(
                        "Map size (" + mapSize + ") != list size (" + listSize + ")"
                    );
                }
                
                // 3. Verify map and list have identical keys
                if (!map.keySet().equals(listKeys)) {
                    throw new IllegalStateException(
                        "Map and list keys don't match. Map: " + 
                        map.keySet() + ", List: " + listKeys
                    );
                }
                
                // 4. Check bidirectional connectivity
                Node<K,V> current = dll.getHead();
                if (current != null) {
                    while (current.next != null) {
                        if (current.next.prev != current) {
                            throw new IllegalStateException(
                                "Bidirectional connectivity broken at node: " + current.key
                            );
                        }
                        current = current.next;
                    }
                }
                
                // 5. Verify capacity not exceeded
                if (mapSize > capacity) {
                    throw new IllegalStateException(
                        "Map size (" + mapSize + ") exceeds capacity (" + capacity + ")"
                    );
                }
                
                // 6. Check all map nodes are attached to list
                for (Node<K,V> mapNode : map.values()) {
                    if (mapNode.prev == null || mapNode.next == null) {
                        throw new IllegalStateException(
                            "Node in map but detached from list: " + mapNode.key
                        );
                    }
                }
                
            } finally {
                listLock.unlock();
            }
        } finally {
            mapLock.unlock();
        }
    }
}
