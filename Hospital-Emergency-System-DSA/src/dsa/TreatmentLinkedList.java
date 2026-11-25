package dsa;

import model.Patient;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Doubly Linked List storing patients currently in treatment
 * as well as completed/discharged patient history for report generation.
 */
public class TreatmentLinkedList {
    private static class Node {
        Patient patient;
        Node prev;
        Node next;

        Node(Patient patient) {
            this.patient = patient;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    public TreatmentLinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    public void addLast(Patient patient) {
        Node node = new Node(patient);
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        size++;
    }

    public Patient removeByPatientId(String patientId) {
        Node curr = head;
        while (curr != null) {
            if (curr.patient.getPatientId().equalsIgnoreCase(patientId)) {
                if (curr.prev != null) {
                    curr.prev.next = curr.next;
                } else {
                    head = curr.next;
                }
                if (curr.next != null) {
                    curr.next.prev = curr.prev;
                } else {
                    tail = curr.prev;
                }
                size--;
                return curr.patient;
            }
            curr = curr.next;
        }
        return null;
    }

    public List<Patient> toList() {
        List<Patient> list = new ArrayList<>();
        Node curr = head;
        while (curr != null) {
            list.add(curr.patient);
            curr = curr.next;
        }
        return list;
    }

    public int size() {
        return size;
    }
}
