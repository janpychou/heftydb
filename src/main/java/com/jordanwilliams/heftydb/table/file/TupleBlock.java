/*
 * Copyright (c) 2013. Jordan Williams
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jordanwilliams.heftydb.table.file;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import com.jordanwilliams.heftydb.data.Key;
import com.jordanwilliams.heftydb.data.Tuple;
import com.jordanwilliams.heftydb.offheap.ByteMap;
import com.jordanwilliams.heftydb.offheap.Memory;
import com.jordanwilliams.heftydb.offheap.Offheap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TupleBlock implements Iterable<Tuple>, Offheap {

    public static class Cache {

        private final com.google.common.cache.Cache<String, TupleBlock> cache;

        public Cache(long maxsize) {
            cache = CacheBuilder.newBuilder().concurrencyLevel(64).weigher(new Weigher<String, TupleBlock>() {
                @Override
                public int weigh(String key, TupleBlock value) {
                    return key.length() + value.memory().size();
                }
            }).removalListener(new RemovalListener<String, TupleBlock>() {
                @Override
                public void onRemoval(RemovalNotification<String, TupleBlock> removalNotification) {
                    removalNotification.getValue().memory().release();
                }
            }).maximumWeight(maxsize).build();
        }

        public Cache() {
            this(1024000);
        }

        public TupleBlock get(long tableId, long offset) {
            return cache.getIfPresent(key(tableId, offset));
        }

        public void put(long tableId, long offset, TupleBlock tupleBlock) {
            cache.put(key(tableId, offset), tupleBlock);
        }

        private String key(long tableId, long offset) {
            return new StringBuilder().append(tableId).append(offset).toString();
        }

        public void clear() {
            cache.invalidateAll();
        }
    }

    public static class Builder {

        private final ByteMap.Builder byteMapBuilder = new ByteMap.Builder();
        private int size;

        public void addRecord(Tuple tuple) {
            byteMapBuilder.add(new Key(tuple.key().data(), tuple.key().snapshotId()), tuple.value());
            size += tuple.size();
        }

        public int size() {
            return size;
        }

        public TupleBlock build() {
            return new TupleBlock(byteMapBuilder.build());
        }
    }

    private class TupleIterator implements Iterator<Tuple> {

        private final Iterator<ByteMap.Entry> entryIterator;

        private TupleIterator(Iterator<ByteMap.Entry> entryIterator) {
            this.entryIterator = entryIterator;
        }

        @Override
        public boolean hasNext() {
            return entryIterator.hasNext();
        }

        @Override
        public Tuple next() {
            ByteMap.Entry nextEntry = entryIterator.next();
            return new Tuple(nextEntry.key(), nextEntry.value());
        }

        @Override
        public void remove() {
            entryIterator.remove();
        }
    }

    private final ByteMap byteMap;

    public TupleBlock(ByteMap byteMap) {
        this.byteMap = byteMap;
    }

    public Tuple get(Key key) {
        int closestIndex = byteMap.floorIndex(key);

        if (closestIndex < 0 || closestIndex >= byteMap.entryCount()) {
            return null;
        }

        Tuple closestTuple = deserialize(closestIndex);
        return closestTuple.key().data().equals(key.data()) ? closestTuple : null;
    }

    public Tuple first() {
        return deserialize(0);
    }

    public Iterator<Tuple> ascendingIterator() {
        return new TupleIterator(byteMap.ascendingIterator());
    }

    public Iterator<Tuple> ascendingIterator(Key key) {
        return new TupleIterator(byteMap.ascendingIterator(key));
    }

    public Iterator<Tuple> descendingIterator() {
        return new TupleIterator(byteMap.descendingIterator());
    }

    public Iterator<Tuple> descendingIterator(Key key) {
        return new TupleIterator(byteMap.descendingIterator(key));
    }

    @Override
    public Iterator<Tuple> iterator() {
        return new TupleIterator(byteMap.ascendingIterator());
    }

    @Override
    public Memory memory() {
        return byteMap.memory();
    }

    @Override
    public String toString() {
        List<Tuple> tuples = new ArrayList<Tuple>();
        for (Tuple tuple : this) {
            tuples.add(tuple);
        }

        return "TupleBlock{tuples=" + tuples + "}";
    }

    private Tuple deserialize(int index) {
        ByteMap.Entry entry = byteMap.get(index);
        return new Tuple(entry.key(), entry.value());
    }
}