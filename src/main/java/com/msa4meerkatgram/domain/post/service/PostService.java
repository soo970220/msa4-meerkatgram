package com.msa4meerkatgram.domain.post.service;

import com.msa4meerkatgram.domain.post.requests.PostCreateReq;
import com.msa4meerkatgram.domain.post.entities.Post;
import com.msa4meerkatgram.domain.post.mapper.PostMapper;
import com.msa4meerkatgram.domain.post.requests.PostIndexReq;
import com.msa4meerkatgram.domain.post.responses.PostIndexRes;
import com.msa4meerkatgram.domain.user.entities.User;
import com.msa4meerkatgram.domain.user.mapper.UserMapper;
import com.msa4meerkatgram.global.errors.custom.DeletedRecordException;
import com.msa4meerkatgram.global.errors.custom.DuplicatedRecordException;
import com.msa4meerkatgram.global.errors.custom.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    public PostIndexRes index(PostIndexReq postIndexReq) {
        int offset = (postIndexReq.page() - 1) * postIndexReq.limit();

        // 특정 페이지 게시글 조회
        List<Post> posts = postMapper.getPagination(postIndexReq.limit(), offset);

        // 토탈 획득
        long total = postMapper.getTotal();
        boolean lastPage = offset + postIndexReq.limit() >= total;

        // 컨트롤러 전달
        return PostIndexRes.builder()
            .total(total)
            .lastPage(lastPage)
            .posts(posts)
            .build();
    }

    public Post show(long id) {
        Post post = postMapper.findByPk(id);
        if (post == null) {
            throw new DeletedRecordException("이미 삭제된 게시글입니다.");
        }

        return post;
    }

    @Transactional(rollbackFor = Exception.class)
    public Post create(long userId, PostCreateReq  postCreateReq) {
        Post post = Post.builder()
            .userId(userId)
            .content(postCreateReq.content())
            .image(postCreateReq.image())
            .build();
            postMapper.create(post);
    return postMapper.findByPk(post.getId());
    }
}