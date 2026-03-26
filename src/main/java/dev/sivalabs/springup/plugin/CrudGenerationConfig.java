package dev.sivalabs.springup.plugin;

public class CrudGenerationConfig {

    private final String controllerName;
    private final String serviceName;
    private final String repositoryName;

    private final String controllerPackage;
    private final String servicePackage;
    private final String repositoryPackage;

    private final boolean generateRequestDto;
    private final boolean generateResponseDto;

    public CrudGenerationConfig(
            String controllerName,
            String serviceName,
            String repositoryName,
            String controllerPackage,
            String servicePackage,
            String repositoryPackage,
            boolean generateRequestDto,
            boolean generateResponseDto
    ) {
        this.controllerName = controllerName;
        this.serviceName = serviceName;
        this.repositoryName = repositoryName;
        this.controllerPackage = controllerPackage;
        this.servicePackage = servicePackage;
        this.repositoryPackage = repositoryPackage;
        this.generateRequestDto = generateRequestDto;
        this.generateResponseDto = generateResponseDto;
    }

    public String controllerName() { return controllerName; }
    public String serviceName() { return serviceName; }
    public String repositoryName() { return repositoryName; }

    public String controllerPackage() { return controllerPackage; }
    public String servicePackage() { return servicePackage; }
    public String repositoryPackage() { return repositoryPackage; }

    public boolean generateRequestDto() { return generateRequestDto; }
    public boolean generateResponseDto() { return generateResponseDto; }
}