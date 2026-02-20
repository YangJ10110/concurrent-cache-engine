# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### To Fix
- Bug fixes from benchmark testing

## [0.2.0] - 2026-02-20

### Added
- Phase 2: Thread safety with split locking (`mapLock`, `listLock`)
- Phase 4: Correctness validation
  - Multithreaded stress tests
  - Invariant checks under concurrent access
  - Edge case coverage (capacity=1, null handling)
- Relaxed LRU updates on reads for improved throughput
- Strict recency updates on writes and eviction

### Changed
- Lock acquisition order enforced to prevent deadlocks

## [0.1.0] - 2026-02-20 (retroactive)

### Added
- Phase 1: Core data structures
  - `Node<K,V>` — minimal data container
  - `DoublyLinkedList<K,V>` — sentinel-based LRU ordering
  - Hand-managed attach/detach semantics
- Basic single-threaded LRUCache implementation
