package prestocloud.model.generator;

import net.minidev.json.JSONObject;

public class CloudListRegistration {

    private String cloudName;
    private String cloudType;
    private String accessKey;
    private String image;
    private String instanceType;
    private String region;

    private String networkName;
    private String subnetId;

    public CloudListRegistration(JSONObject jsonObjectFromCloudList) {
        cloudName = jsonObjectFromCloudList.getAsString("CLOUD_NAME");
        cloudType = jsonObjectFromCloudList.getAsString("CLOUD_TYPE");
        networkName = "network_" + jsonObjectFromCloudList.getAsString("CLOUD_NAME");
        subnetId = jsonObjectFromCloudList.getAsString("SUBNET_ID");
        if (getCloudType().equalsIgnoreCase("aws")) {
            cloudType = "amazon";
            region = jsonObjectFromCloudList.getAsString("AWS_DEFAULT_REGION");
            accessKey = jsonObjectFromCloudList.getAsString("AWS_ACCESS_KEY_ID");
            image = jsonObjectFromCloudList.getAsString("AWS_GOLDEN_IMAGE");
            instanceType = jsonObjectFromCloudList.getAsString("AWS_INSTANCE_TYPE");
        } else if (getCloudType().equalsIgnoreCase("azure")) {
            cloudType = "azure";
            region = jsonObjectFromCloudList.getAsString("AZ_LOCATION");
            accessKey = jsonObjectFromCloudList.getAsString("AZ_ACCESS_USER");
            image = jsonObjectFromCloudList.getAsString("AZ_GOLDEN_IMAGE");
            instanceType = jsonObjectFromCloudList.getAsString("AZ_SIZE");
        } else if (getCloudType().equalsIgnoreCase("openstack")) {
            cloudType = "openstack";
            region = jsonObjectFromCloudList.getAsString("OS_REGION_NAME");
            accessKey = jsonObjectFromCloudList.getAsString("OS_USERNAME");
            image = jsonObjectFromCloudList.getAsString("OS_GOLDEN_IMAGE");
            instanceType = jsonObjectFromCloudList.getAsString("OS_FLAVOR");
        } else {
            throw new IllegalArgumentException();
        }
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getImage() {
        return image;
    }

    public String getRegion() {
        return region;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public String getCloudType() {
        return cloudType;
    }
}
