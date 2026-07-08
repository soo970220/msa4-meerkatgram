package com.msa4meerkatgram.domain.post.responses;

import com.msa4meerkatgram.domain.post.entities.Post;

import java.util.List;

public record PostIndexRes(
        long total
        ,boolean lastPage
        ,List<PostWithUserRes> posts
) {
    public static PostIndexRes from(long total, boolean lastPage, List<Post> posts) {
        return new PostIndexRes(
                total
                , lastPage
                , posts.stream().map(PostWithUserRes::from).toList()
                // posts.stream() = posts(List<Post>)를 Stream으로 변환
                //.map(PostWithUserRes::from) = map()은 각 요소를 다른 객체로 변환하는 메서드
                // (객체명::메서드명)
                // = 람다식 post -> PostWithUserRes.from(post)
        );
    }
}
