package com.alphatica.alis.studio.view.tools;

import com.alphatica.alis.studio.Constants;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.studio.Constants.BACKEND_URL;
import static com.alphatica.alis.studio.view.tools.SwingHelper.runInBackground;

public class ErrorReporter {

    public static void reportError(String message, Exception e) {
        runInBackground(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost request = prepareRequest(message, e);
                httpClient.execute(request, response -> null);
            } catch (Exception ex) {
                System.err.println("Error while reporting: " + ex);
            }
        });
    }

    private static HttpPost prepareRequest(String message, Exception e) {
        List<String> lines = prepareLines(message, e);
        String json = prepareContent(lines);
        HttpPost request = new HttpPost(BACKEND_URL + "/error/report");
        request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        return request;
    }

    private static String prepareContent(List<String> lines) {
        ErrorReport report = new ErrorReport(lines);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(report);
    }

    private static List<String> prepareLines(String message, Exception e) {
        List<String> lines = new ArrayList<>();
        lines.add("Version: " + Constants.STUDIO_BUILD);
        lines.add("Error message: " + message);
        describeException(lines, e);
        return lines;
    }

    private static void describeException(List<String> lines, Exception e) {
        Throwable describing = e;
        while(describing != null) {
            lines.add("Exception: " + describing + " " + describing.getMessage());
            addStackTrace(lines, describing);
            describing = describing.getCause();
        }
    }

    private static void addStackTrace(List<String> lines, Throwable e) {
        lines.add(e.getClass().getName());
        for (StackTraceElement element : e.getStackTrace()) {
            lines.add(element.getFileName() + ":" + element.getLineNumber() + " " + element.getMethodName());
        }
    }

    private ErrorReporter() {}
}

record ErrorReport(List<String> content) {}
