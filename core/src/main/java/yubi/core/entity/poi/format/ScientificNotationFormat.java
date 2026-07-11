package yubi.core.entity.poi.format;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ScientificNotationFormat extends PoiNumFormat {

    public static final String type = "scientificNotation";

    @Override
    public String getFormat() {
        return this.getDecimalPlaces() + "E+0";
    }
}
