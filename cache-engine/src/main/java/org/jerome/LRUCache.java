package org.jerome;
/*
INVARIANTS:
        1. Every key in the map has exactly one node in the DLL.
        2. DLL order represents recency (head = MRU, tail = LRU).
        3. Eviction removes from DLL first, then map.
        4. Map lock is always acquired before DLL lock.
*/

import java.util.HashMap;
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

    LRUCache(int capacity){
        if (capacity < capacityMin || capacity > capacityMax){
            throw new IllegalArgumentException("Capacity must be greater than 1 and not greather than 1000000");
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

             //

             listLock.lock();
             try{
                 if (node == null){

                     if (map.size() >= capacity) {
                         Node<K,V> removedTail = dll.removeTail();
                         if (removedTail != null) map.remove( removedTail.key);
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
}
