package com.example.pdfsum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class HuggingFaceService {

    private final WebClient webClient;
    private final String model;

    public HuggingFaceService(@Value("${HF_API_TOKEN}") String apiToken,
                              @Value("${HF_MODEL}") String model)
    {
        if(apiToken == null || apiToken.isBlank())
        {
            throw new IllegalStateException("Missing Hugging Face API Token");
        }

        this.model = model;
        this.webClient = WebClient.builder()
                        .baseUrl("https://api-inference.huggingface.co/models")
                        //.baseUrl("https://api.huggingface.co/inference/")
                        .defaultHeader("Authorization", "Bearer " + apiToken)
                        .build();
    }

    public String summarize(String input, int maxLength)
    {
        if(input == null || input.isBlank())
            return "";

        Map<String, Object> payload = Map.of("inputs", input,
                                            "parameters", Map.of(
                                                    "max_length", maxLength,
                                                    "min_length", 30,
                                                    "do_sample", false
                                            ));

        Mono<String[]> responseMono = webClient.post()
                                    .uri("/{model}", model)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .bodyValue(payload)
                                    .retrieve()
                                    .bodyToMono(String[].class)
                                    .timeout(Duration.ofSeconds(60));

        try{
            String[] arr = responseMono.block();
            if(arr == null || arr.length == 0)
                    return "";

            return arr[0];
        }
        catch(Exception e)
        {
            try
            {
                Map[] maps =  webClient.post()
                        .uri("/{model}", model)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(Map[].class)
                        .timeout(Duration.ofSeconds(60))
                        .block();

                if(maps != null && maps.length>0) {
                    Object maybeSummary = maps[0].get("summary_text");
                    if (maybeSummary != null)
                        return maybeSummary.toString();

                    maybeSummary = maps[0].get("generated_text");
                    if (maybeSummary != null)
                        return maybeSummary.toString();

                    //TODO: is there a better way to handle this?
                    StringBuilder sb = new StringBuilder();
                    for (Map map : maps) {
                        sb.append(map.toString()).append("\n");
                    }

                    return sb.toString();
                }
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }

            e.printStackTrace();
            return "";
        }
    }
}
