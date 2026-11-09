package co.com.bancolombia.s3.config;

import co.com.bancolombia.s3.config.model.S3ConnectionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@RequiredArgsConstructor
@Profile("local")
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3BucketInitializer implements ApplicationRunner {

    private final S3Client s3Client;
    private final S3ConnectionProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        String bucketName = properties.bucketName();
        try {
            s3Client.headBucket(builder -> builder.bucket(bucketName));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                s3Client.createBucket(builder -> builder.bucket(bucketName));
            } else {
                throw e;
            }
        }
    }
}
