package io.github.shhome.homehub.monitoring.dto;

public record DockerContainerResponse(
        String id,
        String name,
        String image,
        DockerContainerState state,
        String statusDetail,
        String ports,
        String composeProject
) {
}
