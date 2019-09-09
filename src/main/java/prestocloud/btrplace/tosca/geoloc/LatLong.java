package prestocloud.btrplace.tosca.geoloc;

import lombok.Getter;
import lombok.Setter;

public class LatLong {

    @Getter@Setter
    public double latitude;
    @Getter @Setter
    public double longitude;

    public LatLong(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
