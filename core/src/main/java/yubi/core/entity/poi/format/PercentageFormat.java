package yubi.core.entity.poi.format;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PercentageFormat extends PoiNumFormat {

    public static final String type = "percentage";

    @Override
    public String getFormat() {
        return this.getDecimalPlaces() + "%";
    }
}
