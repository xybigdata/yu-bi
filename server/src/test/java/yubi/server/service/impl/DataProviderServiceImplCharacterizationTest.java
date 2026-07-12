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

package yubi.server.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import yubi.core.base.PageInfo;
import yubi.core.base.consts.Const;
import yubi.core.base.consts.ValueType;
import yubi.core.base.consts.VariableTypeEnum;
import yubi.core.common.RequestContext;
import yubi.core.data.provider.DataProviderManager;
import yubi.core.data.provider.DataProviderSource;
import yubi.core.data.provider.Dataframe;
import yubi.core.data.provider.ExecuteParam;
import yubi.core.data.provider.QueryScript;
import yubi.core.data.provider.ScriptType;
import yubi.core.data.provider.ScriptVariable;
import yubi.core.data.provider.SelectColumn;
import yubi.core.entity.RelSubjectColumns;
import yubi.core.entity.Source;
import yubi.core.entity.User;
import yubi.core.entity.View;
import yubi.core.mappers.ext.RelSubjectColumnsMapperExt;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.dto.VariableValue;
import yubi.server.base.params.TestExecuteParam;
import yubi.server.base.params.ViewExecuteParam;
import yubi.server.service.VariableService;
import yubi.server.service.ViewService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataProviderServiceImplCharacterizationTest {

    private DataProviderManager dataProviderManager;

    private RelSubjectColumnsMapperExt columnPermissionMapper;

    private VariableService variableService;

    private ViewService viewService;

    private YuBiSecurityManager securityManager;

    private View view;

    private Source source;

    private TestDataProviderService service;

    @BeforeEach
    void setUp() throws Exception {
        dataProviderManager = mock(DataProviderManager.class);
        columnPermissionMapper = mock(RelSubjectColumnsMapperExt.class);
        variableService = mock(VariableService.class);
        viewService = mock(ViewService.class);
        securityManager = mock(YuBiSecurityManager.class);

        view = new View();
        view.setId("view-1");
        view.setOrgId("org-1");
        view.setSourceId("source-1");
        view.setScript("select amount, region from orders");
        view.setType("SQL");
        view.setModel("{\"columns\":{\"orders.amount\":{\"name\":[\"orders\",\"amount\"],\"type\":\"NUMERIC\"}}}");

        source = new Source();
        source.setId("source-1");
        source.setOrgId("org-1");
        source.setName("orders-source");
        source.setType("JDBC");
        source.setConfig("{\"serverAggregate\":true}");

        User user = new User();
        user.setId("user-1");
        user.setName("Alice");
        user.setUsername("alice");
        user.setEmail("alice@example.com");

        when(securityManager.getCurrentUser()).thenReturn(user);
        when(securityManager.isOrgOwner("org-1")).thenReturn(false);
        when(variableService.listOrgValue("org-1")).thenReturn(List.of(
                variable("region_policy", VariableTypeEnum.PERMISSION, ValueType.STRING, Set.of("east"), false)));
        when(variableService.listViewVarValuesByUser("user-1", "view-1")).thenReturn(List.of(
                variable("status", VariableTypeEnum.QUERY, ValueType.STRING, Set.of("default"), true)));

        RelSubjectColumns columnPermission = new RelSubjectColumns();
        columnPermission.setColumnPermission("[\"orders.amount\",\"customers.region\"]");
        when(columnPermissionMapper.listByUser("view-1", "user-1")).thenReturn(List.of(columnPermission));

        service = new TestDataProviderService(
                dataProviderManager,
                columnPermissionMapper,
                variableService,
                viewService,
                view,
                source);
        service.setSecurityManager(securityManager);
        service.init();
        when(dataProviderManager.execute(any(), any(), any())).thenReturn(service.providerResult);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clean();
    }

    @Test
    void shouldCharacterizeAuthenticatedQueryPermissionsVariablesAndProviderRequest() throws Exception {
        ViewExecuteParam request = executableRequest();
        request.setScript(true);
        request.setParams(Map.of("status", Set.of("paid")));

        Dataframe result = service.execute(request);

        ArgumentCaptor<DataProviderSource> sourceCaptor = ArgumentCaptor.forClass(DataProviderSource.class);
        ArgumentCaptor<QueryScript> scriptCaptor = ArgumentCaptor.forClass(QueryScript.class);
        ArgumentCaptor<ExecuteParam> executeCaptor = ArgumentCaptor.forClass(ExecuteParam.class);
        verify(dataProviderManager).execute(sourceCaptor.capture(), scriptCaptor.capture(), executeCaptor.capture());

        assertSame(service.providerResult, result);
        assertTrue(service.viewPermissionCheck);
        assertFalse(service.sourcePermissionCheck);
        assertEquals(Boolean.TRUE, RequestContext.getScriptPermission());
        assertEquals("select amount, region from orders", result.getScript());

        DataProviderSource providerSource = sourceCaptor.getValue();
        assertEquals("source-1", providerSource.getSourceId());
        assertEquals("JDBC", providerSource.getType());
        assertEquals(Boolean.TRUE, providerSource.getProperties().get("serverAggregate"));

        QueryScript queryScript = scriptCaptor.getValue();
        assertFalse(queryScript.isTest());
        assertEquals("source-1", queryScript.getSourceId());
        assertEquals(Set.of("orders.amount"), queryScript.getSchema().keySet());

        Map<String, ScriptVariable> variables = queryScript.getVariables().stream()
                .collect(Collectors.toMap(ScriptVariable::getName, Function.identity()));
        assertEquals(Set.of("Alice"), variables.get("YUBI_USER_NAME").getValues());
        assertEquals(Set.of("alice"), variables.get("YUBI_USER_USERNAME").getValues());
        assertEquals(Set.of("alice@example.com"), variables.get("YUBI_USER_EMAIL").getValues());
        assertEquals(Set.of("user-1"), variables.get("YUBI_USER_ID").getValues());
        assertEquals(Set.of("east"), variables.get("region_policy").getValues());
        assertFalse(variables.get("region_policy").isDisabled());
        assertEquals(Set.of("paid"), variables.get("status").getValues());
        assertEquals(ValueType.STRING, variables.get("status").getValueType());

        ExecuteParam executeParam = executeCaptor.getValue();
        assertEquals(1L, executeParam.getPageInfo().getPageNo());
        assertEquals(1000L, executeParam.getPageInfo().getPageSize());
        assertTrue(executeParam.isServerAggregate());
        assertEquals(Set.of("customers.region", "orders.amount"), columnKeys(executeParam.getIncludeColumns()));
        verify(viewService).requirePermission(view, Const.MANAGE);
        verify(columnPermissionMapper).listByUser("view-1", "user-1");
    }

    @Test
    void shouldCharacterizeOwnerWildcardAndDisabledPermissionVariables() throws Exception {
        when(securityManager.isOrgOwner("org-1")).thenReturn(true);

        service.execute(executableRequest());

        ArgumentCaptor<QueryScript> scriptCaptor = ArgumentCaptor.forClass(QueryScript.class);
        ArgumentCaptor<ExecuteParam> executeCaptor = ArgumentCaptor.forClass(ExecuteParam.class);
        verify(dataProviderManager).execute(any(), scriptCaptor.capture(), executeCaptor.capture());

        List<ScriptVariable> permissionVariables = scriptCaptor.getValue().getVariables().stream()
                .filter(variable -> VariableTypeEnum.PERMISSION.equals(variable.getType()))
                .toList();
        assertFalse(permissionVariables.isEmpty());
        assertTrue(permissionVariables.stream().allMatch(ScriptVariable::isDisabled));
        assertEquals(Set.of("*"), columnKeys(executeCaptor.getValue().getIncludeColumns()));
        verify(columnPermissionMapper, never()).listByUser(any(), any());
    }

    @Test
    void shouldCharacterizePageNormalization() throws Exception {
        ViewExecuteParam request = executableRequest();
        request.setPageInfo(PageInfo.builder()
                .pageNo(0L)
                .pageSize((long) Integer.MAX_VALUE + 100L)
                .countTotal(true)
                .build());

        service.execute(request);

        ArgumentCaptor<ExecuteParam> executeCaptor = ArgumentCaptor.forClass(ExecuteParam.class);
        verify(dataProviderManager).execute(any(), any(), executeCaptor.capture());
        assertEquals(1L, executeCaptor.getValue().getPageInfo().getPageNo());
        assertEquals(Integer.MAX_VALUE, executeCaptor.getValue().getPageInfo().getPageSize());
        assertTrue(executeCaptor.getValue().getPageInfo().isCountTotal());
    }

    @Test
    void shouldCharacterizeScriptHidingWithoutManagePermission() throws Exception {
        doThrow(new RuntimeException("manage denied"))
                .when(viewService)
                .requirePermission(view, Const.MANAGE);
        ViewExecuteParam request = executableRequest();
        request.setScript(true);

        Dataframe result = service.execute(request);

        assertNull(result.getScript());
        assertEquals(Boolean.FALSE, RequestContext.getScriptPermission());
    }

    @Test
    void shouldPropagateProviderFailureWithoutTranslation() throws Exception {
        Exception providerFailure = new Exception("provider unavailable");
        when(dataProviderManager.execute(any(), any(), any())).thenThrow(providerFailure);

        Exception thrown = assertThrows(Exception.class, () -> service.execute(executableRequest()));

        assertSame(providerFailure, thrown);
    }

    @Test
    void shouldCharacterizePreviewQueryExecution() throws Exception {
        ScriptVariable requestVariable = new ScriptVariable(
                "preview_filter",
                VariableTypeEnum.QUERY,
                ValueType.STRING,
                Set.of("amount > 10"),
                true);
        TestExecuteParam request = new TestExecuteParam();
        request.setSourceId("source-1");
        request.setScript("select * from orders where $preview_filter$");
        request.setScriptType(ScriptType.SQL);
        request.setVariables(List.of(requestVariable));
        request.setSize(0);

        service.testExecute(request);

        ArgumentCaptor<QueryScript> scriptCaptor = ArgumentCaptor.forClass(QueryScript.class);
        ArgumentCaptor<ExecuteParam> executeCaptor = ArgumentCaptor.forClass(ExecuteParam.class);
        verify(dataProviderManager).execute(any(), scriptCaptor.capture(), executeCaptor.capture());
        assertTrue(service.sourcePermissionCheck);
        assertTrue(scriptCaptor.getValue().isTest());
        assertEquals("source-1", scriptCaptor.getValue().getSourceId());
        ScriptVariable previewVariable = scriptCaptor.getValue().getVariables().stream()
                .filter(variable -> "preview_filter".equals(variable.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(ValueType.FRAGMENT, previewVariable.getValueType());
        assertEquals(1L, executeCaptor.getValue().getPageInfo().getPageNo());
        assertEquals(1L, executeCaptor.getValue().getPageInfo().getPageSize());
        assertFalse(executeCaptor.getValue().isCacheEnable());
        assertEquals(Set.of("*"), columnKeys(executeCaptor.getValue().getIncludeColumns()));
    }

    private ViewExecuteParam executableRequest() {
        ViewExecuteParam request = new ViewExecuteParam();
        request.setViewId("view-1");
        request.setColumns(List.of(SelectColumn.of("amount", "orders", "amount")));
        return request;
    }

    private static Dataframe dataframeWithScript() {
        Dataframe dataframe = new Dataframe("query-result");
        dataframe.setColumns(List.of());
        dataframe.setRows(List.of());
        dataframe.setScript("select amount, region from orders");
        return dataframe;
    }

    private static VariableValue variable(String name,
                                          VariableTypeEnum type,
                                          ValueType valueType,
                                          Set<String> values,
                                          boolean expression) {
        VariableValue variable = new VariableValue();
        variable.setName(name);
        variable.setType(type.name());
        variable.setValueType(valueType.name());
        variable.setValues(values);
        variable.setExpression(expression);
        return variable;
    }

    private static Set<String> columnKeys(Collection<SelectColumn> columns) {
        return columns.stream()
                .map(SelectColumn::getColumnKey)
                .collect(Collectors.toSet());
    }

    private static final class TestDataProviderService extends DataProviderServiceImpl {

        private final View view;

        private final Source source;

        private final Dataframe providerResult;

        private boolean viewPermissionCheck;

        private boolean sourcePermissionCheck;

        private TestDataProviderService(DataProviderManager dataProviderManager,
                                        RelSubjectColumnsMapperExt columnPermissionMapper,
                                        VariableService variableService,
                                        ViewService viewService,
                                        View view,
                                        Source source) {
            super(dataProviderManager, columnPermissionMapper, variableService, viewService);
            this.view = view;
            this.source = source;
            this.providerResult = dataframeWithScript();
        }

        @Override
        public <T> T retrieve(String id, Class<T> type, boolean checkPermission) {
            if (View.class.equals(type)) {
                viewPermissionCheck = checkPermission;
                return type.cast(view);
            }
            if (Source.class.equals(type)) {
                sourcePermissionCheck = checkPermission;
                return type.cast(source);
            }
            throw new IllegalArgumentException("Unexpected entity type: " + type.getName());
        }
    }
}
