package com.americanexpress.smartserviceengine.common.util;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PartnerIdMapping {

    private static AmexLogger LOG = AmexLogger.create(PartnerIdMapping.class);
    @Autowired
    private CacheManager cacheManager;

    /**
     * This method is used to get the partner name from the cache.
     *
     * @param partnerId
     * @param apiMsgId
     * @return
     */
    public String getPartnerName(String partnerId, String apiMsgId) {
        LOG.info(apiMsgId, "SmartServiceEngine", "PartnerIdMapping", "getPartnerName",
                "Start of getting getPartnerName from cache", AmexLogger.Result.success, "", "partnerId", partnerId);
        String partnerName = null;
        try {

            Cache cache = cacheManager.getCache(ApiConstants.PARTNER_ID_CACHE);
            Element element = cache.get(partnerId);

            if (element != null) {
                partnerName = (String) element.getObjectValue();
            }
            LOG.info(apiMsgId, "SmartServiceEngine", "PartnerIdMapping", "getPartnerName",
                    "End of getting getPartnerName from cache", AmexLogger.Result.success, "", "partnerName", partnerName);
        }catch(Exception ex){
            LOG.error(apiMsgId, "SmartServiceEngine", "PartnerIdMapping", "getPartnerName","getting getPartnerName from cache Failed",
                    AmexLogger.Result.failure, "Unexpected Exception while getting partnerName from cache", ex);
        }
        return partnerName;
    }

}
