package com.jagrosh.vortex.utils;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A buffer that can only hold up to n elements at a time. This is best used when a bounded datastructures is needed,
 * but would need to be frequently re-allocated, filled in with nulls, or otherwise reset in some way. Null values are not
 * permitted.
 * @param <E> The type of element the buffer holds
 */
// TODO: Maybe make circular
public class TemporaryBuffer<E> implements Collection<E> {
    private final E[] array;
    private final E[] nullArray;
    private final int CAPACITY;
    private int size;

    /**
     * Creates a {@link TemporaryBuffer} with the specified capacity.
     * @param capacity The maximum capacity. Reccomended to be a power of two
     */
    @SuppressWarnings("unchecked")
    public TemporaryBuffer(int capacity) {
        this.CAPACITY = capacity;
        array = (E[]) new Object[capacity];
        nullArray = (E[]) new Object[capacity];
        Arrays.fill(nullArray, null);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        Objects.requireNonNull(o);

        for (int i = 0; i < size; i++) {
            if (array[i].equals(o)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the element at the specified index
     * @param index The index
     * @return The element at the specified index
     * @throws IndexOutOfBoundsException if the index is higher than the current {@link #size()}, or is negative
     */
    public E get(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index " + index + " invalid for buffer of size " + size);
        }

        return array[index];
    }

    /**
     * Sets an element at a given index
     * @param index The index
     * @param e The element
     * @return The old element
     */
    public E set(int index, E e) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index " + index + " invalid for buffer of size " + size);
        }

        E old = array[index];
        array[index] = e;
        return old;
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int index = 0;
            @Override
            public boolean hasNext() {
                return index <= size - 1;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return array[index++];
            }
        };
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return Arrays.copyOf(array, size);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(@NotNull T[] a) {
        Objects.requireNonNull(a);

        if (a.length < size) {
            return (T[]) Arrays.copyOf(array, size, a.getClass());
        } else {
            System.arraycopy(array, 0, a, 0, size);
            return a;
        }
    }

    @Override
    public boolean add(E e) {
        if (e == null) {
            throw new NullPointerException("Null elements are not supported");
        }
        if (size >= CAPACITY) {
            throw new IllegalStateException("Maximum capacity reached!");
        }

        array[size++] = e;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Feel free to implement if required");
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        if (size < c.size()) {
            return false;
        }

        outer: for (Object e : c) {
            for (int i = 0; i < size; i++) {
                if (array[i].equals(e)) {
                    continue outer;
                }
            }

            return false;
        }

        return true;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException("Feel free to implement if required");
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("Feel free to implement if required");
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("Feel free to implement if required");
    }

    @Override
    public void clear() {
        System.arraycopy(nullArray, 0, array, 0, size);
        size = 0;
    }
}
