package com.msa4meerkatgram.domain.post.repositories;

import com.msa4meerkatgram.domain.post.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

}
