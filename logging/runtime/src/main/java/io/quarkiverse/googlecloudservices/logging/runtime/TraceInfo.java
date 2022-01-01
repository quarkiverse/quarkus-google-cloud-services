package io.quarkiverse.googlecloudservices.logging.runtime;

public class TraceInfo {

    private final String traceId;
    private final String spanId;

    public TraceInfo(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getTraceId() {
        return traceId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((spanId == null) ? 0 : spanId.hashCode());
        result = prime * result + ((traceId == null) ? 0 : traceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TraceInfo other = (TraceInfo) obj;
        if (spanId == null) {
            if (other.spanId != null)
                return false;
        } else if (!spanId.equals(other.spanId))
            return false;
        if (traceId == null) {
            if (other.traceId != null)
                return false;
        } else if (!traceId.equals(other.traceId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TraceInfo [spanId=" + spanId + ", traceId=" + traceId + "]";
    }
}
