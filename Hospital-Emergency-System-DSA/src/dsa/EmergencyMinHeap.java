package dsa;

import model.Patient;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Min-Heap Priority Queue for Emergency Triage.
 * Orders patients by emergencyLevel ascending (1 = highest urgency),
 * breaking ties by arrivalOrder ascending (FIFO within the same severity).
 * Supports O(log n) insert, extractMin, and dynamic priority updates.
 */
public class EmergencyMinHeap {
    private final List<Patient> heap;

    public EmergencyMinHeap() {
        this.heap = new ArrayList<>();
    }

    public void clear() {
        heap.clear();
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    public void insert(Patient patient) {
        heap.add(patient);
        heapifyUp(heap.size() - 1);
    }

    public Patient peekMin() {
        return heap.isEmpty() ? null : heap.get(0);
    }

    public Patient extractMin() {
        if (heap.isEmpty()) return null;
        Patient top = heap.get(0);
        Patient last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            heapifyDown(0);
        }
        return top;
    }

    /**
     * Dynamically updates a patient's emergency priority level and restores the heap invariant in O(log n).
     */
    public boolean updatePriority(String patientId, int newEmergencyLevel) {
        int idx = -1;
        for (int i = 0; i < heap.size(); i++) {
            if (heap.get(i).getPatientId().equalsIgnoreCase(patientId)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return false;

        int oldLevel = heap.get(idx).getEmergencyLevel();
        heap.get(idx).setEmergencyLevel(newEmergencyLevel);

        if (newEmergencyLevel < oldLevel) {
            heapifyUp(idx);
        } else if (newEmergencyLevel > oldLevel) {
            heapifyDown(idx);
        }
        return true;
    }

    public boolean removeById(String patientId) {
        int idx = -1;
        for (int i = 0; i < heap.size(); i++) {
            if (heap.get(i).getPatientId().equalsIgnoreCase(patientId)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return false;

        Patient last = heap.remove(heap.size() - 1);
        if (idx < heap.size()) {
            heap.set(idx, last);
            heapifyUp(idx);
            heapifyDown(idx);
        }
        return true;
    }

    /**
     * Returns a priority-ordered snapshot of the waiting queue without mutating the active heap.
     */
    public List<Patient> getOrderedWaitingList() {
        EmergencyMinHeap clone = new EmergencyMinHeap();
        clone.heap.addAll(this.heap);
        List<Patient> ordered = new ArrayList<>(clone.size());
        while (!clone.isEmpty()) {
            ordered.add(clone.extractMin());
        }
        return ordered;
    }

    private int compare(Patient a, Patient b) {
        if (a.getEmergencyLevel() != b.getEmergencyLevel()) {
            return Integer.compare(a.getEmergencyLevel(), b.getEmergencyLevel());
        }
        return Long.compare(a.getArrivalOrder(), b.getArrivalOrder());
    }

    private void heapifyUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (compare(heap.get(index), heap.get(parent)) < 0) {
                swap(index, parent);
                index = parent;
            } else {
                break;
            }
        }
    }

    private void heapifyDown(int index) {
        int n = heap.size();
        while (true) {
            int smallest = index;
            int left = 2 * index + 1;
            int right = 2 * index + 2;

            if (left < n && compare(heap.get(left), heap.get(smallest)) < 0) {
                smallest = left;
            }
            if (right < n && compare(heap.get(right), heap.get(smallest)) < 0) {
                smallest = right;
            }
            if (smallest != index) {
                swap(index, smallest);
                index = smallest;
            } else {
                break;
            }
        }
    }

    private void swap(int i, int j) {
        Patient temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
}
