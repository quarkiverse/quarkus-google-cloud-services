package io.quarkiverse.googlecloudservices.it;

public class KeyValueParameter {

    public static KeyValueParameter of(String k, String v) {
        return new KeyValueParameter(k, v);
    }

    private String key;
    private String value;

    public KeyValueParameter() {
    }

    public KeyValueParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
