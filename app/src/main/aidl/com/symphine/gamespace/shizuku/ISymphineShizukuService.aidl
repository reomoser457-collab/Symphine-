package com.symphine.gamespace.shizuku;

interface ISymphineShizukuService {
    int getUid();
    String getSetting(String namespaceName, String key);
    String putSetting(String namespaceName, String key, String value);
    String deleteSetting(String namespaceName, String key);
    String getSystemProperty(String propName);
    String setAngleFeatureOverride(String featureValue);
    String getWmSize();
    String setWmSize(String widthXHeight);
    String resetWmSize();
    String getWmDensity();
    String setWmDensity(int dpi);
    String resetWmDensity();
    String compilePackage(String packageName, String mode);
    String setGameMode(String packageName, String mode);
    String getGameServiceDump(String packageName);
    String getDisplayDump();
    String getThermalDump();
    String forceStopGamePackage(String packageName);
    void destroy();
}
