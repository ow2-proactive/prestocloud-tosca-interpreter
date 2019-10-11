package prestocloud.model;

public class VmTypeCostRegistration implements Comparable<VmTypeCostRegistration> {
    public String vmTypeName;
    public double vmTypeCost;


    public VmTypeCostRegistration(String vmTypeName, double vmTypeCost) {
        this.vmTypeName = vmTypeName;
        this.vmTypeCost = vmTypeCost;
    }

    @Override
    public int compareTo(VmTypeCostRegistration o) {
        return Double.compare(vmTypeCost, o.vmTypeCost);
    }

    @Override
    public String toString() {
        return this.vmTypeName;
    }
}
