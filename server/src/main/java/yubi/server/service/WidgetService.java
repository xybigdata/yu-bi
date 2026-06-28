package yubi.server.service;

import yubi.core.entity.Widget;
import yubi.core.mappers.WidgetMapper;
import yubi.server.base.params.WidgetCreateParam;
import yubi.server.base.params.WidgetUpdateParam;

import java.util.List;

public interface WidgetService extends BaseCRUDService<Widget, WidgetMapper> {

    List<Widget> createWidgets(List<WidgetCreateParam> createParams);

    boolean updateWidgets(List<WidgetUpdateParam> updateParams);

    boolean deleteWidgets(List<String> widgetIds);

}
