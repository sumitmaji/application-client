package com.sum.application.api;

import com.sum.application.api.model.ApplicationResource;

import java.util.Map;

public interface ApplicationResourceMappingProvider {
    Map<String, Class<? extends ApplicationResource>> getMappings();

}
