package com.giguniverse.backend.Onboarding.Sumsub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@Service
public class SumsubService {

    @Value("${sumsub.token}")
    private String appToken;

    @Value("${sumsub.secretkey}")
    private String secretKey;

    public String generateFreelancerWebSdkPermalink(String userId, String email) throws Exception {
        long ts = System.currentTimeMillis() / 1000;
        String path = "/resources/sdkIntegrations/levels/-/websdkLink";
        String url = "https://api.sumsub.com" + path;

        String body = "{\"levelName\":\"GigsUniverse_Freelancer\",\"userId\":\"" + userId +
                    "\",\"ttlInSecs\":1800,\"applicantIdentifiers\":{\"email\":\"" + email + "\"}}";

        // Generate HMAC SHA256 signature
        String signature = sign(ts, "POST", path, body);


        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("X-App-Token", appToken)
            .header("X-App-Access-Ts", String.valueOf(ts))
            .header("X-App-Access-Sig", signature)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            return json.getString("url");
        } else {
            throw new RuntimeException("Sumsub API error [" + response.statusCode() + "]: " + response.body());
        }
    }

    public String generateEmployerWebSdkPermalink(String userId, String email) throws Exception {
        long ts = System.currentTimeMillis() / 1000;
        String path = "/resources/sdkIntegrations/levels/-/websdkLink";
        String url = "https://api.sumsub.com" + path;

        String body = "{\"levelName\":\"GigsUniverse_Employer\",\"userId\":\"" + userId +
                    "\",\"ttlInSecs\":1800,\"applicantIdentifiers\":{\"email\":\"" + email + "\"}}";

        // Generate HMAC SHA256 signature
        String signature = sign(ts, "POST", path, body);


        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("X-App-Token", appToken)
            .header("X-App-Access-Ts", String.valueOf(ts))
            .header("X-App-Access-Sig", signature)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            return json.getString("url");
        } else {
            throw new RuntimeException("Sumsub API error [" + response.statusCode() + "]: " + response.body());
        }
    }

    private String sign(long ts, String method, String path, String body) throws Exception {
        String message = ts + method + path + body;
        Mac hasher = Mac.getInstance("HmacSHA256");
        hasher.init(new SecretKeySpec(secretKey.getBytes(), "HmacSHA256"));
        byte[] hash = hasher.doFinal(message.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString().toLowerCase(); 
    }

}
