package com.neotech.arc.ghec.service;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.neotech.arc.ghec.dto.AksClusterDetailsDTO;
import com.neotech.arc.ghec.dto.PodDTO;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AzureService {

    private AzureResourceManager azureResourceManager;

    public AzureService() {
        // Authenticate using DefaultAzureCredential (supports CLI, Environment, Managed
        // Identity, etc.)
        try {
            this.azureResourceManager = AzureResourceManager.configure()
                    .authenticate(new DefaultAzureCredentialBuilder().build(), new AzureProfile(AzureEnvironment.AZURE))
                    .withDefaultSubscription();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Counts the number of AKS clusters in the default subscription.
     *
     * @return the number of AKS clusters.
     */
    public int getAksClusterCount() {
        // PagedIterable is returned, we can count them.
        // For large numbers, this might be slow as it iterates pages, but for typical
        // use ok.
        if (this.azureResourceManager == null) {
            return 0;
        }
        return this.azureResourceManager.kubernetesClusters().list().stream().toList().size();
    }

    public List<com.neotech.arc.ghec.dto.AksClusterDTO> getAksClusters() {
        if (this.azureResourceManager == null) {
            return List.of();
        }
        return this.azureResourceManager.kubernetesClusters().list().stream()
                .map(cluster -> new com.neotech.arc.ghec.dto.AksClusterDTO(
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

        // Get admin kubeconfig
        byte[] kubeConfigBytes = cluster.adminKubeConfigs().get(0).value();
        String kubeConfigContent = new String(kubeConfigBytes, StandardCharsets.UTF_8);

        try (KubernetesClient client = new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(kubeConfigContent))
                .build()) {

            List<PodDTO> pods = client.pods().inAnyNamespace().list().getItems().stream()
                    .map(pod -> new PodDTO(
                            pod.getMetadata().getName(),
                            pod.getMetadata().getNamespace(),
                            pod.getStatus().getPhase(),
                            pod.getSpec().getNodeName(),
                            pod.getStatus().getStartTime(),
                            pod.getStatus().getContainerStatuses().stream()
                                    .mapToInt(status -> status.getRestartCount())
                                    .sum()))
                    .collect(Collectors.toList());

            return new AksClusterDetailsDTO(
                    cluster.name(),
                    cluster.resourceGroupName(),
                    cluster.version(),
                    cluster.provisioningState(),
                    cluster.agentPools().size(),
                    pods);
        }
    }
}
