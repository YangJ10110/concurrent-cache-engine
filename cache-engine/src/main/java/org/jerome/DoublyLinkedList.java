package org.jerome;

public class DoublyLinkedList<K, V> {
    private final Node<K, V> head;
    private final Node<K, V> tail;


    public Node<K, V> getHead() {
        return head.next == tail ? null : head.next;
    }

    public Node<K, V> getTail() {
        return tail.prev == head ? null : tail.prev;
    }

    DoublyLinkedList(){
        head = new Node<>(null,null);
        tail = new Node<>(null,null);
        head.next = tail;
        tail.prev = head;
    }

    void detach(Node<K, V> detachNode){
        if (detachNode.prev == null || detachNode.next == null) return;
        detachNode.prev.next = detachNode.next;
        detachNode.next.prev = detachNode.prev;
        detachNode.prev = null;
        detachNode.next = null;
    }

    void attachNodeToHead(Node<K, V> detachedNode){
        if (detachedNode.prev != null || detachedNode.next != null) {
            throw new IllegalStateException("Node already attached");
        }
        detachedNode.prev = head;
        detachedNode.next = head.next;
        head.next.prev = detachedNode;
        head.next = detachedNode;

    }

    public Node<K, V> addToHead(K key, V value){
        Node<K, V> newNode = new Node(key, value);
        attachNodeToHead(newNode);
        return newNode;
    }

    public Node<K,V> removeTail(){
        if (tail.prev == head) return null;
        Node<K,V> detachedTail = tail.prev;
        detach(tail.prev);
        return  detachedTail;
    }

    void moveNodeToHead(Node<K, V> node){
        detach(node);
        attachNodeToHead(node);
    }

}
