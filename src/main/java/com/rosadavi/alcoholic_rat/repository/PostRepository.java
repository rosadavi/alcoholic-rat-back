package com.rosadavi.alcoholic_rat.repository;

import com.rosadavi.alcoholic_rat.domain.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, String> {

}
