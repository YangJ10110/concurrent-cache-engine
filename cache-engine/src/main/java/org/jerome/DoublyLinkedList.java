package org.jerome;

public class DoublyLinkedList<K, V> {
    private Node<K, V> head;
    private Node<K, V> tail;

    private final Node<K,V> headS;
    private final Node<K,V> tailS;

    public Node<K, V> getHead() {
        return headS.next == tailS ? null : headS.next;
    }

    public Node<K, V> getTail() {
        return tailS.prev == headS ? null : tailS.prev;
    }

    DoublyLinkedList(){
        headS = new Node<>(null,null);
        tailS = new Node<>(null,null);
        headS.next = tailS;
        tailS.prev = headS;

        this.head = null;
        this.tail = null;
    }

    void detach(Node<K, V> detachNode){
        detachNode.prev.next = detachNode.next;
        detachNode.next.prev = detachNode.prev;
        detachNode.prev = null;
        detachNode.next = null;
    }

    void attachNodeToHead(Node<K, V> detachNode){
        detachNode.prev = headS;
        detachNode.next = headS.next;
        headS.next.prev = detachNode;
        headS.next = detachNode;

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

        newNode.prev = headS;
        newNode.next = headS.next;
        headS.next.prev = newNode;
        headS.next = newNode;
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
        if (tailS.prev == headS) return;
        tailS.prev.prev.next = tailS;
        tailS.prev = tailS.prev.prev;
    }

    void moveNodeToHead(Node<K, V> node){
        detach(node);
        attachNodeToHead(node);
    }

    // the input should be a node - a memory reference to it
    // i hope this is the correct way lmao
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



    





}
