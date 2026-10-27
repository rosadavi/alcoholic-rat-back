package com.rosadavi.alcoholic_rat.services;

import com.rosadavi.alcoholic_rat.DTO.post.PostCreateRequestDTO;
import com.rosadavi.alcoholic_rat.DTO.post.PostDrinkAppendDTO;
import com.rosadavi.alcoholic_rat.DTO.post.PostDrinkRequestDTO;
import com.rosadavi.alcoholic_rat.domain.bar.Bar;
import com.rosadavi.alcoholic_rat.domain.drink.Drink;
import com.rosadavi.alcoholic_rat.domain.post.Post;
import com.rosadavi.alcoholic_rat.domain.post.PostDrink;
import com.rosadavi.alcoholic_rat.domain.user.User;
import com.rosadavi.alcoholic_rat.exception.*;
import com.rosadavi.alcoholic_rat.repository.BarRepository;
import com.rosadavi.alcoholic_rat.repository.DrinkRepository;
import com.rosadavi.alcoholic_rat.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PostService {
    @Autowired
    PostRepository postRepository;
    @Autowired
    BarRepository barRepository;
    @Autowired
    DrinkRepository drinkRepository;

    private PostDrink toPostDrink(Post post, PostDrinkRequestDTO d) {
        if(d.drinkId() != null) {
            Drink drink = drinkRepository.findById(d.drinkId())
                    .orElseThrow(() -> new DrinkNotFoundException(d.drinkId()));

            return new PostDrink(post, drink,
                    d.title() != null && !d.title().isBlank() ? d.title().trim() : drink.getTitle(),
                    d.volumeMl() != null ? d.volumeMl() : drink.getVolumeMl(),
                    d.alcoholPercentage() != null ? d.alcoholPercentage() : drink.getAlcoholPercentage());
        }

        if(d.title() == null || d.title().isBlank() || d.volumeMl() == null || d.alcoholPercentage() == null) {
            throw new InvalidPostException("Drink must have a title, volume and alcohol percentage");
        }

        return new PostDrink(post, null, d.title().trim(), d.volumeMl(), d.alcoholPercentage());
    }

    @Transactional
    public Post register(PostCreateRequestDTO data, User author) {
        if(data.drinks() == null || data.drinks().isEmpty()) {
            throw new InvalidPostException("Post must contain at least one drink");
        }

        Bar bar = null;
        if(data.barId() != null) {
            bar = barRepository.findById(data.barId())
                    .orElseThrow(() -> new BarNotFoundException(data.barId()));
        }

        Post post = new Post(author, bar, data.description());
        for(PostDrinkRequestDTO d : data.drinks()) {
            post.addDrink(toPostDrink(post, d));
        }

        return postRepository.save(post);
    }

    @Transactional
    public Post appendDrinks(String postId, PostDrinkAppendDTO data, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if(!post.getAuthor().getId().equals(user.getId())) {
            throw new PostForbiddenException("Only the author can add drinks to this post.");
        }

        if(!post.isEditable(Instant.now())) {
            throw new PostForbiddenException("Post can no longer be edited.");
        }

        for(PostDrinkRequestDTO d : data.drinks()) {
            post.addDrink(toPostDrink(post, d));
        }

        return post;
    }
}
