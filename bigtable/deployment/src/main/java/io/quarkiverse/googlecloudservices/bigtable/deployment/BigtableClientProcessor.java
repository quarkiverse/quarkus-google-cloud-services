package io.quarkiverse.googlecloudservices.bigtable.deployment;

import org.jboss.logging.Logger;

public class BigtableClientProcessor {

    private static final Logger LOGGER = Logger.getLogger(BigtableClientProcessor.class.getName());

    protected static final String FEATURE = "google-cloud-bigtable-client";

    /*
     * @BuildStep
     * public void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
     * beans.produce(new AdditionalBeanBuildItem(BigtableClient.class));
     * beans.produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClasses(BigtableConfigProvider.class).build());
     * }
     *
     * @BuildStep
     * public void discoverInjectedBeans(BeanDiscoveryFinishedBuildItem beanDiscovery,
     * BuildProducer<BigtableBuildItem> clients,
     * BuildProducer<FeatureBuildItem> features,
     * CombinedIndexBuildItem index) {
     *
     * Map<String, BigtableBuildItem> items = new HashMap<>();
     *
     * for (InjectionPointInfo injectionPoint : beanDiscovery.getInjectionPoints()) {
     * AnnotationInstance clientAnnotation = injectionPoint.getRequiredQualifier(BigTableDotNames.BIGTABLE_CLIENT);
     * if (clientAnnotation == null) {
     * continue;
     * }
     *
     * String clientName;
     * AnnotationValue clientNameValue = clientAnnotation.value();
     * if (clientNameValue == null || clientNameValue.asString().equals(BigtableClient.ELEMENT_NAME)) {
     * // Determine the service name from the annotated element
     * if (clientAnnotation.target().kind() == AnnotationTarget.Kind.FIELD) {
     * clientName = clientAnnotation.target().asField().name();
     * } else if (clientAnnotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
     * MethodParameterInfo param = clientAnnotation.target().asMethodParameter();
     * clientName = param.method().parameterName(param.position());
     * if (clientName == null) {
     * throw new DeploymentException("Unable to determine the client name from the parameter at position "
     * + param.position()
     * + " in method "
     * + param.method().declaringClass().name() + "#" + param.method().name()
     * +
     * "() - compile the class with debug info enabled (-g) or parameter names recorded (-parameters), or use GrpcClient#value() to specify the service name"
     * );
     * }
     * } else {
     * // This should never happen because @BigtableClient has @Target({ FIELD, PARAMETER })
     * throw new IllegalStateException(clientAnnotation + " may not be declared at " + clientAnnotation.target());
     * }
     * } else {
     * clientName = clientNameValue.asString();
     * }
     *
     * if (clientName.trim().isEmpty()) {
     * throw new DeploymentException(
     * "Invalid @BigtableClient `" + injectionPoint.getTargetInfo() + "` - client name cannot be empty");
     * }
     *
     * BigtableBuildItem item;
     * if (items.containsKey(clientName)) {
     * item = items.get(clientName);
     * } else {
     * item = new BigtableBuildItem(clientName);
     * items.put(clientName, item);
     * }
     *
     * Type injectionType = injectionPoint.getRequiredType();
     * if (injectionType.name().equals(BigTableDotNames.DATA_CLIENT)) {
     * item.addClient(new BigtableBuildItem.ClientInfo(BigTableDotNames.DATA_CLIENT));
     * }
     * }
     *
     * if (!items.isEmpty()) {
     * for (BigtableBuildItem item : items.values()) {
     * clients.produce(item);
     * LOGGER.debugf("Detected client associated with the '%s' configuration prefix", item.getClientName());
     * }
     * features.produce(new FeatureBuildItem(GRPC_CLIENT));
     * }
     * }
     *
     * @BuildStep
     * public void generateGrpcClientProducers(List<BigtableBuildItem> clients,
     * BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
     *
     * for (BigtableBuildItem client : clients) {
     * for (BigtableBuildItem.ClientInfo clientInfo : client.getClients()) {
     * SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
     * .configure(BigTableDotNames.DATA_CLIENT)
     * .addQualifier().annotation(BigTableDotNames.BIGTABLE_CLIENT).addValue("value", client.getClientName())
     * .done()
     * .scope(Singleton.class)
     * .unremovable()
     * .forceApplicationClass()
     * .creator(new Consumer<>() {
     *
     * @Override
     * public void accept(MethodCreator mc) {
     * BigtableClientProcessor.this.generateChannelProducer(mc, client.getClientName());
     * }
     * });
     * syntheticBeans.produce(configurator.done());
     * }
     * }
     * }
     *
     * private void generateChannelProducer(MethodCreator mc, String clientName) {
     * ResultHandle name = mc.load(clientName);
     * ResultHandle result = mc.invokeStaticMethod(BigTableDotNames.CREATE_CLIENT_METHOD, name);
     * mc.returnValue(result);
     * mc.close();
     * }
     */
}
