package com.opsbrain.aianalysis.config;

import com.anthropic.bedrock.backends.BedrockMantleBackend;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the Anthropic client pointed at AWS Bedrock.
 *
 * BedrockMantleBackend.fromEnv() reads AWS_REGION and resolves credentials via
 * the standard AWS provider chain — which on EKS means IRSA: the pod's
 * ServiceAccount is bound to an IAM role allowed to call bedrock:InvokeModel.
 * No keys in config.
 */
@Configuration
public class BedrockClientConfig {

    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.builder()
                .backend(BedrockMantleBackend.fromEnv())
                .build();
    }
}