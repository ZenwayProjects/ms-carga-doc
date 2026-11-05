package co.com.bancolombia.s3.config;

import co.com.bancolombia.s3.config.model.S3ConnectionProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.time.Duration;

@Configuration
public class S3Config {

    @Profile({"dev", "cer", "pdn"})
    @Bean
    public S3Client s3Client(S3ConnectionProperties s3Properties, ObjectProvider<MetricPublisher> publisherProvider) {
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(WebIdentityTokenFileCredentialsProvider.create())
                .region(Region.of(s3Properties.region()));
        MetricPublisher publisher = publisherProvider.getIfAvailable();
        if (publisher != null) {
            builder.overrideConfiguration(o -> o.addMetricPublisher(publisher));
        }
        return builder.build();
    }

    @Profile("local")
    @Bean
    public S3Client localS3Client(S3ConnectionProperties s3Properties, ObjectProvider<MetricPublisher> publisherProvider) {
        S3ClientBuilder builder = S3Client.builder()
                .endpointOverride(URI.create(s3Properties.endpoint()))
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .region(Region.of(s3Properties.region() != null ? s3Properties.region() : "us-east-1"))
                .overrideConfiguration(o -> {
                    o.apiCallAttemptTimeout(Duration.ofSeconds(10));
                    o.apiCallTimeout(Duration.ofSeconds(30));
                    publisherProvider.ifAvailable(o::addMetricPublisher);
                });
        return builder.build();
    }

}
