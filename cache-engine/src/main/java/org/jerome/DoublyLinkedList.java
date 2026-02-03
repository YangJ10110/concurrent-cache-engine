package org.jerome;

public class DoublyLinkedList<K, V> {
    Node<K, V> head;
    Node<K, V> tail;

    public Node<K, V> getHead() {
        return head;
    }

    public Node<K, V> getTail() {
        return tail;
    }

    DoublyLinkedList(){
        this.head = null;
        this.tail = null;
    }

    void addToHead(K key, V value){
       // Steps

       // 1. Creating the new node

        Node<K, V> newNode = new Node(key, value);


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
    }

    void removeTail(){
        // removing the tail

        // Steps
        // Goal - make the prev of the tail to become the new tail

        // make the next of the new tail == null -> garbage collection
        if (tail == null) return;
        if (head == tail){
            head = null;
            tail = null;
            return;
        }

        tail = tail.prev;
        if (tail.next == null) return;
        tail.next = null;
    }

    





}
