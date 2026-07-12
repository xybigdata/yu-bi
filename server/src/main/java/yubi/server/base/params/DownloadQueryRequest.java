/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package yubi.server.base.params;

import lombok.Data;
import org.springframework.util.CollectionUtils;
import yubi.core.base.PageInfo;
import yubi.core.data.provider.SelectColumn;
import yubi.core.data.provider.sql.AggregateOperator;
import yubi.core.data.provider.sql.FilterOperator;
import yubi.core.data.provider.sql.FunctionColumn;
import yubi.core.data.provider.sql.GroupByOperator;
import yubi.core.data.provider.sql.OrderOperator;
import yubi.core.data.provider.sql.SelectKeyword;
import yubi.security.base.ResourceType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 下载和调度持久化载荷使用的查询请求，不作为 Web 查询契约暴露。 */
@Data
public class DownloadQueryRequest {

    private String vizId;

    private String vizName;

    private ResourceType vizType;

    private String viewId;

    private List<SelectKeyword> keywords;

    private List<SelectColumn> columns;

    private Map<String, Set<String>> params;

    private List<FunctionColumn> functionColumns;

    private List<AggregateOperator> aggregators;

    private List<FilterOperator> filters;

    private List<GroupByOperator> groups;

    private List<OrderOperator> orders;

    private PageInfo pageInfo;

    private boolean concurrencyControl;

    private String concurrencyControlMode;

    private boolean cache;

    private int cacheExpires;

    private boolean script;

    private boolean analytics;

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(columns)
                && CollectionUtils.isEmpty(aggregators)
                && CollectionUtils.isEmpty(groups);
    }

    public void setConcurrencyControl(Boolean concurrencyControl) {
        this.concurrencyControl = concurrencyControl != null && concurrencyControl;
    }

    public void setCache(Boolean cache) {
        this.cache = cache != null && cache;
    }

    public void setCacheExpires(Integer cacheExpires) {
        this.cacheExpires = cacheExpires == null ? 0 : cacheExpires;
    }

    public void setScript(Boolean script) {
        this.script = script != null && script;
    }

    public void setAnalytics(Boolean analytics) {
        this.analytics = analytics != null && analytics;
    }
}
