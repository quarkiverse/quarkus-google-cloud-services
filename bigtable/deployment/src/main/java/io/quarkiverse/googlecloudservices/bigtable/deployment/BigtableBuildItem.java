package io.quarkiverse.googlecloudservices.bigtable.deployment;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class BigtableBuildItem extends MultiBuildItem {

    private final String clientName;
    private final Set<ClientInfo> clients;

    public BigtableBuildItem(String name) {
        this.clientName = name;
        this.clients = new HashSet<>();
    }

    public Set<ClientInfo> getClients() {
        return clients;
    }

    public void addClient(ClientInfo client) {
        clients.add(client);
        clients.add(new ClientInfo(BigTableDotNames.DATA_CLIENT));
    }

    public String getClientName() {
        return clientName;
    }

    public static final class ClientInfo {

        public final DotName className;
        public final DotName implName;

        public ClientInfo(DotName className) {
            this(className, null);
        }

        public ClientInfo(DotName className, DotName implName) {
            this.className = className;
            this.implName = implName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(className);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ClientInfo other = (ClientInfo) obj;
            return Objects.equals(className, other.className);
        }

    }
}
