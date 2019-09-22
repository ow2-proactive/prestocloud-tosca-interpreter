package prestocloud.btrplace.tosca.model;


public class RegionCapacityDescriptor implements Comparable<RegionCapacityDescriptor> {

    private String region;
    private String cpucapacity;
    private String memorycapacity;
    private String diskcapacity;
    public static final int INFINITY = Integer.MAX_VALUE -1; // Silly trick to prevent choco-solver from aborting because of bounds reduction reason.

    public RegionCapacityDescriptor(String region, String cpucapacity, String memorycapacity, String diskcapacity)  {
       this.region = region;
       this.cpucapacity = cpucapacity;
       this.memorycapacity = memorycapacity;
       this.diskcapacity = diskcapacity;
    }

    public String getRegion() {
        return region;
    }

//    @Override
    public int compareTo(RegionCapacityDescriptor o) {
        return this.region.compareTo(o.getRegion()) ;
    }

    public int getCpuCapacity() {
        if (this.cpucapacity == null) {
            return INFINITY; //Integer.MAX_VALUE;
        } else {
            return Integer.parseInt(this.cpucapacity);
        }
    }

    public int getMemoryCapacity() {
        if (this.memorycapacity == null) {
            return INFINITY; //Integer.MAX_VALUE;
        } else {
            String[] result = this.memorycapacity.split("");
            if (result.length == 2) {
                if (result[1].equals("MB")) {
                    return Integer.parseInt(this.memorycapacity) / 1024;
                } else {
                    return Integer.parseInt(this.memorycapacity);
                }
            } else {
                return  INFINITY; //Integer.MAX_VALUE;
            }
        }
    }

    public int getDiskCapacity() {
        if (this.diskcapacity == null) {
            return  INFINITY;// Integer.MAX_VALUE;
        } else {
            String[] result = this.diskcapacity.split("");
            if (result.length == 2) {
                if (result[1].equals("MB")) {
                    return Integer.parseInt(this.diskcapacity) / 1024;
                } else {
                    return Integer.parseInt(this.diskcapacity);
                }
            } else {
                return  INFINITY; //Integer.MAX_VALUE;
            }
        }
    }


}
