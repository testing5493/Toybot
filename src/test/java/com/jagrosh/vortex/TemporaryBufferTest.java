package com.jagrosh.vortex;

import com.jagrosh.vortex.utils.TemporaryBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Iterator;

public class TemporaryBufferTest {
    @Test
    void testSize() {
        int[] sizeVals = new int[] {0, 4, 3, 190};

        for (int size : sizeVals) {
            TemporaryBuffer<Integer> buffer = new TemporaryBuffer<>(size);
            for (int i = 0; i < Math.min(40, size); i++) {
                buffer.add(i);
            }

            Assertions.assertEquals(Math.min(40, size), buffer.size());
        }
    }

    @Test
    void testAdd() {
        Integer[] expectedArray = new Integer[100];
        TemporaryBuffer<Integer> buffer = new TemporaryBuffer<Integer>(expectedArray.length);
        for (int i = 0; i < 100; i++) {
            expectedArray[i] = i;
            buffer.add(i);
        }

        Assertions.assertArrayEquals(expectedArray, buffer.toArray());
        Assertions.assertThrows(NullPointerException.class, () -> buffer.add(null));
    }

    @Test
    void testContains() {
        TemporaryBuffer<Integer> buffer = new TemporaryBuffer<>(10);
        buffer.add(19);
        buffer.add(403);
        buffer.add(3);
        buffer.add(4392);

        Assertions.assertTrue(buffer.contains(19));
        Assertions.assertTrue(buffer.contains(3));
        Assertions.assertTrue(buffer.contains(4392));
        Assertions.assertFalse(buffer.contains(-2394));
    }

    @Test
    @ValueSource
    void testGet() {
        int[] vals = new int[] {9432, 49, 9, 190, -19, 190};
        TemporaryBuffer<Integer> buffer = new TemporaryBuffer<>(vals.length);
        for (int val : vals) {
            buffer.add(val);
        }

        for (int i = 0; i < vals.length; i++) {
            Assertions.assertEquals(vals[i], buffer.get(i));
        }

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(vals.length));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(vals.length + 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(-1));
    }

    @Test
    void testIterator() {
        int[][] valsSet = new int[][]{{9432, 49, 9, 190, -19, 190}, {32}, {}};
        for (int[] vals : valsSet) {
            TemporaryBuffer<Integer> buffer = new TemporaryBuffer<>(vals.length);
            for (int val : vals) {
                buffer.add(val);
            }

            Iterator<Integer> iterator = buffer.iterator();
            for (int i = 0; iterator.hasNext(); i++) {
                Assertions.assertEquals(vals[i], iterator.next());
            }

            int i = 0;
            for (Integer n : buffer) {
                Assertions.assertEquals(vals[i++], n);
            }
        }
    }

    @Test
    void testEmptyIterator() {
        TemporaryBuffer<Integer> buffer = new TemporaryBuffer<>(4);
        Assertions.assertFalse(buffer.iterator().hasNext());
    }

    @Test
    void testToArray() {
        Integer[][] valueSets = new Integer[][]{
                {4, 432, 49029, 49, 24, 54},
                {34},
                {}
        };

        for (Integer[] valueSet : valueSets) {
            TemporaryBuffer<Integer> buffer = new TemporaryBuffer<>(valueSet.length);
            for (Integer n : valueSet) {
                buffer.add(n);
            }

            Assertions.assertArrayEquals(valueSet, buffer.toArray());
            Assertions.assertArrayEquals(valueSet, buffer.toArray(new Integer[valueSet.length]));

            for (int i = 0; i < 2; i++) {
                Integer[] resultArray = new Integer[valueSet.length + i];
                Assertions.assertSame(resultArray, buffer.toArray(resultArray));
            }

            if (valueSet.length != 0) {
                Integer[] smallResultArray = new Integer[valueSet.length - 1];
                Assertions.assertArrayEquals(valueSet, buffer.toArray(smallResultArray));
                Assertions.assertNotSame(smallResultArray, buffer.toArray(smallResultArray));
            }
        }
    }
}
