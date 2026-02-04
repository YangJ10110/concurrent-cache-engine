package org.jerome;

import org.junit.Test;
import static org.junit.Assert.*;

public class DoublyLinkedListTest {

    @Test
    public void testAddToHeadOnEmptyList() {
        // ARRANGE
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();

        // ACT
        list.addToHead("First", 100);

        // ASSERT
        Node<String, Integer> currentHead = list.getHead();
        Node<String, Integer> currentTail = list.getTail();

        assertNotNull("Head should not be null", currentHead);
        assertNotNull("Tail should not be null", currentTail);
        assertEquals("Head and Tail should be same for single node", currentHead, currentTail);
        assertEquals("Head key should be 'First'", "First", currentHead.key);
    }

    @Test
    public void testAddToHeadOnPopulatedList() {
        // ARRANGE
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();
        list.addToHead("Old", 1);

        // ACT
        list.addToHead("New", 2);

        // ASSERT
        Node<String, Integer> currentHead = list.getHead();
        Node<String, Integer> currentTail = list.getTail();

        assertNotNull(currentHead);
        assertEquals("New", currentHead.key);

        // handshake verification
        assertEquals(currentTail, currentHead.next);
        assertEquals(currentHead, currentTail.prev);
    }

    @Test
    public void testMultipleAdditions() {
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();
        list.addToHead("C", 3);
        list.addToHead("B", 2);
        list.addToHead("A", 1);

        Node<String, Integer> head = list.getHead();
        Node<String, Integer> tail = list.getTail();

        assertEquals("A", head.key);
        assertEquals("C", tail.key);

        // verify ordering
        assertEquals("B", head.next.key);
        assertEquals("A", head.next.prev.key);
    }

    @Test
    public void testRemoveTailMultipleNodes() {
        // ARRANGE
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();
        list.addToHead("C", 3);
        list.addToHead("B", 2);
        list.addToHead("A", 1);

        // ACT
        list.removeTail();

        // ASSERT
        Node<String, Integer> currentTail = list.getTail();

        assertEquals("B", currentTail.key);
        assertNull(currentTail.next.next);
        assertEquals("A", list.getHead().key);
    }

    @Test
    public void testRemoveTailLastNode() {
        // ARRANGE
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();
        list.addToHead("OnlyNode", 1);

        // ACT
        list.removeTail();

        // ASSERT
        assertNull(list.getHead());
        assertNull(list.getTail());
    }

    @Test
    public void testRemoveTailEmptyList() {
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();

        // Should NOT throw exception
        list.removeTail();
    }

    @Test
    public void testMoveMiddleNodeToHead() {
        // ARRANGE
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();

        list.addToHead("C", 3); // tail
        list.addToHead("B", 2); // middle
        list.addToHead("A", 1); // head

        // List: A <-> B <-> C

        Node<String, Integer> middleNode = list.getHead().next; // B

        // ACT
        list.moveNodeToHead(middleNode);

        // ASSERT
        Node<String, Integer> head = list.getHead();
        Node<String, Integer> tail = list.getTail();

        // B should now be head
        assertEquals("B", head.key);

        // Order should now be: B <-> A <-> C
        assertEquals("A", head.next.key);
        assertEquals("B", head.next.prev.key);

        assertEquals("C", tail.key);

        // Bidirectional integrity check
        Node<String, Integer> node = head;
        while (node.next != null) {
            assertEquals(node, node.next.prev);
            node = node.next;
        }
    }

    @Test
    public void testMoveTailToHead() {
        // ARRANGE
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();

        list.addToHead("C", 3); // tail
        list.addToHead("B", 2);
        list.addToHead("A", 1); // head

        Node<String, Integer> tailNode = list.getTail(); // C

        // ACT
        list.moveNodeToHead(tailNode);

        // ASSERT
        Node<String, Integer> head = list.getHead();
        Node<String, Integer> tail = list.getTail();

        assertEquals("C", head.key);
        assertEquals("B", tail.key);

        assertNull(head.prev.prev);
        assertNull(tail.next.next);
    }

    @Test
    public void testMoveHeadToHead_NoChange() {
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();

        list.addToHead("B", 2);
        list.addToHead("A", 1);

        Node<String, Integer> originalHead = list.getHead();

        list.moveNodeToHead(originalHead);

        assertEquals(originalHead, list.getHead());
        assertEquals("B", list.getTail().key);
    }

    @Test
    public void testMoveDeepMiddleNode() {
        DoublyLinkedList<String, Integer> list = new DoublyLinkedList<>();

        list.addToHead("D",4);
        list.addToHead("C",3);
        list.addToHead("B",2);
        list.addToHead("A",1);

        // A <-> B <-> C <-> D

        Node<String,Integer> node = list.getHead().next.next; // C

        list.moveNodeToHead(node);

        Node<String,Integer> n = list.getHead();
        while (n.next != null) {
            assertEquals(n, n.next.prev);
            n = n.next;
        }
    }









}
