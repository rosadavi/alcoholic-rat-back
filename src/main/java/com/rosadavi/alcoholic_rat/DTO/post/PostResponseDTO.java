package com.rosadavi.alcoholic_rat.DTO.post;

import com.rosadavi.alcoholic_rat.domain.post.Post;

import java.time.Instant;
import java.util.List;

public record PostResponseDTO(
        String id,
        String authorLogin,
        String barId,
        String description,
        Instant createdAt,
        List<PostDrinkResponseDTO> drinks,
        List<PostPhotoResponseDTO> photos
) {
    public PostResponseDTO(Post p) {
        this(p.getId(), p.getAuthor().getLogin(), p.getBar() != null ? p.getBar().getId() : null,
                p.getDescription(), p.getCreatedAt(),
                p.getDrinks().stream().map(PostDrinkResponseDTO::new).toList(),
                p.getPhotos().stream().map(PostPhotoResponseDTO::new).toList());
    }
}
