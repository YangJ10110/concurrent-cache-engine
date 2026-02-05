# Concurrent Cache Engine (Java)

A Java-based in-memory cache built to practice **core data structures, concurrency, and performance trade-offs** found in real backend systems.

This project focuses on **design clarity and correctness under multi-threaded access**, not on using prebuilt cache libraries.

---

## What This Demonstrates

- Practical use of **HashMap + Doubly Linked List** for O(1) cache operations
- Understanding of **LRU eviction mechanics**
- Explicit handling of **thread safety and lock contention**
- Ability to reason about **performance vs correctness trade-offs**
- Incremental system design with testing before merging to `main`

---

## Key Features

- LRU (Least Recently Used) eviction policy
- Thread-safe access for concurrent reads and writes
- Constant-time get/put operations
- No reliance on `LinkedHashMap` or external cache libraries

---

## Architecture Overview

**LRUCache**
- Public cache API
- Coordinates eviction and concurrency

**DoublyLinkedList**
- Maintains access order
- Head = most recently used
- Tail = least recently used

**Node**
- Stores key, value, and list pointers
- Bridges map and list in O(1)

---

## Design Focus

- Correctness before optimization
- Clear invariants and responsibilities
- Trade-offs documented before implementation
- Concurrency handled explicitly, not implicitly

---

## Tech Stack

- Java 11
- Standard concurrency primitives
- JUnit (testing)

---

## Status

Active development.  
Current focus: solid LRU correctness and thread safety before extending to TTL and LFU.
