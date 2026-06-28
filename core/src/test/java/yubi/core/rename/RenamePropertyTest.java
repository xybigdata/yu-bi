package yubi.core.rename;

import net.jqwik.api.*;
import net.jqwik.api.Tuple;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for the project rename logic (datart -> yubi).
 *
 * Validates: Requirements 1.1, 1.2, 1.4, 1.5, 3.4, 5.3, 7.1, 10.5, 10.6
 */
class RenamePropertyTest {

    private static final String OLD_PREFIX = "datart.";
    private static final String NEW_PREFIX = "yubi.";
    private static final String OLD_PASCAL_PREFIX = "Datart";
    private static final String NEW_PASCAL_PREFIX = "YuBi";

    /**
     * Applies the package path prefix rename rule:
     * replace "datart." prefix with "yubi." prefix.
     */
    private String applyPackageRename(String packagePath) {
        if (packagePath.startsWith(OLD_PREFIX)) {
            return NEW_PREFIX + packagePath.substring(OLD_PREFIX.length());
        }
        return packagePath;
    }

    /**
     * Feature: project-rename, Property 1: 包路径前缀替换保持后缀不变
     *
     * For any valid Java package path starting with "datart.", replacing the prefix
     * "datart." with "yubi." SHALL produce a result equal to "yubi." + original suffix,
     * where the suffix part is completely unchanged.
     *
     * Validates: Requirements 1.1, 1.2, 1.5, 3.4, 7.1
     */
    @Property(tries = 100)
    @Label("Feature: project-rename, Property 1: 包路径前缀替换保持后缀不变")
    void packagePrefixReplacementPreservesSuffix(@ForAll("validJavaPackageSuffixes") String suffix) {
        String originalPath = OLD_PREFIX + suffix;
        String expectedPath = NEW_PREFIX + suffix;

        String actualPath = applyPackageRename(originalPath);

        assertThat(actualPath)
                .as("Renaming '%s' should yield '%s'", originalPath, expectedPath)
                .isEqualTo(expectedPath);

        // Additionally verify the suffix portion is exactly preserved
        String actualSuffix = actualPath.substring(NEW_PREFIX.length());
        assertThat(actualSuffix)
                .as("Suffix after rename should be identical to original suffix")
                .isEqualTo(suffix);
    }

    /**
     * Applies the PascalCase identifier rename rule:
     * replace "Datart" prefix with "YuBi" prefix.
     */
    private String applyPascalCaseRename(String identifier) {
        if (identifier.startsWith(OLD_PASCAL_PREFIX)) {
            return NEW_PASCAL_PREFIX + identifier.substring(OLD_PASCAL_PREFIX.length());
        }
        return identifier;
    }

    /**
     * Feature: project-rename, Property 2: PascalCase 标识符替换保持后缀不变
     *
     * For any valid identifier starting with "Datart" (e.g., class names, type names,
     * interface names), replacing the prefix "Datart" with "YuBi" SHALL produce a result
     * equal to "YuBi" + original suffix, where the suffix part is completely unchanged.
     *
     * Validates: Requirements 1.4, 5.3, 10.5, 10.6
     */
    @Property(tries = 100)
    @Label("Feature: project-rename, Property 2: PascalCase 标识符替换保持后缀不变")
    void pascalCasePrefixReplacementPreservesSuffix(@ForAll("validPascalCaseSuffixes") String suffix) {
        String originalIdentifier = OLD_PASCAL_PREFIX + suffix;
        String expectedIdentifier = NEW_PASCAL_PREFIX + suffix;

        String actualIdentifier = applyPascalCaseRename(originalIdentifier);

        assertThat(actualIdentifier)
                .as("Renaming '%s' should yield '%s'", originalIdentifier, expectedIdentifier)
                .isEqualTo(expectedIdentifier);

        // Additionally verify the suffix portion is exactly preserved
        String actualSuffix = actualIdentifier.substring(NEW_PASCAL_PREFIX.length());
        assertThat(actualSuffix)
                .as("Suffix after rename should be identical to original suffix")
                .isEqualTo(suffix);
    }

    /**
     * Generates random valid PascalCase suffixes for identifiers.
     * Examples: "ServerApplication", "SecurityManager", "SqlPrettyWriter", "AuthenticationProvider"
     *
     * A valid PascalCase suffix starts with an uppercase letter, followed by one or more
     * camelCase segments (each starting with an uppercase letter followed by lowercase letters/digits).
     */
    @Provide
    Arbitrary<String> validPascalCaseSuffixes() {
        Arbitrary<String> segment = pascalCaseSegment();
        return segment.list().ofMinSize(1).ofMaxSize(4)
                .map(segments -> String.join("", segments));
    }

