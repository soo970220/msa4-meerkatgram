package com.msa4meerkatgram.domain.auth.mappers;

import com.msa4meerkatgram.domain.user.entities.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMapper {
    int updateRefreshToken(long id, String refreshToken);
    int create(User user);


}
