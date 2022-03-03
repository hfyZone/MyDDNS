package hfy.zone.entity;

/**
 * @author HanFeiYang
 * @date 2022/3/3 14:33
 */
public class AliAccessKey {
    private String regionID = "cn-hangzhou";
    private String AccessKeyID;
    private String AccessKeySecret;

    public String getRegionID() {
        return regionID;
    }

    public String getAccessKeyID() {
        return AccessKeyID;
    }

    public void setAccessKeyID(String accessKeyID) {
        AccessKeyID = accessKeyID;
    }

    public String getAccessKeySecret() {
        return AccessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        AccessKeySecret = accessKeySecret;
    }
}
