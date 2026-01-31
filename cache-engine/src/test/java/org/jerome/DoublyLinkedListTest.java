package org.jerome;


import org.junit.Test;

class DoublyLinkedListTest {
    public static void main(String[] args) {
        testAddToHeadOnEmptyList();
        testAddToHeadOnPopulatedList();
        testMultipleAdditions();
    }

    public static void testAddToHeadOnEmptyList() {
        // --- ARRANGE ---
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();

        // --- ACT ---
        list.addToHead("First", 100);

        // --- ASSERT ---
        Node<String, Integer> currentHead = list.getHead();
        Node<String, Integer> currentTail = list.getTail();
        assert currentHead != null : "Head should not be null";
        assert currentTail != null : "Tail should not be null";
        assert currentHead == list.tail : "Head and Tail should be the same for a single node";
        assert currentHead.key.equals("First") : "Head key should be 'First'";

        System.out.println("Empty List Test: PASSED");
    }

    public static void testAddToHeadOnPopulatedList() {
        // --- ARRANGE ---
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();
        list.addToHead("Old", 1);

        // --- ACT ---
        list.addToHead("New", 2);

        list.addToHead("New2", 2);
        list.addToHead("New3", 2);
        // --- ASSERT ---
        // Use the getters to inspect the private fields
        Node<String, Integer> currentHead = list.getHead();
        Node<String, Integer> currentTail = list.getTail();

        System.out.println(currentHead.key);
        System.out.println(currentTail.key);
        assert currentHead != null : "Head should not be null";
        assert currentHead.key.equals("New") : "New node should be at the head";

        // Verify the "handshake" logic
        assert currentHead.next == currentTail : "New head's next should point to the old node";
        assert currentTail.prev == currentHead : "Old node's prev should point to the new head";

        System.out.println("Populated List Test: PASSED");
    }

    public static void testMultipleAdditions() {
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();
        list.addToHead("C", 3);
        list.addToHead("B", 2);
        list.addToHead("A", 1);

        // Let's walk from Head to Tail
        Node<String, Integer> temp = list.getHead();
        while (temp != null) {
            System.out.print(temp.key + " <-> ");
            temp = temp.next; // This moves us to the next "link"
        }
        System.out.println("null");
    }


}