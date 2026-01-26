
# **Concurrent Cache Engine — Design Document (Initial Phase)**

**Version:** v1.0.0
**Date:** 2026-01-26
**Authors:** Jerome Yang

---

## 1. **Project Goal**

* Build a **thread-safe, in-memory, LRU cache** as a reusable Java library (`Cache<K, V>`).
* **Goal:** Deep understanding of concurrency, DSA, and performance-critical system design.
* **Constraints:** Multi-threaded access, eventual consistency, relaxed LRU updates for performance.

---

## 2. **Phase 1 Decisions — Context & Constraints**

| Decision        | Chosen Option                    | Trade-offs / Rationale                                                                                                                |
| --------------- | -------------------------------- |---------------------------------------------------------------------------------------------------------------------------------------|
| Language        | Java 11                          | LTS, supports full concurrency primitives, predictable memory model, widely used in backend systems                                   |
| Learning Goal   | Deep understanding               | Focus on understanding DSA in real-life application, and building thread-safe, performant cache; portfolio/documentation is secondary |
| Execution Model | Multi-threaded                   | Ensures practical experience with concurrent access and locking; simulates real backend environment                                   |
| Form Factor     | Reusable library (`Cache<K, V>`) | Allows the cache to be used across multiple applications, decouples from service-specific assumptions                                 |

---

## 3. **Phase 2 Decisions — Eviction Policy**

| Decision        | Chosen Option                      | Trade-offs / Rationale                                                                                        |
| --------------- | ---------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Eviction Policy | LRU only, with pluggable interface | Simpler to implement than LFU; provides clear baseline for future experiments; supports swapping in LFU later |
| LRU Strictness  | Relaxed (experimental)             | Reduces lock contention under multi-threading; strict LRU can be added later for comparison                   |
| Data Structures | Hand-written DLL + Node            | Provides complete control over invariants, memory, and concurrency; better for learning than built-in classes |

**Notes:**

* LRU = Least Recently Used → removes the least recently accessed item when cache reaches capacity.
* DLL maintains order; `head` = most recently used, `tail` = least recently used.

---

## 4. **Phase 3 Decisions — Concurrency & Thread Safety**

| Decision          | Chosen Option                       | Trade-offs / Rationale                                                                                        |
| ----------------- | ----------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Consistency       | Eventually consistent               | Allows relaxed LRU; improves throughput in read-heavy workloads                                               |
| Locking Strategy  | Split locks: `mapLock` + `listLock` | Better concurrency than global lock; careful ordering prevents deadlocks (Map lock acquired before List lock) |
| Read Optimization | Relaxed LRU updates                 | Reads may skip moving node to head to reduce contention; enables experimentation with performance             |

**Notes:**

* `get()` is read-heavy; `put()` is write + eviction-heavy.
* Lock acquisition order is critical: **Map lock → List lock → Eviction**.
* Focus on correct invariants under concurrency before adding TTL or LFU.

---

## 5. **Data Structure Mapping**

| Class                   | Responsibility                                             | Notes                                                                                                                        |
| ----------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `Node<K,V>`             | Represents a key-value pair; links for DLL (`prev`/`next`) | Plain data object; no concurrency handling inside                                                                            |
| `DoublyLinkedList<K,V>` | Maintains LRU order                                        | All modifications protected by `listLock`; supports `moveToHead`, `removeTail`, `remove`                                     |
| `LRUCache<K,V>`         | Main cache class                                           | Maintains `HashMap<K, Node>` + `DLL`; manages concurrency via `ReentrantLock`s; handles `get()`, `put()`, and eviction logic |

---

## 6. **Invariants**

1. Every key in `map` has exactly one node in `DLL`.
2. DLL order represents recency (`head` = MRU, `tail` = LRU).
3. Eviction removes nodes from DLL first, then map.
4. Lock acquisition order:

    * `mapLock` before `listLock`
    * Both locks held during eviction
5. Relaxed LRU updates may skip `moveToHead` during `get()` under contention.

---

## 7. **Trade-offs & Notes**

| Aspect           | Trade-off                                                                                                             |
| ---------------- | --------------------------------------------------------------------------------------------------------------------- |
| Read performance | Relaxed LRU improves read throughput at the cost of slightly inaccurate recency                                       |
| Complexity       | Split locks increase concurrency but require careful ordering to prevent deadlocks                                    |
| Correctness      | Eventually consistent LRU is sufficient for cache workloads; stricter linearizable guarantees would reduce throughput |
| Extensibility    | LRU-only now, but pluggable policy interface allows LFU or TTL in future phases                                       |

---

## 8. **Next Steps (Phase 4 Planning)**

* Implement core logic in the three classes:

    1. `Node` — simple data container
    2. `DoublyLinkedList` — order maintenance, node removal, move-to-head
    3. `LRUCache` — thread-safe `get()`, `put()`, eviction
* Write **unit tests**:

    * Single-threaded correctness
    * Multi-threaded stress tests (get/put race conditions)
* Prepare for **TTL / time-based eviction** experiments
* Prepare for **strict vs relaxed LRU** performance comparison


