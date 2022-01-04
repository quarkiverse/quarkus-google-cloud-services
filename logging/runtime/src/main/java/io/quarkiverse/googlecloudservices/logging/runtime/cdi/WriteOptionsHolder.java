package io.quarkiverse.googlecloudservices.logging.runtime.cdi;

import com.google.cloud.logging.Logging.WriteOption;

/**
 * Helper interface to make an array of options easily injectable.
 */
@FunctionalInterface
public interface WriteOptionsHolder {

    public WriteOption[] getOptions();

}
