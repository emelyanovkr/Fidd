package com.fidd.ydisk.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidd.ydisk.rest.models.Link;
import com.fidd.ydisk.rest.models.Resource;
import com.fidd.ydisk.rest.models.YandexApiError;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Client {
    private static final String DEFAULT_API_BASE = "https://cloud-api.yandex.net/v1/disk";

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final String apiBase;
    private final HttpClient httpClient;
    private final String oauthToken;
    private final ObjectMapper mapper;

    public Client(String oauthToken, ObjectMapper mapper) {
        this(oauthToken, mapper, DEFAULT_API_BASE, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    Client(String oauthToken, ObjectMapper mapper, String apiBase, HttpClient httpClient) {
        this.oauthToken = oauthToken;
        this.mapper = mapper;
        this.apiBase = apiBase;
        this.httpClient = httpClient;
    }

    private HttpRequest.Builder baseRequest(String endpoint) {
        return HttpRequest.newBuilder()
                .uri(URI.create(apiBase + endpoint))
                .header("Authorization", "OAuth " + oauthToken)
                .header("Accept", "application/json");
    }

    private <T> T send(HttpRequest request, Class<T> responseClass) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            try {
                YandexApiError apiError = mapper.readValue(response.body(), YandexApiError.class);
                throw new RuntimeException("Yandex Disk Error [" + response.statusCode() + "]: "
                        + apiError.message() + " - " + apiError.description());
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw e;
                }
                throw new RuntimeException("HTTP Error [" + response.statusCode() + "]: " + response.body());
            }
        }

        return mapper.readValue(response.body(), responseClass);
    }

    public Resource getResources(String path) throws Exception {
        // TODO: normal pagination
        String endpoint = "/resources?path=" + java.net.URLEncoder.encode(path, StandardCharsets.UTF_8.name()) +"&limit=200";
        HttpRequest request = baseRequest(endpoint).GET().build();
        return send(request, Resource.class);
    }

    public InputStream downloadFile(String remotePath) throws Exception {
        return downloadFileWithRange(remotePath, 0, null);
    }

    public InputStream downloadFileWithRange(String remotePath, long offset, Long limit) throws Exception {
        Link downloadLink = getDownloadLink(remotePath);
        URI uri = URI.create(downloadLink.href());

        return downloadOrRedirect(uri, offset, limit, 0);
    }

    protected InputStream downloadOrRedirect(URI uri, long offset, Long limit, int redirectCount) throws Exception {
        if (redirectCount > 5) {
            throw new RuntimeException("Too many redirects");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .GET();
        
        if (offset > 0 || limit != null) {
            String rangeHeader = "bytes=" + offset + "-";
            if (limit != null && limit > 0) {
                rangeHeader += (offset + limit - 1);
            }
            builder.header("Range", rangeHeader);
        }

        HttpRequest getRequest = builder.build();

        HttpResponse<java.io.InputStream> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofInputStream());

        //System.out.println(FMT.format(Instant.now()) + " Download response status: " + response.statusCode() + " for URI: " + uri);

        if (response.statusCode() >= 300 && response.statusCode() < 400) {
            String loc = response.headers().firstValue("location").orElseThrow(() -> new RuntimeException("Redirect without Location"));
            uri = uri.resolve(loc);
            return downloadOrRedirect(uri, offset, limit, ++redirectCount);
        } else if (response.statusCode() >= 400 && response.statusCode() != 416) {
            throw new RuntimeException("Download failed with status: " + response.statusCode());
        }
        return response.body();
    }

    public Link getUploadLink(String remotePath, boolean overwrite) throws Exception {
        // TODO: remotePath is interpolated into the query string without URL-encoding.
        //  Paths containing spaces, : (e.g. disk:/...), #, etc. will produce invalid requests or be parsed incorrectly
        //  by the server. Encode the query parameter value (as you already do in getResources).
        String endpoint = "/resources/upload?path=" + remotePath + "&overwrite=" + overwrite;
        HttpRequest request = baseRequest(endpoint).GET().build();
        return send(request, Link.class);
    }

    public void uploadFile(String remotePath, java.nio.file.Path localFile) throws Exception {
        Link uploadLink = getUploadLink(remotePath, true);

        HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(URI.create(uploadLink.href()))
                .method(uploadLink.method(), HttpRequest.BodyPublishers.ofFile(localFile))
                .build();

        HttpResponse<Void> response = httpClient.send(putRequest, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() != 201 && response.statusCode() != 202) {
            throw new RuntimeException("Upload failed with status: " + response.statusCode());
        }
    }

    public Link getDownloadLink(String remotePath) throws Exception {
        // TODO: remotePath is interpolated into the query string without URL-encoding, which will break for paths
        //  with reserved characters. Encode the query parameter value before building the endpoint.
        String endpoint = "/resources/download?path=" + remotePath;
        HttpRequest request = baseRequest(endpoint).GET().build();
        return send(request, Link.class);
    }
}
