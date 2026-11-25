package dsa;

import model.Doctor;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom FIFO Queue implemented via Singly Linked Nodes (head & tail pointers).
 * Manages available doctors in fair rotation with O(1) enqueue and O(1) dequeue.
 */
public class DoctorWaitingQueue {
    private static class Node {
        Doctor doctor;
        Node next;

        Node(Doctor doctor) {
            this.doctor = doctor;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    public DoctorWaitingQueue() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    public void enqueue(Doctor doctor) {
        // Avoid duplicate enqueue if already queued
        if (contains(doctor.getDoctorId())) return;
        Node node = new Node(doctor);
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        size++;
    }

    public Doctor dequeue() {
        if (head == null) return null;
        Doctor doc = head.doctor;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return doc;
    }

    public Doctor peek() {
        return (head == null) ? null : head.doctor;
    }

    public boolean contains(String doctorId) {
        Node curr = head;
        while (curr != null) {
            if (curr.doctor.getDoctorId().equalsIgnoreCase(doctorId)) {
                return true;
            }
            curr = curr.next;
        }
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public List<Doctor> toList() {
        List<Doctor> list = new ArrayList<>();
        Node curr = head;
        while (curr != null) {
            list.add(curr.doctor);
            curr = curr.next;
        }
        return list;
    }
}
