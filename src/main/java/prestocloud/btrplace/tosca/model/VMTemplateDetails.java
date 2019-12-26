package prestocloud.btrplace.tosca.model;

import lombok.Getter;
import lombok.Setter;

public class VMTemplateDetails {

    @Getter
    @Setter
    public String id;
    @Getter
    @Setter
    public String name;
    @Getter
    @Setter
    public String cloud;
    @Getter
    @Setter
    public String region;
    @Getter
    @Setter
    public String geolocation;
    @Getter
    @Setter
    public double price;
    @Getter
    @Setter
    public String instanceName;
    @Getter
    @Setter
    public String cpuNum;
    @Getter
    @Setter
    public String cpuFreq;
    @Getter
    @Setter
    public String mem;
    @Getter
    @Setter
    public String diskSize;

    public VMTemplateDetails(String id, String name, String cloud, String region, String geolocation, double price, String instanceName, String cpuNum, String cpuFreq, String mem, String diskSize) {
        this.id = id;
        this.name = name;
        this.cloud = cloud;
        this.region = region;
        this.geolocation = geolocation;
        this.price = price;
        this.instanceName = instanceName;
        this.cpuNum = cpuNum;
        this.cpuFreq = cpuFreq;
        this.mem = mem;
        this.diskSize = diskSize;
    }
}
