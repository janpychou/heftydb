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

package com.jordanwilliams.heftydb.write;

import com.jordanwilliams.heftydb.record.Record;
import com.jordanwilliams.heftydb.state.Files;

import java.util.Iterator;

public class FileTableWriter {

    private final long tableId;
    private final Files files;
    private final IndexWriter indexWriter;
    private final FilterWriter filterWriter;

    private FileTableWriter(long tableId, Files files, long approxRecordCount) {
        this.tableId = tableId;
        this.files = files;
        this.indexWriter = IndexWriter.open(tableId, files);
        this.filterWriter = FilterWriter.open(tableId, files, approxRecordCount);
    }

    public void write(Iterator<Record> records) {

    }

    public void finish() {

    }

    public static FileTableWriter open(long tableId, Files files, long approxRecordCount) {
        return new FileTableWriter(tableId, files, approxRecordCount);
    }
}
