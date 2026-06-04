package com.msa4meerkatgram.global.utill.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.List;

@ConfigurationProperties(prefix = "file")
public record FileConfig(
    //어플리케이션 파일 참고
    String serverUri
    , String storagePath
    , String profilePath
    , String postPath
    , List<String> allowExtensionList
    ) {

}
