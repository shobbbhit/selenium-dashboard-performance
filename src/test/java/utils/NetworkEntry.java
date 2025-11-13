package utils;

public class NetworkEntry {
    private String requestId;
    private String requestUrl;
    private String method;
    private long timestamp;
    private Integer responseStatus;
    private String responseUrl;
    private String mimeType;
    private String responseBody; // full body when available
    private String requestHeaders;
    private String responseHeaders;

    public static NetworkEntry fromRequestEvent(String requestId, String url, String method, long ts, String headersJson) {
        NetworkEntry e = new NetworkEntry();
        e.requestId = requestId;
        e.requestUrl = url;
        e.method = method;
        e.timestamp = ts;
        e.requestHeaders = headersJson;
        return e;
    }

    // getters / setters
    public String getRequestId() { return requestId; }
    public String getRequestUrl() { return requestUrl; }
    public String getMethod() { return method; }
    public long getTimestamp() { return timestamp; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseUrl() { return responseUrl; }
    public void setResponseUrl(String responseUrl) { this.responseUrl = responseUrl; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public String getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(String requestHeaders) { this.requestHeaders = requestHeaders; }
    public String getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }
}
