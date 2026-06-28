package yubi.core.rename;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for the project rename replacement rules.
 * These are example-based tests validating specific rename transformations.
 *
 * Validates: Requirements 2.3, 4.2, 4.4, 11.1, 11.2
 */
class RenameExampleTest {

    // --- Replacement rule functions (simulating the rename engine) ---

    /**
     * Replaces mainClass references from old to new fully-qualified class name.
     */
    private String replaceMainClass(String input) {
        return input.replace("datart.DatartServerApplication", "yubi.YuBiServerApplication");
    }

    /**
     * Replaces Maven groupId from "datart" to "yubi".
     */
    private String replaceGroupId(String input) {
        return input.replace("<groupId>datart</groupId>", "<groupId>yubi</groupId>");
    }

    /**
     * Replaces Maven artifactId from "datart-{suffix}" to "yu-bi-{suffix}".
     */
    private String replaceArtifactId(String input) {
        Pattern pattern = Pattern.compile("datart-([a-z][a-z0-9-]*)");
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll("yu-bi-$1");
    }

    /**
     * Replaces shell function names from "datart_*" to "yubi_*".
     */
    private String replaceShellFunctionName(String input) {
        // Replace function definitions and calls: datart_xxx -> yubi_xxx
        return input.replaceAll("\\bdatart_(\\w+)", "yubi_$1");
    }

    /**
     * Replaces environment variable prefix from "DATART_" to "YUBI_".
     */
    private String replaceEnvVarPrefix(String input) {
        return input.replaceAll("\\bDATART_(\\w+)", "YUBI_$1");
    }

    /**
     * Replaces version number from "1.0.0-rc.3" or "1.0.0" to "2.0.0".
     */
    private String replaceVersion(String input) {
        String result = input.replace("1.0.0-rc.3", "2.0.0");
        // Only replace standalone "1.0.0" (in version context), not partial matches
        result = result.replaceAll("(?<!\\.)(\"?)1\\.0\\.0(\"?)(?![-.]\\w)", "$12.0.0$2");
        return result;
    }

    /**
     * Replaces REALM_NAME constant value from "datart" to "yubi".
     */
    private String replaceRealmName(String input) {
        return input.replace("REALM_NAME = \"datart\"", "REALM_NAME = \"yubi\"");
    }

    // --- Test Cases ---

    @Test
    @DisplayName("mainClass replacement: datart.DatartServerApplication -> yubi.YuBiServerApplication")
    void mainClassReplacement() {
        // Maven pom.xml context
        String pomInput = "<mainClass>datart.DatartServerApplication</mainClass>";
        String pomExpected = "<mainClass>yubi.YuBiServerApplication</mainClass>";
        assertThat(replaceMainClass(pomInput)).isEqualTo(pomExpected);

        // Shell script context
        String shellInput = "START_CLASS=\"datart.DatartServerApplication\"";
        String shellExpected = "START_CLASS=\"yubi.YuBiServerApplication\"";
        assertThat(replaceMainClass(shellInput)).isEqualTo(shellExpected);

        // Dockerfile context
        String dockerInput = "ENTRYPOINT [\"java\", \"-cp\", \"lib/*\", \"datart.DatartServerApplication\"]";
        String dockerExpected = "ENTRYPOINT [\"java\", \"-cp\", \"lib/*\", \"yubi.YuBiServerApplication\"]";
        assertThat(replaceMainClass(dockerInput)).isEqualTo(dockerExpected);
    }

