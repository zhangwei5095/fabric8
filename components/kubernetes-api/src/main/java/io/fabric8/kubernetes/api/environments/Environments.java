/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.environments;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A helper class for working with environments (Dev, Test, Staging, Production) in fabric8
 */
public class Environments {
    private static final transient Logger LOG = LoggerFactory.getLogger(Environments.class);

    private final Map<String, Environment> environments;

    public Environments(Map<String, Environment> environments) {
        this.environments = environments;
    }

    public static Environments load(KubernetesClient kubernetesClient, String namespace) {
        namespace = getDefaultNamespace(kubernetesClient, namespace);
        LOG.debug("Loading environments from namespace: " + namespace);
        ConfigMap configMap = kubernetesClient.configMaps().inNamespace(namespace).withName("fabric8-environments").get();
        return load(configMap);
    }

    /**
     * Returns the namespace for the given environment name if its defined or null if one cannot be found
     */
    public static String namespaceForEnvironment(KubernetesClient kubernetesClient, String environmentKey, String namespace) {
        Environments environments = Environments.load(kubernetesClient, namespace);
        Environment environment = environments.getEnvironment(environmentKey);
        String answer = null;
        if (environment != null) {
            answer = environment.getNamespace();
        }
        return answer;
    }

    protected static String getDefaultNamespace(KubernetesClient kubernetesClient, String namespace) {
        if (Strings.isNullOrBlank(namespace)) {
            namespace = kubernetesClient.getNamespace();
            if (Strings.isNullOrBlank(namespace)) {
                namespace = KubernetesHelper.defaultNamespace();
            }
        }
        return namespace;
    }

    private static Environments load(ConfigMap configMap) {
        Map<String, Environment> environmentMap = new HashMap<>();
        if (configMap != null) {
            Map<String, String> data = configMap.getData();
            if (data != null) {
                Set<Map.Entry<String, String>> entries = data.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    String key = entry.getKey();
                    String yaml = entry.getValue();
                    Environment environment = parseEnvironment(key, yaml);
                    if (environment != null) {
                        environmentMap.put(key, environment);
                    }
                }
            }
        }
        return new Environments(environmentMap);
    }

    private static Environment parseEnvironment(String key, String yaml) {
        try {
            return KubernetesHelper.loadYaml(yaml, Environment.class);
        } catch (IOException e) {
            LOG.warn("Failed to parse environment YAML for " + key + ". Reason: " + e + ". YAML: " + yaml, e);
            return null;
        }
    }

    public Environment getEnvironment(String environmentKey) {
        return environments.get(environmentKey);
    }

    public Map<String, Environment> getEnvironments() {
        return environments;
    }

    /**
     * Returns the sorted set of environments
     */
    public SortedSet<Environment> getEnvironmentSet() {
        return new TreeSet<>(environments.values());
    }
}