    /**
     * Generates a single PascalCase segment (e.g., "Server", "Application", "Sql").
     * Starts with an uppercase letter, followed by 1-10 lowercase letters/digits.
     */
    private Arbitrary<String> pascalCaseSegment() {
        Arbitrary<Character> firstChar = Arbitraries.chars()
                .range('A', 'Z');
        Arbitrary<String> restChars = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(10);
        return Combinators.combine(firstChar, restChars)
                .as((first, rest) -> first + rest);
    }

    /**
     * Generates random valid Java package suffixes.
     * Examples: "core.common.utils", "data.provider.calcite", "server.config"
     *
     * A valid Java package suffix consists of 1-5 dot-separated segments,
     * each being a valid Java identifier (lowercase letters and digits, starting with a letter).
     */
    @Provide
    Arbitrary<String> validJavaPackageSuffixes() {
        Arbitrary<String> segment = validJavaIdentifierSegment();
        return segment.list().ofMinSize(1).ofMaxSize(5)
                .map(segments -> String.join(".", segments));
    }

    /**
     * Generates a valid Java package name segment.
     * Segments are lowercase identifiers: start with a letter, followed by letters/digits.
     * Length between 2 and 12 characters.
     */
    private Arbitrary<String> validJavaIdentifierSegment() {
        Arbitrary<Character> firstChar = Arbitraries.chars()
                .range('a', 'z');
        Arbitrary<String> restChars = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(11);
        return Combinators.combine(firstChar, restChars)
                .as((first, rest) -> first + rest);
    }

    // --- Property 3: Directory path segment exact match replacement ---

    /**
     * Replaces directory path segments that are exactly "datart" with "yubi".
     * Segments that merely contain "datart" as a substring (e.g. "datart-ext") are NOT replaced.
     */
    static String replaceExactPathSegments(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String[] segments = path.split("/", -1);
        String[] result = new String[segments.length];
        for (int i = 0; i < segments.length; i++) {
            if ("datart".equals(segments[i])) {
                result[i] = "yubi";
            } else {
                result[i] = segments[i];
            }
        }
        return String.join("/", result);
    }

    /**
     * Feature: project-rename, Property 3: 目录路径段精确匹配替换
     *
     * For any file system path, only segments that are exactly equal to "datart"
     * shall be replaced with "yubi"; other segments that contain "datart" as a substring
     * but are not exactly "datart" (e.g. "datart-ext") shall remain unchanged.
     *
     * Validates: Requirements 1.3, 9.4
     */
    @Property(tries = 100)
    @Label("Feature: project-rename, Property 3: 目录路径段精确匹配替换")
    void directoryPathSegmentExactMatchReplacement(
            @ForAll("pathsWithMixedSegments") String path) {

        String[] originalSegments = path.split("/", -1);
        String result = replaceExactPathSegments(path);
        String[] resultSegments = result.split("/", -1);

        // The number of segments must remain the same
        assertThat(resultSegments).hasSameSizeAs(originalSegments);

        for (int i = 0; i < originalSegments.length; i++) {
            if ("datart".equals(originalSegments[i])) {
                // Exact "datart" segments should become "yubi"
                assertThat(resultSegments[i])
                        .as("Segment exactly equal to 'datart' at index %d should be replaced with 'yubi'", i)
                        .isEqualTo("yubi");
            } else {
                // All other segments (including those containing "datart" as substring) remain unchanged
                assertThat(resultSegments[i])
                        .as("Segment '%s' at index %d should remain unchanged", originalSegments[i], i)
                        .isEqualTo(originalSegments[i]);
            }
        }
    }

    // --- Property 5: ArtifactId hyphenated form conversion ---

    private static final String OLD_ARTIFACT_PREFIX = "datart-";
    private static final String NEW_ARTIFACT_PREFIX = "yu-bi-";

    /**
     * Applies the artifactId rename rule:
     * replace "datart-" prefix with "yu-bi-" prefix.
     */
    private String applyArtifactIdRename(String artifactId) {
        if (artifactId.startsWith(OLD_ARTIFACT_PREFIX)) {
            return NEW_ARTIFACT_PREFIX + artifactId.substring(OLD_ARTIFACT_PREFIX.length());
        }
        return artifactId;
    }

