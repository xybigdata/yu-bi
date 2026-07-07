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
package yubi.core.data.provider;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DataProviderConfigTemplate implements Serializable {

    private String type;

    private String name;

    private String displayName;

    private List<Attribute> attributes;

    @Data
    public static class Attribute implements Serializable {

        private String name;

        private String displayName;

        private boolean required;

        private boolean encrypt;

        private Object defaultValue;

        private String key;

        private String type;

        private String description;

        private List<Object> options;

        private List<Attribute> children;

        public void setRequired(Boolean required) {
            this.required = required != null && required;
        }

        public void setEncrypt(Boolean encrypt) {
            this.encrypt = encrypt != null && encrypt;
        }

    }


}
