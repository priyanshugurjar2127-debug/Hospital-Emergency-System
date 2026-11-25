package dsa;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Generic Hash Map using Separate Chaining (Linked Nodes per Bucket).
 * Provides O(1) average-case lookup, insertion, and deletion by String ID.
 */
public class CustomHashMap<K, V> {
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Node<K, V>[] buckets;
    private int capacity;
    private int size;

    @SuppressWarnings("unchecked")
    public CustomHashMap(int capacity) {
        this.capacity = capacity;
        this.buckets = (Node<K, V>[]) new Node[capacity];
        this.size = 0;
    }

    private int getBucketIndex(K key) {
        int h = (key == null) ? 0 : key.hashCode();
        h ^= (h >>> 16);
        return (h & 0x7fffffff) % capacity;
    }

    public void put(K key, V value) {
        int index = getBucketIndex(key);
        Node<K, V> current = buckets[index];
        while (current != null) {
            if ((key == null && current.key == null) || (key != null && key.equals(current.key))) {
                current.value = value;
                return;
            }
            current = current.next;
        }
        Node<K, V> newNode = new Node<>(key, value);
        newNode.next = buckets[index];
        buckets[index] = newNode;
        size++;
    }

    public V get(K key) {
        int index = getBucketIndex(key);
        Node<K, V> current = buckets[index];
        while (current != null) {
            if ((key == null && current.key == null) || (key != null && key.equals(current.key))) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public boolean remove(K key) {
        int index = getBucketIndex(key);
        Node<K, V> current = buckets[index];
        Node<K, V> prev = null;
        while (current != null) {
            if ((key == null && current.key == null) || (key != null && key.equals(current.key))) {
                if (prev == null) {
                    buckets[index] = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
                return true;
            }
            prev = current;
            current = current.next;
        }
        return false;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public List<V> values() {
        List<V> all = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            Node<K, V> current = buckets[i];
            while (current != null) {
                all.add(current.value);
                current = current.next;
            }
        }
        return all;
    }

    public int size() {
        return size;
    }
}
