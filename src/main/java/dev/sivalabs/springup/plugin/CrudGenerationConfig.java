package dev.sivalabs.springup.plugin;

public record CrudGenerationConfig(
        String controllerName,
        String serviceName,
        String repositoryName,
        String controllerPackage,
        String servicePackage,
        String repositoryPackage) {
}
