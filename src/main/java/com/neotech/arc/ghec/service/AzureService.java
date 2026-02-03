package com.neotech.arc.ghec.service;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.neotech.arc.ghec.dto.AksClusterDetailsDTO;
import com.neotech.arc.ghec.dto.AksClusterDTO;
import com.neotech.arc.ghec.dto.PodDTO;
import org.springframework.stereotype.Service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AzureService {

    private AzureResourceManager azureResourceManager;

    public AzureService() {
        try {
            this.azureResourceManager = AzureResourceManager.configure()
                    .authenticate(
                            new DefaultAzureCredentialBuilder().build(),
                            new AzureProfile(AzureEnvironment.AZURE))
                    .withDefaultSubscription();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize AzureResourceManager", e);
        }
    }

    public int getAksClusterCount() {
        if (this.azureResourceManager == null) {
            return 0;
        }
        return (int) this.azureResourceManager.kubernetesClusters().list().stream().count();
    }

    public List<AksClusterDTO> getAksClusters() {
        if (this.azureResourceManager == null) {
            return List.of();
        }

        return this.azureResourceManager.kubernetesClusters().list().stream()
                .map(cluster -> new AksClusterDTO(
                        cluster.name(),
                        cluster.resourceGroupName(),
                        cluster.version(),
                        cluster.provisioningState(),
                        cluster.agentPools().size()))
                .collect(Collectors.toList());
    }

    public AksClusterDetailsDTO getClusterDetails(String resourceGroup, String clusterName) {

        if (this.azureResourceManager == null) {
            return null;
        }

        KubernetesCluster cluster = this.azureResourceManager.kubernetesClusters()
                .getByResourceGroup(resourceGroup, clusterName);

        if (cluster == null) {
            throw new RuntimeException("Cluster not found: " + clusterName);
        }

        // ⚠️ Admin kubeconfig (OK for now, see notes below)
        byte[] kubeConfigBytes = cluster.adminKubeConfigs().get(0).value();
        String kubeConfigContent = new String(kubeConfigBytes, StandardCharsets.UTF_8);

        try {
            // Build Kubernetes API client from kubeconfig
            KubeConfig kubeConfig = KubeConfig.loadKubeConfig(
                    new StringReader(kubeConfigContent));

            ApiClient apiClient = ClientBuilder.kubeconfig(kubeConfig).build();

            CoreV1Api coreV1Api = new CoreV1Api(apiClient);

            // List pods across all namespaces
            V1PodList podList = coreV1Api.listPodForAllNamespaces(
                    null,   // pretty
                    null,   // allowWatchBookmarks
                    null,   // continue
                    null,   // fieldSelector
                    null,   // labelSelector
                    null,   // limit
                    null,   // resourceVersion
                    null,   // resourceVersionMatch
                    null,   // timeoutSeconds
                    false   // watch
            );

            List<PodDTO> pods = podList.getItems().stream()
                    .map(this::toPodDTO)
                    .collect(Collectors.toList());

            return new AksClusterDetailsDTO(
                    cluster.name(),
                    cluster.resourceGroupName(),
                    cluster.version(),
                    cluster.provisioningState(),
                    cluster.agentPools().size(),
                    pods
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve pod details for AKS cluster", e);
        }
    }

    private PodDTO toPodDTO(V1Pod pod) {

        int restartCount = 0;
        if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
            restartCount = pod.getStatus().getContainerStatuses()
                    .stream()
                    .mapToInt(cs -> cs.getRestartCount())
                    .sum();
        }

        return new PodDTO(
                pod.getMetadata().getName(),
                pod.getMetadata().getNamespace(),
                pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown",
                pod.getSpec() != null ? pod.getSpec().getNodeName() : null,
                pod.getStatus() != null ? pod.getStatus().toString() : null,
                restartCount
        );
    }
}