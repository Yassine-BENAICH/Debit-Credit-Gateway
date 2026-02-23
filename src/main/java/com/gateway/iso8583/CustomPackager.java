package com.gateway.iso8583;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.packager.ISO87APackager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Component
public class CustomPackager {
    
    private ISOPackager packager;
    private ISOPackager defaultPackager;
    
    @Value("${gateway.iso.packager.type:custom}")
    private String packagerType;
    
    @Value("${gateway.iso.packager.config:classpath:iso8583/iso-packager.xml}")
    private String packagerConfig;
    
    private final ResourceLoader resourceLoader;
    
    public CustomPackager(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.defaultPackager = new ISO87APackager();
    }
    
    @PostConstruct
    public void init() {
        try {
            if ("custom".equals(packagerType)) {
                Resource resource = resourceLoader.getResource(packagerConfig);
                InputStream is = resource.getInputStream();
                this.packager = new GenericPackager(is);
            } else {
                this.packager = defaultPackager;
            }
        } catch (Exception e) {
            this.packager = defaultPackager;
        }
    }
    
    public ISOPackager getPackager() {
        return packager;
    }
    
    public byte[] pack(ISOMsg isoMsg) throws ISOException {
        isoMsg.setPackager(packager);
        return isoMsg.pack();
    }
    
    public ISOMsg unpack(byte[] data) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(packager);
        isoMsg.unpack(data);
        return isoMsg;
    }
    
    public ISOMsg createMessage(String mti) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setMTI(mti);
        isoMsg.setPackager(packager);
        return isoMsg;
    }
}