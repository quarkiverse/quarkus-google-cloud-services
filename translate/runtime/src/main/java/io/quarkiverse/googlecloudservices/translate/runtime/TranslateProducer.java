package io.quarkiverse.googlecloudservices.translate.runtime;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;

@ApplicationScoped
public class TranslateProducer {

    @Produces
    @Singleton
    @Default
    public Translate translate() throws IOException {
        return TranslateOptions.getDefaultInstance().getService();
    }
}
