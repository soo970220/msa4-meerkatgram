package com.msa4meerkatgram.domain.post.requests;

public record PostCreateReq(
    String content,
    String image
) {

}
