package se.flinker.logback.extras;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

class Http {
    public Response get(String url) throws MalformedURLException, IOException {
        requireNonNull(url, "arg [url] can't be null");
        return get(url, Collections.<String, String>emptyMap());
    }

    public Response get(String url, Map<String, String> headers) throws MalformedURLException, IOException {
        requireNonNull(url, "first arg [url] can't be null");
        requireNonNull(headers, "second arg [headers] can't be null");
        return execute("GET", url, headers, (String)null);
    }

    public Response post(String url, Map<String, String> headers, String body) throws MalformedURLException, IOException {
        requireNonNull(url, "first arg [url] can't be null");
        requireNonNull(headers, "second arg [headers] can't be null");
        requireNonNull(body, "third arg [body] can't be null");
        return execute("POST", url, headers, body);
    }

    public Response post(String url, Map<String, String> headers, ByteArrayOutputStream body) throws MalformedURLException, IOException {
        requireNonNull(url, "first arg [url] can't be null");
        requireNonNull(headers, "second arg [headers] can't be null");
        requireNonNull(body, "third arg [body] can't be null");
        return execute("POST", url, headers, body);
    }

    private Response execute(String metod, String url, Map<String, String> headers, ByteArrayOutputStream body) throws MalformedURLException, IOException {
        OutputStream out = null;
        try {
            HttpURLConnection conn = createConnection(metod, url, headers);

            if (body != null) {
                byte[] buffer = body.toByteArray();
                out = conn.getOutputStream();
                out.write(buffer, 0, buffer.length);
            }

            return readResponse(conn);
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }

    private Response execute(String metod, String url, Map<String, String> headers, String body) throws MalformedURLException, IOException {
        BufferedWriter out = null;
        try {
            HttpURLConnection conn = createConnection(metod, url, headers);

            if (body != null && body.trim().length() > 0) {
                out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "utf-8"));
                out.write(body);
                out.flush();
            }

            return readResponse(conn);
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }
    
    protected HttpURLConnection createConnection(String metod, String url, Map<String, String> headers) throws IOException {
        URL endpoint = URI.create(url).toURL();
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod(metod);
        for (Entry<String, String> entry : headers.entrySet()) {
            conn.addRequestProperty(entry.getKey(), entry.getValue());
        }
        return conn;
    }
    
    protected Response readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader in = null;
        try {
            Response resp = new Response();
            resp.code = conn.getResponseCode();
            StringBuilder builder = new StringBuilder();
            if (resp.code >= 400) {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    in = new BufferedReader(new InputStreamReader(errorStream, "utf-8"));
                } else {
                    in = new BufferedReader(new NullBufferedReader());
                }
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            }

            int read;
            while ((read = in.read()) != -1) {
                builder.append((char) read);
            }

            resp.data = builder.toString();
            return resp;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
            }
        }
    }

    public static class Response {
        public int code;
        public String data;
        
        public Response() {
        }
        
        public Response(int code, String data) {
            this.code = code;
            this.data = data;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Response [code=").append(code).append(", data=")
                    .append(data).append("]");
            return builder.toString();
        }
    }
}

