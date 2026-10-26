package com.rosadavi.alcoholic_rat.controller;

import com.rosadavi.alcoholic_rat.DTO.post.PostCreateRequestDTO;
import com.rosadavi.alcoholic_rat.DTO.post.PostResponseDTO;
import com.rosadavi.alcoholic_rat.domain.user.User;
import com.rosadavi.alcoholic_rat.services.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    @Autowired
    PostService service;

    @PostMapping
    public ResponseEntity<PostResponseDTO> create(@RequestBody @Valid PostCreateRequestDTO data,
                                                  @AuthenticationPrincipal User user) {
        var post = service.register(data, user);
        return ResponseEntity.created(URI.create("/posts/" + post.getId())).body(new PostResponseDTO(post));
    }
}
