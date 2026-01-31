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

    }

    





}
