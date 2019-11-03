package prestocloud.btrplace.tosca.model;

import java.util.Optional;

public class EdgeResourceTemplateDetails {

    public Optional<String> name;
    public String id;
    public String edgeType;
    public String location;
    public Optional<String> gps_coordinate;
    public String num_cpus;
    public Optional<String> disk_size;
    public String mem_size;
    public Optional<String> cpu_frequency;
    public Optional<String> price;
    public Optional<String> cameraSensor;
    public Optional<String> microphoneSensor;
    public Optional<String> temperatureSensor;
    public Optional<String> privatekey;
    public Optional<String> password;
    public String username;
}