    /**
     * Feature: project-rename, Property 5: ArtifactId 连字符形式转换
     *
     * For any Maven artifactId of the form "datart-{suffix}", replacing the prefix
     * "datart-" with "yu-bi-" SHALL produce a result equal to "yu-bi-{suffix}",
     * where the {suffix} part is completely unchanged.
     *
     * Validates: Requirements 2.2
     */
    @Property(tries = 100)
    @Label("Feature: project-rename, Property 5: ArtifactId 连字符形式转换")
    void artifactIdHyphenatedFormConversion(@ForAll("validArtifactIdSuffixes") String suffix) {
        String originalArtifactId = OLD_ARTIFACT_PREFIX + suffix;
        String expectedArtifactId = NEW_ARTIFACT_PREFIX + suffix;

        String actualArtifactId = applyArtifactIdRename(originalArtifactId);

        assertThat(actualArtifactId)
                .as("Renaming '%s' should yield '%s'", originalArtifactId, expectedArtifactId)
                .isEqualTo(expectedArtifactId);

        // Additionally verify the suffix portion is exactly preserved
        String actualSuffix = actualArtifactId.substring(NEW_ARTIFACT_PREFIX.length());
        assertThat(actualSuffix)
                .as("Suffix after rename should be identical to original suffix")
                .isEqualTo(suffix);
    }

    /**
     * Generates random valid artifactId suffixes.
     * Examples: "core", "data-provider-base", "jdbc-data-provider", "server"
     *
     * A valid artifactId suffix consists of 1-4 hyphen-separated segments,
     * each being a lowercase alphanumeric string (letters and digits, starting with a letter).
     */
    @Provide
    Arbitrary<String> validArtifactIdSuffixes() {
        Arbitrary<String> segment = validArtifactSegment();
        return segment.list().ofMinSize(1).ofMaxSize(4)
                .map(segments -> String.join("-", segments));
    }

    /**
     * Generates a single valid artifactId segment.
     * Segments are lowercase alphanumeric: start with a letter, followed by letters/digits.
     * Length between 2 and 10 characters.
     */
    private Arbitrary<String> validArtifactSegment() {
        Arbitrary<Character> firstChar = Arbitraries.chars()
                .range('a', 'z');
        Arbitrary<String> restChars = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(9);
        return Combinators.combine(firstChar, restChars)
                .as((first, rest) -> first + rest);
    }

    /**
     * Generates random directory paths that may or may not contain segments exactly equal to "datart".
     * Paths include a mix of:
     * - Exact "datart" segments (should be replaced)
     * - Segments containing "datart" as substring but not exactly equal (should NOT be replaced)
     * - Normal segments with no "datart" reference at all
     */
    @Provide
    Arbitrary<String> pathsWithMixedSegments() {
        // Segment that is exactly "datart"
        Arbitrary<String> exactDatartSegment = Arbitraries.just("datart");

        // Segments that contain "datart" as substring but are NOT exactly "datart"
        Arbitrary<String> datartSubstringSegment = Arbitraries.of(
                "datart-ext", "datart-core", "my-datart", "datart2",
                "datarts", "pre-datart-post", "xdatart", "datart_plugin"
        );

        // Normal segments that do not contain "datart" at all
        Arbitrary<String> normalSegment = Arbitraries.of(
                "src", "main", "java", "core", "common", "utils",
                "com", "org", "test", "resources", "config", "service"
        );

        // Mix all segment types with controlled frequency
        Arbitrary<String> anySegment = Arbitraries.frequencyOf(
                Tuple.of(3, exactDatartSegment),       // 30% chance of exact "datart"
                Tuple.of(3, datartSubstringSegment),   // 30% chance of datart-like substring
                Tuple.of(4, normalSegment)             // 40% chance of normal segment
        );

        // Build paths with 1-6 segments joined by "/"
        return anySegment.list().ofMinSize(1).ofMaxSize(6)
                .map(segments -> String.join("/", segments));
    }

    // --- Property 6: 归属引用保护 ---

    /**
     * List of protected attribution texts that must NEVER be modified by rename operations.
     * These are legally required attribution references per Apache 2.0 license.
     */
    private static final List<String> PROTECTED_ATTRIBUTION_TEXTS = Arrays.asList(
            "running-elephant/datart",
            "(originally Datart by running-elephant)",
            "Originally forked from running-elephant/datart",
            "This project is based on datart by running-elephant",
            "Datart by running-elephant"
    );

