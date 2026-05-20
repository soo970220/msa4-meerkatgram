package com.msa4meerkatgram.domain.post.entities;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Post {
    private long id;
    private long userId;
    private String content;
    private String image;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

}
