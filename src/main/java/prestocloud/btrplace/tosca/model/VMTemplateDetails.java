package prestocloud.btrplace.tosca.model;

import lombok.Getter;
import lombok.Setter;

public class VMTemplateDetails {

    @Getter @Setter
    public String id;
    @Getter @Setter
    public String name;
    @Getter @Setter
    public String cloud;
    @Getter @Setter
    public String region;
    @Getter @Setter
    public String geolocation;
    @Getter @Setter
    public double price;

    public VMTemplateDetails(String id, String name, String cloud, String region, String geolocation, double price) {
        this.id = id;
        this.name = name;
        this.cloud = cloud;
        this.region = region;
        this.geolocation = geolocation;
        this.price = price;
    }
}
