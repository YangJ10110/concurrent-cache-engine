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

    void addToHead(K key, V value){
       // Steps

       // 1. Creating the new node

        Node<K, V> newNode = new Node(key, value);

        /** - changed for segmental form for adding head and tails anchors

         // 2. Check  if the list is empty (head is still null)
         boolean listEmpty = (head == null);
         // 3. Logic for empty list
         if (listEmpty){
         head = newNode;
         tail = newNode;
         }

         // 4. Logic for non-empty list

         if(!listEmpty) {
         newNode.next = head;
         head.prev = newNode;
         head = newNode;
         }

        */

        newNode.prev = head;
        newNode.next = head.next;
        head.next.prev = newNode;
        head.next = newNode;
    }

    void removeTail(){
/**
        if (tail == null) return;
        if (head == tail){
            head = null;
            tail = null;
            return;
        }
        Node<K, V> oldTail = tail;
        tail = oldTail.prev;
        tail.next = null;
        oldTail.prev = null;
*/
        if (tail.prev == head) return;
        detach(tail.prev);
    }

    void moveNodeToHead(Node<K, V> node){
        detach(node);
        attachNodeToHead(node);
    }

    // the input should be a node - a memory reference to it
    // i hope this is the correct way lmao
/**
 * 
 * old approach to moveNodeToHead without invariant approach
    void moveNodeToHeadS(Node<K, V> node){
        // first we check if there's a list at all
        if (head == null || node == head ) return;
        // lets create a copy of both of the node
        if (node == tail) tail = node.prev;

        Node<K, V> oldHead = head;
        Node<K, V> newHead = node;


        // switching the prev, next of each node
        // if the node's the next

        if (oldHead.next == node){
            oldHead.next = node.next;
            node.next.prev = oldHead;
        }

        if (node.next == null) {
            node.prev.next = null;
        } else{
            node.next.prev = node.prev;
        }

        if (node.prev != null) {
            node.prev.next = node.next;
        }


        newHead.prev = null;
        newHead.next = oldHead;

        oldHead.prev = newHead;

        head = newHead;
    }
*/


    





}
