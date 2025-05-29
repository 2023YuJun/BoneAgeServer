package com.example.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dicom")
public class DICOMConfig {
    private String localAeTitle;
    private String pacsAeTitle;
    private String pacsIp;
    private int pacsPort;
    private int downloadPort;

    // Getters and Setters
//    public String getLocalAeTitle() { return localAeTitle; }
//    public void setLocalAeTitle(String localAeTitle) { this.localAeTitle = localAeTitle; }
//    public String getPacsAeTitle() { return pacsAeTitle; }
//    public void setPacsAeTitle(String pacsAeTitle) { this.pacsAeTitle = pacsAeTitle; }
//    public String getPacsIp() { return pacsIp; }
//    public void setPacsIp(String pacsIp) { this.pacsIp = pacsIp; }
//    public int getPacsPort() { return pacsPort; }
//    public void setPacsPort(int pacsPort) { this.pacsPort = pacsPort; }
}