package io.quarkiverse.googlecloudservices.bigtable.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;

import io.quarkiverse.googlecloudservice.bigtable.api.BigtableClient;
import io.quarkiverse.googlecloudservices.bigtable.runtime.BigtableConfigProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.InjectionPointTransformerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;

public class BigtableClientProcessor {

    private static final Logger LOGGER = Logger.getLogger(BigtableClientProcessor.class.getName());

    private static final String FEATURE = "google-cloud-bigtable-client";

    @BuildStep
    public void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(new AdditionalBeanBuildItem(BigtableClient.class));
        beans.produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClasses(BigtableConfigProvider.class).build());
    }

    @BuildStep
    public void discoverInjectedBeans(BeanDiscoveryFinishedBuildItem beanDiscovery,
            BuildProducer<BigtableBuildItem> clients,
            BuildProducer<FeatureBuildItem> features,
            CombinedIndexBuildItem index) {
        Map<String, BigtableBuildItem> items = new HashMap<>();
        for (InjectionPointInfo injectionPoint : beanDiscovery.getInjectionPoints()) {
            AnnotationInstance clientAnnotation = injectionPoint.getRequiredQualifier(BigTableDotNames.BIGTABLE_CLIENT);
            if (clientAnnotation == null) {
                continue;
            }
            String clientName;
            AnnotationValue clientNameValue = clientAnnotation.value();
            if (clientNameValue == null || clientNameValue.asString().equals(BigtableClient.ELEMENT_NAME)) {
                // Determine the service name from the annotated element
                if (clientAnnotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                    clientName = clientAnnotation.target().asField().name();
                } else if (clientAnnotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    MethodParameterInfo param = clientAnnotation.target().asMethodParameter();
                    clientName = param.method().parameterName(param.position());
                    if (clientName == null) {
                        throw new DeploymentException("Unable to determine the client name from the parameter at position "
                                + param.position()
                                + " in method "
                                + param.method().declaringClass().name() + "#" + param.method().name()
                                +
                                "() - compile the class with debug info enabled (-g) or parameter names recorded (-parameters), or use BigtableClient#value() to specify the service name");
                    }
                } else {
                    // This should never happen because @BigtableClient has @Target({ FIELD, PARAMETER })
                    throw new IllegalStateException(clientAnnotation + " may not be declared at " + clientAnnotation.target());
                }
            } else {
                clientName = clientNameValue.asString();
            }
            if (clientName.trim().isEmpty()) {
                throw new DeploymentException(
                        "Invalid @BigtableClient `" + injectionPoint.getTargetInfo() + "` - client name cannot be empty");
            }
            BigtableBuildItem item;
            if (items.containsKey(clientName)) {
                item = items.get(clientName);
            } else {
                item = new BigtableBuildItem(clientName);
                items.put(clientName, item);
            }
            Type injectionType = injectionPoint.getRequiredType();
            if (injectionType.name().equals(BigTableDotNames.DATA_CLIENT)) {
                item.addClient(new BigtableBuildItem.ClientInfo(BigTableDotNames.DATA_CLIENT));
            }
        }
        if (!items.isEmpty()) {
            for (BigtableBuildItem item : items.values()) {
                clients.produce(item);
                LOGGER.debugf("Detected client associated with the '%s' configuration prefix", item.getClientName());
            }
            features.produce(new FeatureBuildItem(FEATURE));
        }
    }

    @BuildStep
    public void generateBigtableClientProducers(List<BigtableBuildItem> clients,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        for (BigtableBuildItem client : clients) {
            for (BigtableBuildItem.ClientInfo clientInfo : client.getClients()) {
                String clientName = client.getClientName();
                SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                        .configure(clientInfo.className)
                        .addQualifier().annotation(BigTableDotNames.BIGTABLE_CLIENT).addValue("value", clientName).done()
                        .scope(Singleton.class)
                        .unremovable()
                        .forceApplicationClass()
                        .addType(BigtableDataClient.class)
                        .creator(new Consumer<MethodCreator>() {
                            @Override
                            public void accept(MethodCreator mc) {
                                BigtableClientProcessor.this.generateClientProducer(mc, clientName);
                            }
                        });
                syntheticBeans.produce(configurator.done());
            }
        }
    }

    @BuildStep
    InjectionPointTransformerBuildItem transformInjectionPoints() {
        return new InjectionPointTransformerBuildItem(new InjectionPointsTransformer() {

            @Override
            public void transform(TransformationContext ctx) {
                AnnotationInstance clientAnnotation = Annotations.find(ctx.getQualifiers(), BigTableDotNames.BIGTABLE_CLIENT);
                if (clientAnnotation != null && clientAnnotation.value() == null) {
                    String clientName = null;
                    if (ctx.getTarget().kind() == AnnotationTarget.Kind.FIELD) {
                        clientName = clientAnnotation.target().asField().name();
                    } else if (ctx.getTarget().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                        MethodParameterInfo param = clientAnnotation.target().asMethodParameter();
                        // We don't need to check if parameter names are recorded - that's validated elsewhere
                        clientName = param.method().parameterName(param.position());
                    }
                    if (clientName != null) {
                        ctx.transform().remove(BigTableDotNames::isBigtableClient)
                                .add(BigTableDotNames.BIGTABLE_CLIENT, AnnotationValue.createStringValue("value", clientName))
                                .done();
                    }
                }
            }

            @Override
            public boolean appliesTo(Type requiredType) {
                return true;
            }

        });
    }

    private void generateClientProducer(MethodCreator mc, String clientName) {
        ResultHandle name = mc.load(clientName);
        ResultHandle result = mc.invokeStaticMethod(BigTableDotNames.CREATE_CLIENT_METHOD, name);
        mc.returnValue(result);
        mc.close();
    }

}