    /**
     * Applies all rename rules to a text but skips protected attribution patterns.
     * This simulates the actual rename engine behavior that preserves legal attributions.
     */
    private String applyRenameWithAttributionProtection(String text) {
        // Check if text contains any protected attribution pattern - if so, preserve it
        for (String protectedText : PROTECTED_ATTRIBUTION_TEXTS) {
            if (text.contains(protectedText)) {
                // Protected text found: apply rename only outside the protected portion
                return applyRenamePreservingProtected(text, protectedText);
            }
        }
        // No protected text - apply all rename rules
        return applyAllRenameRules(text);
    }

    /**
     * Applies rename rules to text while preserving the protected attribution substring.
     */
    private String applyRenamePreservingProtected(String text, String protectedPattern) {
        int idx = text.indexOf(protectedPattern);
        if (idx < 0) {
            return applyAllRenameRules(text);
        }
        String before = text.substring(0, idx);
        String preserved = text.substring(idx, idx + protectedPattern.length());
        String after = text.substring(idx + protectedPattern.length());
        return applyAllRenameRules(before) + preserved + applyAllRenameRules(after);
    }

    /**
     * Applies all standard rename rules (without attribution protection).
     */
    private String applyAllRenameRules(String text) {
        String result = text;
        result = result.replace("Datart", "YuBi");
        result = result.replace("datart", "yubi");
        result = result.replace("DATART", "YUBI");
        return result;
    }

    /**
     * Feature: project-rename, Property 6: 归属引用保护
     *
     * For any text containing protected attribution references (NOTICE file's
     * "running-elephant/datart" source description, README "Project Origin" section,
     * MAINTAINERS.md project origin description, and copyright header's
     * "(originally Datart by running-elephant)" annotation), after all rename
     * operations are completed, these protected texts SHALL remain unchanged.
     *
     * Validates: Requirements 6.4, 9.1
     */
    @Property(tries = 100)
    @Label("Feature: project-rename, Property 6: 归属引用保护")
    void attributionReferencesProtected(@ForAll("textsWithProtectedAttribution") String text) {
        // Identify which protected patterns exist in the input
        List<String> presentPatterns = PROTECTED_ATTRIBUTION_TEXTS.stream()
                .filter(text::contains)
                .collect(java.util.stream.Collectors.toList());

        // Apply rename with attribution protection
        String result = applyRenameWithAttributionProtection(text);

        // Assert: all protected attribution texts that were present remain unchanged
        for (String protectedText : presentPatterns) {
            assertThat(result)
                    .as("Protected attribution text '%s' must remain unchanged after rename", protectedText)
                    .contains(protectedText);
        }
    }

    /**
     * Generates random text that contains at least one protected attribution reference
     * mixed with other content that may contain non-protected "datart" references.
     */
    @Provide
    Arbitrary<String> textsWithProtectedAttribution() {
        // Select a random protected attribution text
        Arbitrary<String> protectedText = Arbitraries.of(PROTECTED_ATTRIBUTION_TEXTS);

        // Random surrounding content that may contain non-protected datart references
        Arbitrary<String> surroundingContent = Arbitraries.of(
                "This project was previously known as datart. ",
                "See the datart documentation for details. ",
                "The Datart configuration system ",
                "Based on DATART_VTABLE logic, ",
                "import datart.core.common; ",
                "// Original project: ",
                "/* Source: ",
                " - maintained by YuBi team. ",
                "Licensed under Apache 2.0. ",
                ""
        );

        // Random whitespace/separator variations
        Arbitrary<String> separator = Arbitraries.of(
                "\n", "\n\n", " ", "  ", "\t", " * ", " // "
        );

        return Combinators.combine(surroundingContent, protectedText, separator, surroundingContent)
                .as((before, prot, sep, after) -> before + sep + prot + sep + after);
    }

    // --- Property 7: 版权声明归属保持 ---

    private static final String ATTRIBUTION_TEXT = "(originally Datart by running-elephant)";

