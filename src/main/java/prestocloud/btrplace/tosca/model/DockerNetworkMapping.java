package prestocloud.btrplace.tosca.model;

public class DockerNetworkMapping {

    private String target;
    private String publish;
    private InetProtocol protocol;

    public DockerNetworkMapping(String target, String publish, String protocol) {
        this.target = target;
        this.publish = publish;
        this.protocol = InetProtocol.valueOf(protocol);
    }

    public String getDockerCliArg() {
        return String.format(" -p %s:%s%s",publish,target, (protocol.equals(InetProtocol.TCP_UDP) ? "" : "/" + protocol.val));
    }

    public enum InetProtocol {
        TCP_UDP ("tcp_udp"),
        TCP ("tcp"),
        UDP ("udp");

        private String val;

        InetProtocol(String input) {
            this.val = input.toLowerCase();
        }
    }

    public String getPublicPort() {
        return this.publish;
    }
}

