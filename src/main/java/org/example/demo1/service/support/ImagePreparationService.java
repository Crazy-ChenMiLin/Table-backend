package org.example.demo1.service.support;

import org.example.demo1.config.MimoProperties;
import org.example.demo1.exception.BusinessException;
import org.example.demo1.logging.RecognitionTimingContext;
import org.example.demo1.util.Base64Util;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ImagePreparationService {

    private final MimoProperties mimoProperties;
    private final RecognitionStepTimer stepTimer;

    public ImagePreparationService(MimoProperties mimoProperties, RecognitionStepTimer stepTimer) {
        this.mimoProperties = mimoProperties;
        this.stepTimer = stepTimer;
    }

    public PreparedImage prepare(MultipartFile image, RecognitionTimingContext timingContext) throws Exception {
        stepTimer.measureVoid(timingContext, "validate_image", () -> {
            validateImage(image);
            return null;
        });

        if (!StringUtils.hasText(mimoProperties.getApiKey())) {
            throw new BusinessException(500, "未配置 AI_API_KEY 环境变量");
        }

        byte[] imageBytes = stepTimer.measure(timingContext, "read_image_bytes", image::getBytes);
        String dataUri = stepTimer.measure(
                timingContext,
                "base64_encode",
                () -> Base64Util.encodeToDataUri(imageBytes, image.getContentType())
        );

        return new PreparedImage(image.getContentType(), image.getSize(), imageBytes, dataUri);
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(400, "图片文件不能为空");
        }

        String contentType = image.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new BusinessException(400, "仅支持JPG、PNG格式的图片");
        }

        long maxBytes = (long) mimoProperties.getMaxImageSizeMb() * 1024 * 1024;
        if (image.getSize() > maxBytes) {
            throw new BusinessException(400, "图片大小不能超过" + mimoProperties.getMaxImageSizeMb() + "MB");
        }
    }
}
