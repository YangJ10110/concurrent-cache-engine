package org.jerome;
/*
INVARIANTS:
        1. Every key in the map has exactly one node in the DLL.
        2. DLL order represents recency (head = MRU, tail = LRU).
        3. Eviction removes from DLL first, then map.
        4. Map lock is always acquired before DLL lock.
*/

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<K, V> {
    HashMap<K, Node<K,V> > map = new HashMap<>();
    DoublyLinkedList<K,V> dll = new DoublyLinkedList<>();
    ReentrantLock mapLock = new ReentrantLock();
    ReentrantLock listLock = new ReentrantLock();
    int capacity;

    LRUCache(int capacity){
        this.capacity = capacity;
    }

    public V get(K key){
        Node<K,V> node;
        if (!map.containsKey(key)) return null;
        node = map.get(key);
        dll.moveNodeToHead(node);
        V value = node.value;
        return value;
    }

     public void put(K key, V value){
        // check if the key exists
        if (map.containsKey(key)){

            // create a copy of the node
            Node<K,V> node = map.get(key);
            node.value = value;
            dll.moveNodeToHead(node);
            return;
        }

        if (map.size() == capacity){
            Node <K,V> removedKey = dll.removeTail();
            map.remove(removedKey.key);
        }
        // create a node if key does not exist
        Node<K,V> newNode =  dll.addToHead(key, value);
        map.put(key, newNode);

    }
}