    @Test
    @DisplayName("groupId replacement: datart -> yubi")
    void groupIdReplacement() {
        String input = "<groupId>datart</groupId>";
        String expected = "<groupId>yubi</groupId>";
        assertThat(replaceGroupId(input)).isEqualTo(expected);

        // Parent reference
        String parentInput = "<parent>\n  <groupId>datart</groupId>\n  <artifactId>yu-bi-parent</artifactId>\n</parent>";
        String parentExpected = "<parent>\n  <groupId>yubi</groupId>\n  <artifactId>yu-bi-parent</artifactId>\n</parent>";
        assertThat(replaceGroupId(parentInput)).isEqualTo(parentExpected);

        // Dependency reference
        String depInput = "<dependency>\n  <groupId>datart</groupId>\n  <artifactId>yu-bi-core</artifactId>\n</dependency>";
        String depExpected = "<dependency>\n  <groupId>yubi</groupId>\n  <artifactId>yu-bi-core</artifactId>\n</dependency>";
        assertThat(replaceGroupId(depInput)).isEqualTo(depExpected);
    }

    @Test
    @DisplayName("artifactId replacement: datart-{module} -> yu-bi-{module}")
    void artifactIdReplacement() {
        assertThat(replaceArtifactId("datart-core")).isEqualTo("yu-bi-core");
        assertThat(replaceArtifactId("datart-server")).isEqualTo("yu-bi-server");
        assertThat(replaceArtifactId("datart-parent")).isEqualTo("yu-bi-parent");
        assertThat(replaceArtifactId("datart-data-provider-base")).isEqualTo("yu-bi-data-provider-base");

        // In XML context
        String xmlInput = "<artifactId>datart-core</artifactId>";
        String xmlExpected = "<artifactId>yu-bi-core</artifactId>";
        assertThat(replaceArtifactId(xmlInput)).isEqualTo(xmlExpected);
    }

    @Test
    @DisplayName("shell function name replacement: datart_start/stop/status -> yubi_start/stop/status")
    void shellFunctionNameReplacement() {
        // Function definitions
        assertThat(replaceShellFunctionName("datart_start(){")).isEqualTo("yubi_start(){");
        assertThat(replaceShellFunctionName("datart_stop(){")).isEqualTo("yubi_stop(){");
        assertThat(replaceShellFunctionName("datart_status(){")).isEqualTo("yubi_status(){");

        // Function calls (must also be renamed consistently)
        assertThat(replaceShellFunctionName("    datart_start")).isEqualTo("    yubi_start");
        assertThat(replaceShellFunctionName("    datart_stop")).isEqualTo("    yubi_stop");
        assertThat(replaceShellFunctionName("    datart_status >/dev/null 2>&1"))
                .isEqualTo("    yubi_status >/dev/null 2>&1");

        // Full script snippet: definition and call must be consistent
        String scriptSnippet = """
                datart_status(){
                    result=`ps -ef | grep 'DatartServerApplication' | wc -l`
                    return $result
                }
                datart_start(){
                    datart_status >/dev/null 2>&1
                    if [[ $? -eq 0 ]]; then
                        nohup java -cp "lib/*" "datart.DatartServerApplication" &
                    fi
                }
                datart_stop(){
                    datart_status >/dev/null 2>&1
                }""";

        String renamedSnippet = replaceShellFunctionName(scriptSnippet);

        // All definitions replaced
        assertThat(renamedSnippet).contains("yubi_status(){");
        assertThat(renamedSnippet).contains("yubi_start(){");
        assertThat(renamedSnippet).contains("yubi_stop(){");

        // All call points replaced
        assertThat(renamedSnippet).contains("    yubi_status >/dev/null 2>&1");

        // No old function names remain
        assertThat(renamedSnippet).doesNotContain("datart_status");
        assertThat(renamedSnippet).doesNotContain("datart_start");
        assertThat(renamedSnippet).doesNotContain("datart_stop");
    }

    @Test
    @DisplayName("environment variable prefix replacement: DATART_* -> YUBI_*")
    void envVarPrefixReplacement() {
        // Variable definitions
        assertThat(replaceEnvVarPrefix("DATART_DEMO_PORT=8081")).isEqualTo("YUBI_DEMO_PORT=8081");
        assertThat(replaceEnvVarPrefix("DATART_HOME=/opt/yubi")).isEqualTo("YUBI_HOME=/opt/yubi");

        // Variable references with ${...} syntax
        assertThat(replaceEnvVarPrefix("port=${DATART_DEMO_PORT:-8080}"))
                .isEqualTo("port=${YUBI_DEMO_PORT:-8080}");

        // Multiple env vars in the same line
        String multiInput = "export DATART_HOME=/opt DATART_PORT=8080";
        String multiExpected = "export YUBI_HOME=/opt YUBI_PORT=8080";
        assertThat(replaceEnvVarPrefix(multiInput)).isEqualTo(multiExpected);
    }

