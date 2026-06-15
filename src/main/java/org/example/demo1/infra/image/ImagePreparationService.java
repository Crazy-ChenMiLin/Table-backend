package org.example.demo1.infra.image;

import org.example.demo1.common.context.RecognitionTimingContext;
import org.example.demo1.common.timing.RecognitionStepTimer;
import org.example.demo1.config.AiProperties;
import org.example.demo1.entity.bo.PreparedImage;
import org.example.demo1.exception.BusinessException;
import org.example.demo1.util.Base64Util;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImagePreparationService {

    private final AiProperties aiProperties;
    private final RecognitionStepTimer stepTimer;

    public ImagePreparationService(AiProperties aiProperties, RecognitionStepTimer stepTimer) {
        this.aiProperties = aiProperties;
        this.stepTimer = stepTimer;
    }

    public PreparedImage prepare(MultipartFile image, RecognitionTimingContext timingContext) throws Exception {
        //1.图片validateImage校验
        stepTimer.measureVoid(timingContext, "validate_image", () -> {//（提前用ctx记录时间）
            validateImage(image);//校验
            return null;
        });
        //2.检查AI_API_KEY环境变量
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new BusinessException(500, "未配置 AI_API_KEY 环境变量");
        }
        //3.图片base64转码
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

        long maxBytes = (long) aiProperties.getMaxImageSizeMb() * 1024 * 1024;
        if (image.getSize() > maxBytes) {
            throw new BusinessException(400, "图片大小不能超过" + aiProperties.getMaxImageSizeMb() + "MB");
        }
    }
}
