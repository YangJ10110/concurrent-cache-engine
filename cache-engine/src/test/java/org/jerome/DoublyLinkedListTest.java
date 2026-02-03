package org.jerome;


import org.junit.Test;

class DoublyLinkedListTest {
    public static void main(String[] args) {
        testAddToHeadOnEmptyList();
        testAddToHeadOnPopulatedList();
        testMultipleAdditions();
        testRemoveTailMultipleNodes();
        testRemoveTailLastNode();
        testRemoveTailEmptyList();
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


        public static void testRemoveTailMultipleNodes() {
            // --- ARRANGE ---
            DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();
            list.addToHead("C", 3); // Will be tail
            list.addToHead("B", 2);
            list.addToHead("A", 1); // Will be head
            // List is: A <-> B <-> C

            // --- ACT ---
            list.removeTail();

            // --- ASSERT ---
            Node<String, Integer> currentTail = list.getTail();
            assert currentTail.key.equals("B") : "Tail should now be 'B'";
            assert currentTail.next == null : "New tail's next must be null";
            assert list.getHead().key.equals("A") : "Head should still be 'A'";

            System.out.println("Multiple Nodes Test: PASSED");
        }

        public static void testRemoveTailLastNode() {
            // --- ARRANGE ---
            DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();
            list.addToHead("OnlyNode", 1);

            // --- ACT ---
            list.removeTail();

            // --- ASSERT ---
            assert list.getHead() == null : "Head should be null after removing last node";
            assert list.getTail() == null : "Tail should be null after removing last node";

            System.out.println("Last Node Test: PASSED");
        }

        public static void testRemoveTailEmptyList() {
            // --- ARRANGE ---
            DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();

            // --- ACT & ASSERT ---
            try {
                list.removeTail(); // Should handle gracefully (no-op or return null)
                System.out.println("Empty List Test: PASSED");
            } catch (Exception e) {
                assert false : "removeTail() crashed on an empty list!";
            }
        }
    }


