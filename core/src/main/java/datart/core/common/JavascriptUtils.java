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

package datart.core.common;

import datart.core.base.exception.Exceptions;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JavascriptUtils {

    static final String SCRIPT_ENGINE_PROPERTY = "datart.script.engine";

    static final String SCRIPT_ENGINE_ENV = "DATART_SCRIPT_ENGINE";

    private static final List<String> DEFAULT_ENGINE_NAMES = List.of(
            "graal.js",
            "js",
            "JavaScript",
            "javascript",
            "nashorn"
    );

    public static Object invoke(Invocable invocable, String functionName, Object... args) throws Exception {
        if (invocable != null) {
            return invocable.invokeFunction(functionName, args);
        }
        return null;
    }

    public static Invocable load(String path) throws IOException, ScriptException {
        InputStream stream = JavascriptUtils.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            Exceptions.notFound(path);
        }
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            ScriptEngine engine = createScriptEngine();
            engine.eval(reader);
            if (engine instanceof Invocable) {
                return (Invocable) engine;
            }
            return null;
        }
    }

    private static ScriptEngine createScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager(JavascriptUtils.class.getClassLoader());
        String configuredEngineName = resolveConfiguredEngineName();
        for (String engineName : resolveCandidateEngineNames(configuredEngineName)) {
            ScriptEngine engine = manager.getEngineByName(engineName);
            if (engine != null) {
                return engine;
            }
        }

        String availableEngines = manager.getEngineFactories().stream()
                .map(JavascriptUtils::describeEngine)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));

        if (configuredEngineName != null) {
            Exceptions.base(String.format(
                    "未找到已配置的脚本引擎 [%s]，当前可用引擎：%s",
                    configuredEngineName,
                    availableEngines.isEmpty() ? "无" : availableEngines
            ));
        }

        Exceptions.base(String.format(
                "未找到可用的 JavaScript 脚本引擎，请安装 Nashorn 或 GraalJS。当前可用引擎：%s",
                availableEngines.isEmpty() ? "无" : availableEngines
        ));
        return null;
    }

    private static Set<String> resolveCandidateEngineNames(String configuredEngineName) {
        LinkedHashSet<String> engineNames = new LinkedHashSet<>();
        if (configuredEngineName != null) {
            engineNames.add(configuredEngineName);
            return engineNames;
        }
        engineNames.addAll(DEFAULT_ENGINE_NAMES);
        return engineNames;
    }

    private static String resolveConfiguredEngineName() {
        String configuredEngineName = System.getProperty(SCRIPT_ENGINE_PROPERTY);
        if (configuredEngineName == null || configuredEngineName.isBlank()) {
            configuredEngineName = System.getenv(SCRIPT_ENGINE_ENV);
        }
        if (configuredEngineName == null || configuredEngineName.isBlank()) {
            return null;
        }
        return configuredEngineName.trim();
    }

    private static String describeEngine(ScriptEngineFactory factory) {
        return factory.getEngineName() + " names=" + factory.getNames();
    }

}