    @Test
    @DisplayName("version reset: 1.0.0-rc.3 -> 2.0.0 and 1.0.0 -> 2.0.0")
    void versionReset() {
        // Maven version with release candidate suffix
        String rcVersion = "<version>1.0.0-rc.3</version>";
        String rcExpected = "<version>2.0.0</version>";
        assertThat(replaceVersion(rcVersion)).isEqualTo(rcExpected);

        // Maven parent version reference
        String parentVersion = "<parent>\n  <version>1.0.0-rc.3</version>\n</parent>";
        String parentExpected = "<parent>\n  <version>2.0.0</version>\n</parent>";
        assertThat(replaceVersion(parentVersion)).isEqualTo(parentExpected);

        // package.json version (no rc suffix)
        String packageJson = "\"version\": \"1.0.0\"";
        String packageExpected = "\"version\": \"2.0.0\"";
        assertThat(replaceVersion(packageJson)).isEqualTo(packageExpected);

        // Both formats in the same document
        String mixed = "Backend: 1.0.0-rc.3, Frontend: \"1.0.0\"";
        String mixedExpected = "Backend: 2.0.0, Frontend: \"2.0.0\"";
        assertThat(replaceVersion(mixed)).isEqualTo(mixedExpected);
    }

    @Test
    @DisplayName("REALM_NAME constant replacement: \"datart\" -> \"yubi\"")
    void realmNameReplacement() {
        // In Java source code - field definition
        String fieldDef = "    private static final String REALM_NAME = \"datart\";";
        String fieldExpected = "    private static final String REALM_NAME = \"yubi\";";
        assertThat(replaceRealmName(fieldDef)).isEqualTo(fieldExpected);

        // In authentication provider
        String authProvider = "public static final String REALM_NAME = \"datart\";";
        String authExpected = "public static final String REALM_NAME = \"yubi\";";
        assertThat(replaceRealmName(authProvider)).isEqualTo(authExpected);
    }

    @Test
    @DisplayName("YAML syntax remains valid after replacement")
    void yamlSyntaxAfterReplacement() {
        // Simulate a YAML config that had datart: prefix replaced with yubi:
        String yamlContent = """
                spring:
                  datasource:
                    url: jdbc:mysql://localhost:3306/yubi?allowMultiQueries=true
                    username: root
                    password: 123456

                yubi:
                  migration:
                    enable: true
                  server:
                    address: ${yubi.address:http://127.0.0.1:8080}
                  user:
                    register: true
                    active:
                      send-mail: ${yubi.send-mail:false}
                      expire-hours: ${yubi.register.expire-hours:48}
                  security:
                    token:
                      timeout-min: 30

                server:
                  port: 8080
                """;

        // Verify the YAML parses correctly without exceptions
        Yaml yaml = new Yaml();
        assertThatNoException().isThrownBy(() -> {
            Map<String, Object> parsed = yaml.load(yamlContent);
            assertThat(parsed).isNotNull();
            assertThat(parsed).containsKey("yubi");
            assertThat(parsed).containsKey("spring");
            assertThat(parsed).containsKey("server");
        });

        // Verify key paths are accessible after rename
        Map<String, Object> parsed = yaml.load(yamlContent);
        @SuppressWarnings("unchecked")
        Map<String, Object> yubiConfig = (Map<String, Object>) parsed.get("yubi");
        assertThat(yubiConfig).containsKey("migration");
        assertThat(yubiConfig).containsKey("server");
        assertThat(yubiConfig).containsKey("user");
        assertThat(yubiConfig).containsKey("security");
    }
}
