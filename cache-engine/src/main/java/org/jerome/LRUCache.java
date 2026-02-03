package org.jerome;
/*
INVARIANTS:
        1. Every key in the map has exactly one node in the DLL.
        2. DLL order represents recency (head = MRU, tail = LRU).
        3. Eviction removes from DLL first, then map.
        4. Map lock is always acquired before DLL lock.
*/

public class LRUCache {
}
