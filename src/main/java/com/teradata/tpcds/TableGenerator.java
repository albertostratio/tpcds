/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.teradata.tpcds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.teradata.tpcds.Results.constructResults;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class TableGenerator
{
    private final Session session;

    public TableGenerator(Session session)
    {
        this.session = requireNonNull(session, "session is null");
    }

    public void generateTable(Table table, long startingRowNumber, long endingRowNumber)
    {
        // If this is a child table, and not being generated by itself, then it will get generated as part of the parent table generation.
        if (table.isChild() && !session.hasTable()) {
            return;
        }

        List<OutputStreamWriter> fileWriters = new ArrayList<>();
        try {
            addFileWritersForTableAndChildren(fileWriters, table);
            Results results = constructResults(table, startingRowNumber, endingRowNumber, session);
            for (List<String> parentAndChildRows : results) {
                for (int i = 0; i < parentAndChildRows.size(); i++) {
                    fileWriters.get(i).write(parentAndChildRows.get(i));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            for (OutputStreamWriter fileWriter : fileWriters) {
                try {
                    fileWriter.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addFileWritersForTableAndChildren(List<OutputStreamWriter> fileWriters, Table table)
            throws IOException
    {
        OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(getPath(table), true), StandardCharsets.ISO_8859_1);
        fileWriters.add(fileWriter);
        if (table.hasChild() && !session.hasTable()) {
            addFileWritersForTableAndChildren(fileWriters, table.getChild());
        }
    }

    private String getPath(Table table)
    {
        // TODO: path names for update and parallel cases
        return format("%s%s%s%s",
                session.getTargetDirectory(),
                File.separator,
                table.toString(),
                session.getSuffix());
    }
}
