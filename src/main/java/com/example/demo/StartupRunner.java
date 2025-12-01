package com.example.demo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class StartupRunner implements CommandLineRunner {

    private final WebClient webClient;

    public StartupRunner(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Override
    public void run(String... args) throws Exception {

        log.info("Generating webhook…");

        WebhookResponse response = webClient.post()
                .uri("https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA")
                .bodyValue(new GenerateWebhookRequest("John Doe", "REG12347", "john@example.com"))
                .retrieve()
                .bodyToMono(WebhookResponse.class)
                .block();

        assert response != null;
        log.info("Webhook = {}", response.getWebhook());
        log.info("Token = {}", response.getAccessToken());

        String finalQuery = """
                SELECT 
                    d.DEPARTMENT_NAME,
                    AVG(TIMESTAMPDIFF(YEAR, e.DOB, CURDATE())) AS AVERAGE_AGE,
                    GROUP_CONCAT(CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) ORDER BY e.EMP_ID LIMIT 10) AS EMPLOYEE_LIST
                FROM DEPARTMENT d
                JOIN EMPLOYEE e ON d.DEPARTMENT_ID = e.DEPARTMENT
                JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID
                WHERE p.AMOUNT > 70000
                GROUP BY d.DEPARTMENT_ID, d.DEPARTMENT_NAME
                ORDER BY d.DEPARTMENT_ID DESC;
                """;

        SubmissionRequest submission = new SubmissionRequest(finalQuery);

        String result = webClient.post()
                .uri(response.getWebhook())
                .header("Authorization", response.getAccessToken())
                .bodyValue(submission)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Webhook response: {}", result);
    }

    @Data
    static class GenerateWebhookRequest {
        private final String name;
        private final String regNo;
        private final String email;
    }

    @Data
    static class SubmissionRequest {
        private final String finalQuery;
    }

    @Data
    static class WebhookResponse {
        private String webhook;
        private String accessToken;
    }
}
