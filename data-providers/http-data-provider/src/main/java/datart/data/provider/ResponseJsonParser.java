/*
 * Datart
 * <p>
 * Copyright 2021
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
package datart.data.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import datart.core.base.consts.ValueType;
import datart.core.base.exception.BaseException;
import datart.core.base.exception.Exceptions;
import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;
import datart.data.provider.jdbc.DataTypeUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class ResponseJsonParser implements HttpResponseParser {

    private static final String PROPERTY_SPLIT = "\\.";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Dataframe parseResponse(String targetPropertyName, ClassicHttpResponse response, List<Column> columns) throws IOException {
        String jsonString;
        try {
            jsonString = EntityUtils.toString(response.getEntity());
        } catch (ParseException e) {
            throw new IOException("Failed to parse http response entity", e);
        }

        JsonNode arrayNode;
        if (StringUtils.isEmpty(targetPropertyName)) {
            arrayNode = OBJECT_MAPPER.readTree(jsonString);
        } else {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
            String[] split = targetPropertyName.split(PROPERTY_SPLIT);
            for (int i = 0; i < split.length - 1; i++) {
                jsonNode = jsonNode.path(split[i]);
                if (jsonNode.isMissingNode() || jsonNode.isNull() || !jsonNode.isObject()) {
                    Exceptions.tr(BaseException.class, "message.provider.http.property.miss", targetPropertyName);
                }
            }
            arrayNode = jsonNode.path(split[split.length - 1]);
            if (arrayNode.isMissingNode() || arrayNode.isNull()) {
                Exceptions.tr(BaseException.class, "message.provider.http.property.miss", targetPropertyName);
            }
        }
        Dataframe dataframe = new Dataframe();
        if (arrayNode == null || arrayNode.isNull() || !arrayNode.isArray() || arrayNode.size() == 0) {
            return dataframe;
        }

        if (CollectionUtils.isEmpty(columns)) {
            columns = getSchema(arrayNode.get(0));
        }

        dataframe.setColumns(columns);

        List<List<Object>> rows = new ArrayList<>();
        for (JsonNode itemNode : arrayNode) {
            JsonNode objectNode = itemNode.isObject() ? itemNode : OBJECT_MAPPER.createObjectNode();
            List<Object> row = columns.stream()
                .map(column -> {
                    JsonNode valueNode = objectNode.get(column.columnName());
                    return nodeToValue(valueNode);
                }).collect(Collectors.toList());
            rows.add(row);
        }
        dataframe.setRows(rows);
        return dataframe;
    }

    private ArrayList<Column> getSchema(JsonNode jsonObject) {
        ArrayList<Column> columns = new ArrayList<>();
        if (jsonObject == null || !jsonObject.isObject()) {
            return columns;
        }
        jsonObject.fieldNames().forEachRemaining(key -> {
            Column column = new Column();
            column.setName(key);
            Object val = nodeToValue(jsonObject.get(key));
            if (val != null) {
                column.setType(DataTypeUtils.javaType2DataType(val));
            } else {
                column.setType(ValueType.STRING);
            }
            columns.add(column);
        });
        return columns;
    }

    private Object nodeToValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull() || valueNode.isMissingNode()) {
            return null;
        }
        if (valueNode.isContainerNode()) {
            return valueNode.toString();
        }
        if (valueNode.isTextual()) {
            return valueNode.asText();
        }
        return OBJECT_MAPPER.convertValue(valueNode, Object.class);
    }

}
