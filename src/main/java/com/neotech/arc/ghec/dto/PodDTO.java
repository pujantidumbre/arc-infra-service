package com.neotech.arc.ghec.dto;

public record PodDTO(
        String name,
        String namespace,
        String status,
        String nodeName,
        String startTime,
        int restartCount) {
}
