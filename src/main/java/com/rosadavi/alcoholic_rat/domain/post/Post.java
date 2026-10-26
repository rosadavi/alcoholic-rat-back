package com.rosadavi.alcoholic_rat.domain.post;

import com.rosadavi.alcoholic_rat.domain.bar.Bar;
import com.rosadavi.alcoholic_rat.domain.user.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Table(name = "post")
@Entity(name = "post")
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Post {
    public static final Duration EDIT_WINDOW = Duration.ofHours(24);

    public Post(User author, Bar bar, String description) {
        this.author = author;
        this.bar = bar;
        this.description = description;
        this.createdAt = Instant.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bar_id", updatable = false)
    private Bar bar;

    @Column(length = 1000, updatable = false)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.PERSIST)
    @OrderBy("addedAt ASC")
    private List<PostDrink> drinks = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.PERSIST)
    @OrderBy("addedAt ASC")
    private List<PostPhoto> photos = new ArrayList<>();

    public boolean isEditable(Instant now) {
        return now.isBefore(createdAt.plus(EDIT_WINDOW));
    }

    public void addDrink(PostDrink d) {
        drinks.add(d);
    }

    public void addPhoto(PostPhoto p) {
        photos.add(p);
    }
}
