package io.quarkiverse.googlecloudservices.spanner.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory;

@TargetClass(className = "io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory")
public final class InternalLoggerFactorySubstitution {
    @Substitute
    private static InternalLoggerFactory useLog4JLoggerFactory(String name) {
        return null;
    }
}
