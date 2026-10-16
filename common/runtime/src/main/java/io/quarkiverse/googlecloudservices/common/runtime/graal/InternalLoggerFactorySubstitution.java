package io.quarkiverse.googlecloudservices.common.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory;

@TargetClass(className = "io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory")
public final class InternalLoggerFactorySubstitution {
    @Substitute
    private static InternalLoggerFactory useLog4J2LoggerFactory(String name) {
        return null;
    }

    @Substitute
    private static InternalLoggerFactory useLog4JLoggerFactory(String name) {
        return null;
    }
}
