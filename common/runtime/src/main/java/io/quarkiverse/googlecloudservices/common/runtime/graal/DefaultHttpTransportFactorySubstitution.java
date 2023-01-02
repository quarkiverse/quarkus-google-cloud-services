package io.quarkiverse.googlecloudservices.common.runtime.graal;

import java.util.function.BooleanSupplier;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.google.cloud.http.HttpTransportOptions$DefaultHttpTransportFactory", onlyWith = DefaultHttpTransportFactorySubstitution.OnlyIfInClassPath.class)
public final class DefaultHttpTransportFactorySubstitution {
    @Substitute
    public HttpTransport create() {
        // AppEngine HttpTransport didn't work on native image.
        // Anyway, AppEngine don't allow to deploy native image on it, so it's not an issue.
        return new NetHttpTransport();
    }

    public static class OnlyIfInClassPath implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.google.cloud.http.HttpTransportOptions$DefaultHttpTransportFactory", false,
                        Thread.currentThread().getContextClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
