package com.neotech.arc.ghec.dto;

public class AksClusterDTO {
    private String name;
    private String resourceGroup;
    private String kubernetesVersion;
    private String provisioningState;
    private int agentPoolCount;

    public AksClusterDTO(String name, String resourceGroup, String kubernetesVersion, String provisioningState,
            int agentPoolCount) {
        this.name = name;
        this.resourceGroup = resourceGroup;
        this.kubernetesVersion = kubernetesVersion;
        this.provisioningState = provisioningState;
        this.agentPoolCount = agentPoolCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getKubernetesVersion() {
        return kubernetesVersion;
    }

    public void setKubernetesVersion(String kubernetesVersion) {
        this.kubernetesVersion = kubernetesVersion;
    }

    public String getProvisioningState() {
        return provisioningState;
    }

    public void setProvisioningState(String provisioningState) {
        this.provisioningState = provisioningState;
    }

    public int getAgentPoolCount() {
        return agentPoolCount;
    }

    public void setAgentPoolCount(int agentPoolCount) {
        this.agentPoolCount = agentPoolCount;
    }
}
