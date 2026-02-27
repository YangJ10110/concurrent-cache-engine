# Code review: LRUCache read-buffer fix

## What’s good

- **Read path is lock-free after map lookup** — `get()` only holds `mapLock` for the lookup, then releases it. No `listLock` in the hot path, which should reduce contention vs. the old “mapLock → tryLock(listLock) → move to head” design.
- **Batching recency updates** — Moving many nodes to head in one `tryDrain()` / `drainFully()` amortizes listLock cost and avoids one lock per read.
- **Lock ordering is consistent** — `put()` always takes `mapLock` then `listLock`; `tryDrain()` only takes `listLock`. No deadlock from ordering.
- **put() drains before eviction** — `drainFully()` runs at the start of `put()` under `listLock`, so eviction sees an up-to-date list.

---

## Critical bug: recency never updated on read

**Symptom:** `evictionRemovesNodeFromMap` fails (e.g. after `get(1)` you expect key `2` to be evicted on next `put`, but key `2` is still present).

**Cause:** In `get()` you never enqueue the current node into `readBuffer`. So:

- `tryDrain()` only processes whatever was enqueued by other threads (and nothing enqueues).
- No read ever updates recency, so the list order never reflects “recently read” and eviction removes the wrong entry (e.g. the one that was just read instead of the true LRU).

**Fix:** Enqueue the node after a successful lookup, then drain (so this thread may apply its own update if it wins `listLock`):

```java
if (node == null) return null;
readBuffer.offer(node);
tryDrain();
return node.value;
```

---

## Correctness: stale (evicted) nodes in the buffer

**Risk:** After `get(k)` enqueues `node`, another thread can `put()` and evict `k`. That thread’s `drainFully()` (or this thread’s `tryDrain()`) may later run and call `dll.moveNodeToHead(n)` for that node. In `DoublyLinkedList`, `moveNodeToHead` does `detach` then `attachNodeToHead`. For an already-detached node, `detach` is a no-op and `attachNodeToHead` **re-attaches** it, so a node that was evicted (removed from map and list) gets put back into the list. That breaks the invariant “every key in the map has exactly one node in the DLL” and can leave the list containing nodes not in the map.

**Fix:** When draining, only call `moveNodeToHead` for nodes that are still in the list. A node that was evicted has been detached, so `node.prev == null` (and `node.next == null`). In both `tryDrain()` and `drainFully()`:

- Before `dll.moveNodeToHead(n)`, check that the node is still linked, e.g. `if (n.prev != null) { dll.moveNodeToHead(n); }`.

Then evicted nodes in the buffer are skipped and the list is not corrupted.

---

## Minor / cleanup

1. **`DRAIN_THRESHOLD` is unused** — Either use it (e.g. only call `tryDrain()` when the buffer size reaches the threshold to amortize lock cost) or remove it to avoid dead code.
2. **Typo in exception message** — “not greather than” → “not greater than”.
3. **Formatting** — Slight inconsistency (e.g. `HashMap<K, Node<K,V> >` has a space before `>`; `capacityMin`/`capacityMax` could be `static final` if they’re constants).

---

## Summary

| Issue | Severity | Action |
|-------|----------|--------|
| Node never offered to `readBuffer` in `get()` | Critical | Add `readBuffer.offer(node)` before `tryDrain()` |
| Evicted nodes re-attached when draining | Critical | Skip detached nodes in drain loops: `if (n.prev != null) dll.moveNodeToHead(n)` |
| `DRAIN_THRESHOLD` unused | Minor | Use or remove |
| Exception message typo | Minor | Fix “greather” → “greater” |

Applying the two critical fixes will restore correct LRU eviction and avoid list corruption under concurrent eviction and draining.
