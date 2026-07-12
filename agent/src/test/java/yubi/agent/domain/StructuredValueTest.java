package yubi.agent.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructuredValueTest {

    @Test
    void shouldDeeplyFreezeStructuredContainers() {
        List<StructuredValue> sourceList = new ArrayList<>(List.of(StructuredValue.text("one")));
        Map<String, StructuredValue> sourceMap = new LinkedHashMap<>();
        sourceMap.put("items", StructuredValue.array(sourceList));

        var value = StructuredValue.object(sourceMap);
        sourceList.add(StructuredValue.text("two"));
        sourceMap.clear();

        var items = (StructuredValue.ArrayValue) value.values().get("items");
        assertEquals(1, items.values().size());
        assertThrows(UnsupportedOperationException.class, value.values()::clear);
        assertThrows(UnsupportedOperationException.class, items.values()::clear);
    }
}
