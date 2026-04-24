import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizLeaderboard {
    private static final String REG_NO = "RA2311026050084";
    private static final String BASE_URL = "https://dev.api.go.vidalhealthtpa.com/srm-quiz-task";

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        Set<String> processedEvents = new HashSet<>();
        Map<String, Integer> participantScores = new HashMap<>();

        for (int poll = 0; poll < 10; poll++) {
            String url = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + poll;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            Pattern eventPattern = Pattern.compile("\\{[^{]*\"rowId\"[^}]*\\}");
            Matcher eventMatcher = eventPattern.matcher(response.body());

            while (eventMatcher.find()) {
                String eventJson = eventMatcher.group();
                String rowId = extractString(eventJson, "rowId");
                String participant = extractString(eventJson, "participant");
                int score = Integer.parseInt(extractNumber(eventJson, "score"));
                
                String uniqueKey = rowId + "-" + participant;
                if (processedEvents.add(uniqueKey)) {
                    participantScores.put(participant, participantScores.getOrDefault(participant, 0) + score);
                }
            }
            Thread.sleep(5000);
        }

        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(participantScores.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        StringBuilder leaderboardJson = new StringBuilder("[");
        for (int i = 0; i < sortedList.size(); i++) {
            Map.Entry<String, Integer> entry = sortedList.get(i);
            leaderboardJson.append(String.format("{\"participant\":\"%s\",\"totalScore\":%d}", entry.getKey(), entry.getValue()));
            if (i < sortedList.size() - 1) leaderboardJson.append(",");
        }
        leaderboardJson.append("]");

        String submitJson = String.format("{\"regNo\":\"%s\",\"leaderboard\":%s}", REG_NO, leaderboardJson.toString());

        HttpRequest submitRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(submitJson))
                .build();

        HttpResponse<String> submitResponse = client.send(submitRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println(submitResponse.body());
    }

    private static String extractString(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static String extractNumber(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? m.group(1) : "0";
    }
}