package com.sum.application.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sum.application.api.ApplicationResourceMappingProvider;
import com.sum.application.api.model.ApplicationResource;
import com.sum.application.model.annotation.ApiGroup;
import com.sum.application.model.annotation.ApiVersion;
import com.sum.application.model.util.Helper;
import com.sum.application.api.model.ApplicationListBuilder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ApplicationDeserializer extends JsonDeserializer<ApplicationResource> {

    private static final String KIND = "kind";
    private static final String API_VERSION = "apiVersion";

    private static final Mapping mapping = new Mapping();

    @Override
    public ApplicationResource deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.readValueAsTree();
        if (node.isObject()) {
            return fromObjectNode(jp, node);
        } else if (node.isArray()) {
            return fromArrayNode(jp, node);
        } else {
            return null;
        }
    }

    private ApplicationResource fromArrayNode(JsonParser jp, JsonNode node) throws IOException {
        Iterator<JsonNode> iterator = node.elements();
        while (iterator.hasNext()) {
            JsonNode jsonNode = iterator.next();
            if (jsonNode.isObject()) {
                ApplicationResource resource = fromObjectNode(jp, jsonNode);
            }
        }
        return new ApplicationListBuilder().withItems(new ArrayList<>()).build();
    }

    private static ApplicationResource fromObjectNode(JsonParser jp, JsonNode node) throws IOException {
        String key = getKey(node);
        if (key != null) {
            Class<? extends ApplicationResource> resourceType = mapping.getForKey(key);
            if (resourceType == null) {
                throw JsonMappingException.from(jp, "No resource type found for:" + key);
            } else if (ApplicationResource.class.isAssignableFrom(resourceType)) {
                return jp.getCodec().treeToValue(node, resourceType);
            }
        }
        return null;
    }

    /**
     * Return a string representation of the key of the type: <version>#<kind>.
     */
    private static String getKey(JsonNode node) {
        JsonNode apiVersion = node.get(API_VERSION);
        JsonNode kind = node.get(KIND);

        return mapping.createKey(
                apiVersion != null ? apiVersion.textValue() : null,
                kind != null ? kind.textValue() : null);
    }

    /**
     * Registers a Custom Resource Definition Kind
     */
    public static void registerCustomKind(String kind, Class<? extends ApplicationResource> clazz) {
        registerCustomKind(null, kind, clazz);
    }

    /**
     * Registers a Custom Resource Definition Kind
     */
    public static void registerCustomKind(String apiVersion, String kind, Class<? extends ApplicationResource> clazz) {
        mapping.registerKind(apiVersion, kind, clazz);
    }

    /**
     * Registers a Custom Resource Mapping Provider
     */
    public static void registerProvider(ApplicationResourceMappingProvider provider) {
        mapping.registerProvider(provider);
    }

    static class Mapping {

        private static final String KEY_SEPARATOR = "#";
        private static final String[] PACKAGES = {
                "io.fabric8.Application.api.model.",
                "io.fabric8.Application.api.model.admission",
                "io.fabric8.Application.api.model.admissionregistration.v1.",
                "io.fabric8.Application.api.model.admissionregistration.v1beta1.",
                "io.fabric8.Application.api.model.apiextensions.",
                "io.fabric8.Application.api.model.apps.",
                "io.fabric8.Application.api.model.authentication.",
                "io.fabric8.Application.api.model.authorization.",
                "io.fabric8.Application.api.model.autoscaling.",
                "io.fabric8.Application.api.model.autoscaling.v1.",
                "io.fabric8.Application.api.model.autoscaling.v2beta1.",
                "io.fabric8.Application.api.model.autoscaling.v2beta2.",
                "io.fabric8.Application.api.model.batch.",
                "io.fabric8.Application.api.model.certificates.",
                "io.fabric8.Application.api.model.coordination.",
                "io.fabric8.Application.api.model.coordination.v1.",
                "io.fabric8.Application.api.model.discovery.",
                "io.fabric8.Application.api.model.extensions.",
                "io.fabric8.Application.api.model.events.",
                "io.fabric8.Application.api.model.networking.",
                "io.fabric8.Application.api.model.networking.v1beta1.",
                "io.fabric8.Application.api.model.policy.",
                "io.fabric8.Application.api.model.rbac.",
                "io.fabric8.Application.api.model.storage.",
                "io.fabric8.Application.api.model.scheduling.",
                "io.fabric8.Application.api.model.settings.",
                "io.fabric8.openshift.api.model."
        };

        private Map<String, Class<? extends ApplicationResource>> mappings = new ConcurrentHashMap<>();

        Mapping() {
            registerAllProviders();
        }

        public Class<? extends ApplicationResource> getForKey(String key) {
            if (key == null) {
                return null;
            }
            Class<? extends ApplicationResource> clazz = mappings.get(key);
            if (clazz != null) {
                return clazz;
            }
            clazz = getInternalTypeForName(key);
            if (clazz != null) {
                mappings.put(key, clazz);
            }
            return clazz;
        }

        public void registerKind(String apiVersion, String kind, Class<? extends ApplicationResource> clazz) {
            mappings.put(createKey(apiVersion, kind), clazz);
        }

        public void registerProvider(ApplicationResourceMappingProvider provider) {
            if (provider == null) {
                return;
            }
            Map<String, Class<? extends ApplicationResource>> providerMappings = provider.getMappings().entrySet().stream()
                    //If the model is shaded (which is as part of Application-client uberjar) this is going to cause conflicts.
                    //This is why we NEED TO filter out incompatible resources.
                    .filter(entry -> ApplicationResource.class.isAssignableFrom(entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            mappings.putAll(providerMappings);
        }

        /**
         * Returns a composite key for apiVersion and kind.
         */
        String createKey(String apiVersion, String kind) {
            if (kind == null) {
                return null;
            } else if (apiVersion == null) {
                return kind;
            } else {
                return apiVersion + KEY_SEPARATOR + kind;
            }
        }

        private void registerAllProviders() {
            getAllMappingProviders().forEach(this::registerProvider);
        }

        Stream<ApplicationResourceMappingProvider> getAllMappingProviders() {
            //Use service loader to load extension types.
            Iterable<ApplicationResourceMappingProvider> currentThreadClassLoader =
                    () -> ServiceLoader.load(ApplicationResourceMappingProvider.class, Thread.currentThread().getContextClassLoader())
                            .iterator();
            Iterable<ApplicationResourceMappingProvider> classClassLoader =
                    () -> ServiceLoader.load(ApplicationResourceMappingProvider.class, ApplicationDeserializer.class.getClassLoader())
                            .iterator();
            return Stream.concat(
                    StreamSupport.stream(currentThreadClassLoader.spliterator(), false),
                    StreamSupport.stream(classClassLoader.spliterator(), false))
                    .filter(distinctByClassName(ApplicationResourceMappingProvider::getClass));
        }

        private String getClassName(String key) {
            if (key != null && key.contains(KEY_SEPARATOR)) {
                return key.substring(key.indexOf(KEY_SEPARATOR) + 1);
            } else {
                return key;
            }
        }

        private Class<? extends ApplicationResource> getInternalTypeForName(String key) {
            String name = getClassName(key);
            List<Class<? extends ApplicationResource>> possibleResults = new ArrayList<>();

            // First pass, check if there are more than one class of same name
            for (String aPackage : PACKAGES) {
                Class<? extends ApplicationResource> result = loadClassIfExists(aPackage + name);
                if (result != null) {
                    possibleResults.add(result);
                }
            }

            // If only one class found, return it
            if (possibleResults.size() == 1) {
                return possibleResults.get(0);
            }

            // Compare with apiVersions being compared for set of classes found
            for (Class<? extends ApplicationResource> result : possibleResults) {
                String defaultKeyFromClass = getKeyFromClass(result);
                if (key.equals(defaultKeyFromClass)) {
                    return result;
                }
            }
            return null;
        }

        private String getKeyFromClass(Class<? extends ApplicationResource> clazz) {
            String apiGroup = Helper.getAnnotationValue(clazz, ApiGroup.class);
            String apiVersion = Helper.getAnnotationValue(clazz, ApiVersion.class);
            if (apiGroup != null && !apiGroup.isEmpty() && apiVersion != null && !apiVersion.isEmpty()) {
                return createKey(apiGroup + "/" + apiVersion, clazz.getSimpleName());
            }
            return clazz.getSimpleName();
        }


        private Class<? extends ApplicationResource> loadClassIfExists(String className) {
            try {
                Class<?> clazz = ApplicationDeserializer.class.getClassLoader().loadClass(className);
                if (!ApplicationResource.class.isAssignableFrom(clazz)) {
                    return null;
                }
                return (Class<? extends ApplicationResource>) clazz;
            } catch (Exception t) {
                return null;
            }
        }

        private Predicate<ApplicationResourceMappingProvider> distinctByClassName(
                Function<ApplicationResourceMappingProvider, Class<? extends ApplicationResourceMappingProvider>> mapperProvider) {
            Set<String> existing = ConcurrentHashMap.newKeySet();
            return provider -> existing.add(mapperProvider.apply(provider).getName());
        }

    }
}
