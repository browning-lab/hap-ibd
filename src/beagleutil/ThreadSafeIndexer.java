/*
 * Copyright 2019 Brian L. Browning
 *
 * This file is part of the HapIbd program.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package beagleutil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Class {@code ThreadSafeIndexer} indexes objects.
 * </p>
 * Instances of class {@code ThreadSafeIndexer} are thread-safe.
 *
 * @param <T> the type parameter.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class ThreadSafeIndexer<T> {

    /**
     * The default initial capacity, which is 500.
     */
    public static final int DEFAULT_INIT_CAPACITY = 500;

    private final List<T> list ;
    private final Map<T, Integer> map;

    /**
     * Creates a new {@code ThreadSafeIndexer} instance with the default
     * initial capacity.
     *
     * @see #DEFAULT_INIT_CAPACITY
     */
    public ThreadSafeIndexer() {
        this(DEFAULT_INIT_CAPACITY);
    }

    /**
     * Creates a new {@code ThreadSafeIndexer}instance with the specified
     * initial capacity.
     * @param initCapacity the initial capacity
     * @throws IllegalArgumentException if {@code initCapacity < 1}
     */
    public ThreadSafeIndexer(int initCapacity) {
        if (initCapacity < 1) {
            throw new IllegalArgumentException(String.valueOf(initCapacity));
        }
        this.list = new ArrayList<>(initCapacity);
        this.map = new HashMap<>(initCapacity);
    }

    /**
     * Returns the index of the specified object.  If the object
     * is not yet indexed, the object will be indexed. Indices
     * are assigned in consecutive order beginning with 0.
     * @param object the object whose index will be retrieved
     * @return the index of the specified object
     * @throws NullPointerException if {@code object==null}
     */
    public synchronized int getIndex(T object) {
        if (object==null) {
            throw new NullPointerException();
        }
        if (map.keySet().contains(object)) {
            return map.get(object);
        }
        else {
            int idIndex = list.size();
            list.add(object);
            map.put(object, idIndex);
            return idIndex;
        }
    }

    /**
     * Returns an array of object indices corresponding to the specified
     * object array.  If an object is not yet indexed, the object will be
     * indexed.  Object indices are assigned in increasing order starting with 0.
     * @param objects an array of objects
     * @return an array of object identifier indices
     * @throws IllegalArgumentException if there is a {@code j} satisfying
     * {@code (0 <= j && j < objects.length) && objects[j].isEmpty()}
     * @throws NullPointerException if {@code objects == null}
     * @throws NullPointerException if there is a {@code j} satisfying
     * {@code (0 <= j && j < objects.length) && (objects[j] == null)}
     */
    public synchronized int[] getIndices(T[] objects) {
        int[] indices = new int[objects.length];
        for (int j=0; j<indices.length; ++j) {
            T object = objects[j];
            if (object==null) {
                throw new NullPointerException();
            }
            if (map.keySet().contains(object)) {
                indices[j] = map.get(object);
            }
            else {
                int idIndex = list.size();
                list.add(object);
                map.put(object, idIndex);
                indices[j] = idIndex;
            }
        }
        return indices;
    }

    /**
     * Returns the index of the specified object, or returns
     * {@code -1} if the specified object is not indexed.
     *
     * @param object an object
     * @return the index of the specified object, or
     * {@code -1} if the specified object is not indexed
     *
     * @throws NullPointerException if {@code object == null}.
     */
    public synchronized int getIndexIfIndexed(T object) {
        if (object==null) {
            throw new NullPointerException();
        }
        if (map.keySet().contains(object)) {
            return map.get(object);
        }
        else {
            return -1;
        }
    }

    /**
     * Returns the number of indexed objects.
     * @return the number of indexed objects
     */
    public synchronized int size() {
        return list.size();
    }

    /**
     * Returns the object with the specified index.
     * @param index an object index
     * @return the object with the specified index
     * @throws IndexOutOfBoundsException if
     * {@code  index < 0 || index >= this.size()}
     */
    public synchronized T item(int index) {
        return list.get(index);
    }

    /**
     * Returns a list of objects with the specified indices.
     * @param indices an array of object indices
     * @return a list of objects with the specified indices
     * @throws IndexOutOfBoundsException if there exists a {@code j} satisfying
     * {@code (0 <= j && j < indices.length)
     * && (indices[j] < 0 || indices[j] >= this.size())}
     */
    public synchronized List<T> items(int[] indices) {
        List<T> items = new ArrayList<>(indices.length);
        for (int index : indices) {
            items.add(list.get(index));
        }
        return items;
    }

    /**
     * Returns an listed of all indexed objects. The returned list will
     * have size {@code this.size()}, and it will satisfy
     * {@code this.items().get(k).equals(this.item(k))==true}
     * for {@code  0 <= k && k < this.size()}
     *
     * @return an array of objects
     */
    public synchronized List<T> items() {
        return new ArrayList<>(list);
    }

    /**
     * Returns {@code this.items().toString()}.
     * @return a string representation of {@code this}
     */
    @Override
    public synchronized String toString() {
        return this.items().toString();
    }
}
