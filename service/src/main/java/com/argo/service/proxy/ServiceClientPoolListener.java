package com.argo.service.proxy;

import com.argo.service.ServiceConfig;
import com.argo.service.listener.ServicePoolListener;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA.
 * User: yamingdeng
 * Date: 13-12-15
 * Time: 下午1:12
 */
@Component
public class ServiceClientPoolListener implements ServicePoolListener, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * key is the name of service
     * value is the server addresses of those service.
     */
    private ConcurrentHashMap<String, List<String>> servicesMap = new ConcurrentHashMap<String, List<String>>();
    private static AtomicLong shift = new AtomicLong();

    @Override
    public void onServiceChanged(String name, List<String> urls) {
        this.servicesMap.put(name, urls);
    }

    @Override
    public String select(String serviceName){
        List<String> servers = this.servicesMap.get(serviceName);
        if(servers != null && servers.size()>0){
            Long index = shift.incrementAndGet();
            index = index % servers.size();
            return servers.get(index.intValue());
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //从配置文件读取.
        Map<String, String> servers = ServiceConfig.instance.get(Map.class, "rmis");
        if (servers != null){
            Iterator<String> itor = servers.keySet().iterator();
            while (itor.hasNext()){
                String name = itor.next();
                String val = servers.get(name);
                this.onServiceChanged(name, Lists.newArrayList(val.split(",")));
            }
        }

        instance = this;
    }

    public  static ServiceClientPoolListener instance = null;
}