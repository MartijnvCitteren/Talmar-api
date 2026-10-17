package eu.talmar.api.adapter.out.ai.mistral;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The chat client of the Mistral adapter, built from Spring AI's auto-configured builder (model,
 * options and retry come from {@code spring.ai.*}). A class rather than properties because Spring
 * Boot only auto-configures the builder, not a client. The client merges the configured model
 * options into every call; calling the {@code ChatModel} directly with call options would drop
 * them.
 */
@Configuration
class MistralChatClientConfig {

  @Bean
  ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
    return chatClientBuilder.build();
  }
}
