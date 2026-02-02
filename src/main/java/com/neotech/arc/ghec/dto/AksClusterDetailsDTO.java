package com.neotech.arc.ghec.dto;

import java.util.List;

public class AksClusterDetailsDTO extends AksClusterDTO {
    private final List<PodDTO> pods;

    public AksClusterDetailsDTO(String name, String resourceGroup, String version, String provisioningState,
            int agentPoolCount, List<PodDTO> pods) {
        super(name, resourceGroup, version, provisioningState, agentPoolCount);
        this.pods = pods;
    }

    public List<PodDTO> getPods() {
        return pods;
    }
}