    /**
     * Applies the copyright header transformation.
     * Transforms original copyright headers (containing "* Datart" + "Copyright 2021")
     * into the new format that includes attribution text.
     */
    private String applyCopyrightHeaderTransformation(String originalHeader) {
        // The transformation replaces the original copyright header format with the new format
        // that always includes the attribution "(originally Datart by running-elephant)"
        StringBuilder newHeader = new StringBuilder();
        newHeader.append("/*\n");
        newHeader.append(" * YuBi\n");
        newHeader.append(" *\n");
        newHeader.append(" * Copyright 2021 ").append(ATTRIBUTION_TEXT).append("\n");
        newHeader.append(" * Copyright 2024-2026 YuBi Contributors\n");
        newHeader.append(" *\n");
        newHeader.append(" * Licensed under the Apache License, Version 2.0 (the \"License\");\n");
        newHeader.append(" * you may not use this file except in compliance with the License.\n");
        newHeader.append(" * You may obtain a copy of the License at\n");
        newHeader.append(" *\n");
        newHeader.append(" *   http://www.apache.org/licenses/LICENSE-2.0\n");
        newHeader.append(" *\n");
        newHeader.append(" * Unless required by applicable law or agreed to in writing, software\n");
        newHeader.append(" * distributed under the License is distributed on an \"AS IS\" BASIS,\n");
        newHeader.append(" * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
        newHeader.append(" * See the License for the specific language governing permissions and\n");
        newHeader.append(" * limitations under the License.\n");
        newHeader.append(" */");
        return newHeader.toString();
    }

    /**
     * Feature: project-rename, Property 7: 版权声明归属保持
     *
     * For any source file containing original copyright header
     * ("* Datart" + "Copyright 2021"), the transformed copyright header SHALL contain
     * "(originally Datart by running-elephant)" attribution text, ensuring the original
     * copyright attribution is legally preserved.
     *
     * Validates: Requirements 1.7, 5.6
     */
    @Property(tries = 100)
    @Label("Feature: project-rename, Property 7: 版权声明归属保持")
    void copyrightHeaderContainsAttribution(@ForAll("originalCopyrightHeaders") String originalHeader) {
        // Apply the copyright header transformation
        String transformedHeader = applyCopyrightHeaderTransformation(originalHeader);

        // Assert: the transformed header contains the attribution text
        assertThat(transformedHeader)
                .as("Transformed copyright header must contain attribution text '%s'", ATTRIBUTION_TEXT)
                .contains(ATTRIBUTION_TEXT);

        // Assert: the transformed header contains the new brand name
        assertThat(transformedHeader)
                .as("Transformed copyright header must contain 'YuBi' brand")
                .contains("* YuBi");

        // Assert: the transformed header contains the new copyright line
        assertThat(transformedHeader)
                .as("Transformed copyright header must contain new copyright line")
                .contains("Copyright 2024-2026 YuBi Contributors");

        // Assert: the transformed header preserves Apache 2.0 license reference
        assertThat(transformedHeader)
                .as("Transformed copyright header must contain Apache 2.0 license reference")
                .contains("Licensed under the Apache License, Version 2.0");
    }

    /**
     * Generates random original copyright headers with variations in whitespace,
     * optional HTML tags, and different formatting styles.
     * All generated headers follow the pattern: "* Datart" + "Copyright 2021" + Apache license.
     */
    @Provide
    Arbitrary<String> originalCopyrightHeaders() {
        // Different whitespace variations between elements
        Arbitrary<String> innerWhitespace = Arbitraries.of(" ", "  ", "\n * ", "\n *\n * ");

        // Optional HTML <p> tags that might wrap the copyright notice
        Arbitrary<String> optionalTag = Arbitraries.of("", "<p>", "");

        // Optional closing HTML tags
        Arbitrary<String> optionalCloseTag = Arbitraries.of("", "</p>", "");

        // Different styles of the "Datart" line
        Arbitrary<String> datartLine = Arbitraries.of(
                " * Datart",
                " *  Datart",
                " * Datart\n *"
        );

        // Different styles of the copyright line
        Arbitrary<String> copyrightLine = Arbitraries.of(
                " * Copyright 2021",
                " *  Copyright 2021",
                " * Copyright 2021\n *"
        );

        // Different license text styles
        Arbitrary<String> licenseText = Arbitraries.of(
                " * Licensed under the Apache License, Version 2.0 (the \"License\");",
                " *  Licensed under the Apache License, Version 2.0 (the \"License\");",
                " * Licensed under the Apache License, Version 2.0 (the \"License\");\n * you may not use this file except in compliance with the License."
        );

        return Combinators.combine(datartLine, innerWhitespace, optionalTag, copyrightLine,
                        optionalCloseTag, innerWhitespace, licenseText)
                .as((datart, ws1, tag, copyright, closeTag, ws2, license) ->
                        "/*\n" + datart + "\n *\n" + tag + copyright + closeTag + "\n *\n" + license + "\n */");
    }
}
