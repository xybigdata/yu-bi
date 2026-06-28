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

package yubi.server.config;

import yubi.server.config.interceptor.BasicValidRequestInterceptor;
import yubi.server.controller.BaseController;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.cfg.EnumFeature;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${yubi.server.path-prefix}")
    private String pathPrefix;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //i18n locale interceptor
        registry.addInterceptor(new BasicValidRequestInterceptor()).addPathPatterns("/**");
    }

    //Add request url prefix
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(getPathPrefix(), aClass -> aClass.getSuperclass().equals(BaseController.class));
    }

    public String getPathPrefix() {
        if (pathPrefix.endsWith("/")) {
            return pathPrefix.substring(0, pathPrefix.length() - 1);
        }
        return pathPrefix;
    }

    @Bean
    public JsonMapperBuilderCustomizer yubiJacksonCustomizer() {
        return builder -> builder.enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING);
    }
}
