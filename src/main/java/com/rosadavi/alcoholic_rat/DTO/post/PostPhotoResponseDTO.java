package com.rosadavi.alcoholic_rat.DTO.post;

import com.rosadavi.alcoholic_rat.domain.post.PostPhoto;

import java.time.Instant;

public record PostPhotoResponseDTO(
        String id,
        String url,
        String contentType,
        Instant addedAt
) {
    public PostPhotoResponseDTO(PostPhoto p) {
        this(p.getId(), "/posts/" + p.getPost().getId() + "/photos/" + p.getId(), p.getContentType(), p.getAddedAt());
    }
}
