package com.neotech.arc.ghec.controller;

import com.neotech.arc.ghec.dto.AksClusterDetailsDTO;
import com.neotech.arc.ghec.service.AzureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/azure")
public class AzureController {

    private final AzureService azureService;

    @Autowired
    public AzureController(AzureService azureService) {
        this.azureService = azureService;
    }

    @GetMapping("/aks/count")
    public int getAksClusterCount() {
        return azureService.getAksClusterCount();
    }

    @GetMapping("/aks")
    public java.util.List<com.neotech.arc.ghec.dto.AksClusterDTO> getAksClusters() {
        return azureService.getAksClusters();
    }

    @GetMapping("/aks/{resourceGroup}/{clusterName}")
    public AksClusterDetailsDTO getClusterDetails(@PathVariable String resourceGroup,
            @PathVariable String clusterName) {
        return azureService.getClusterDetails(resourceGroup, clusterName);
    }
}
