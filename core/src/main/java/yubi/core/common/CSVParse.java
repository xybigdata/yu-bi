/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package yubi.core.common;

import yubi.core.base.exception.Exceptions;
import lombok.Data;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CSVParse {

    private String path;

    public static CSVParse create(String path, ParseConfig parseConfig) {
        CSVParse csvParse = new CSVParse();
        csvParse.path = path;
        return csvParse;
    }

    public static CSVParse create(String path) {
        CSVParse csvParse = new CSVParse();
        csvParse.path = path;
        return csvParse;
    }

    public List<List<Object>> parse() throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            Exceptions.notFound(path);
        }
        List<CSVRecord> records = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.DEFAULT).getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        List<List<Object>> values = records.parallelStream().map(this::extractValues)
                .collect(Collectors.toList());
        // remove utf-8-with-bom char
        String start = values.get(0).get(0).toString();
        if (start.length() > 0 && start.charAt(0) == '\uFEFF') {
            values.get(0).set(0, start.substring(1));
        }
        return values;
    }

    private List<Object> extractValues(CSVRecord record) {
        if (record == null || record.size() == 0) {
            return Collections.emptyList();
        }
        LinkedList<Object> values = new LinkedList<>();
        for (int i = 0; i < record.size(); i++) {
            values.add(record.get(i));
        }
        return values;
    }

    @Data
    public static class ParseConfig {
        private String dateFormat;
    }

}
