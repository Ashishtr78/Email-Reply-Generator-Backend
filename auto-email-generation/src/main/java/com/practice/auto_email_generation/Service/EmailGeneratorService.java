package com.practice.auto_email_generation.Service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientSsl;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.auto_email_generation.Entity.EmailRequest;

import lombok.val;

@Service
public class EmailGeneratorService {

	@Autowired
	private final WebClient webClient;

	public EmailGeneratorService(WebClient.Builder webClientBuilder) {
		super();
		this.webClient = webClientBuilder.build();
	}

	@Value("${gemini.api.url}")
	private String geminiApiUrl;
	@Value("${gemini.api.key}")
	private String geminiApiKey;

	public String generateEmailReply(EmailRequest emailRequest) {
		// build prompt
		String prompt = buildPrompt(emailRequest);
		// craft a request
		Map<String, Object> requestBody = Map.of(
			    "contents", List.of(
			        Map.of("parts", List.of(
			            Map.of("text", prompt)
			        ))
			    )
			);
		// Do request and get responce

		String response = webClient.post().uri(geminiApiUrl +"?key="+ geminiApiKey).header("Content-Type", "application/json")
				.bodyValue(requestBody)
				.retrieve().bodyToMono(String.class).block();
		// return response

		return extractResponseContent(response);

	}

	private String extractResponseContent(String response) {
		 try {
		        ObjectMapper mapper = new ObjectMapper();
		        JsonNode rootNode = mapper.readTree(response);

		      
		        JsonNode candidatesNode = rootNode.path("candidates");
		        if (!candidatesNode.isArray() || candidatesNode.isEmpty()) {
		            return "No candidates found.";
		        }

		        JsonNode firstCandidate = candidatesNode.get(0);
		        if (firstCandidate == null) {
		            return "No candidate response available.";
		        }

		      
		        JsonNode contentNode = firstCandidate.path("content");
		        JsonNode partsNode = contentNode.path("parts");

		       
		        if (!partsNode.isArray() || partsNode.isEmpty()) {
		            return "No content parts available.";
		        }

		      
		        return partsNode.get(0).path("text").asText("No response text available");

		    } catch (Exception e) {
		        return "Error: " + e.getMessage();
		    }
	}

	private String buildPrompt(EmailRequest emailRequest) {
		StringBuilder prompt = new StringBuilder();
		prompt.append(
				" give me proper generated email from this tone  ");
		if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
			prompt.append("use a ").append(emailRequest.getTone()).append(" tone.");
		}
		prompt.append("\n text \n ").append(emailRequest.getEmailContent());
		System.out.println(prompt);
		return prompt.toString();
	}
}
