package prestocloud.btrplace.tosca.model;

import java.util.Optional;

public class SshKey {

    private String fragmentName;
    private Optional<String> publicSshKey;

    public SshKey(String fragmentName,String publicSshKey) {
        this.fragmentName = fragmentName;
        this.publicSshKey = (publicSshKey == null) ? Optional.empty() : Optional.of(publicSshKey);
    }

    public String getFragmentName() {
        return this.fragmentName;
    }

    public boolean hasKey() {
        return this.publicSshKey.isPresent();
    }

    public String getPublicKey() {
        return  (this.hasKey()) ? this.publicSshKey.get() : "";
    }

}
