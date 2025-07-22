package com.mm.image_aws.service.transformer;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component // Đánh dấu là một Spring Bean để có thể tự động inject
public class GoogleDriveUrlTransformer implements UrlTransformer {

    private static final Pattern GOOGLE_DRIVE_URL_PATTERN =
            Pattern.compile("drive\\.google\\.com/(?:file/d/|open\\?id=|uc\\?id=|drive/folders/)([a-zA-Z0-9_-]{28,})");

    @Override
    public Optional<String> transform(String url) {
        Matcher matcher = GOOGLE_DRIVE_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            String fileId = matcher.group(1);
            String directUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
            return Optional.of(directUrl);
        }
        return Optional.empty();
    }
}