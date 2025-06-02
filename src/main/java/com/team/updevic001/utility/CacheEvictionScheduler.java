package com.team.updevic001.utility;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheEvictionScheduler {

    private final CacheManager cacheManager;


    @Scheduled(fixedRate = 30 * 60 * 1000) // hər 30 dəqiqə (millisaniyə ilə)
    public void evictAllCaches() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear(); // cache-i təmizlə
                System.out.println("Cleared cache: " + name);
            }
        }
    }
}
