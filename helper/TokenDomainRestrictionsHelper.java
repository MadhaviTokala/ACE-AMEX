package com.americanexpress.smartserviceengine.helper;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.etv.issuetoken.data.TokenDomainControlVO;
import com.americanexpress.smartserviceengine.common.constants.ServiceConstants;
import com.americanexpress.smartserviceengine.common.vo.TokenDomainRestrictionsVO;
import com.americanexpress.smartserviceengine.dao.TokenDomainRestrictionsDAO;

@Service
public class TokenDomainRestrictionsHelper{
	
	@Autowired
	private TokenDomainRestrictionsDAO tokenDomainRestrictionsDAO;
	
	public Map<String,Object> read(String partnerID) {
		TokenDomainControlVO tokenDomainControlVO = null;
		List<TokenDomainRestrictionsVO> domainRestrictionsVOList = tokenDomainRestrictionsDAO.getDomainRestrictionRules(partnerID);
		//List<TokenDomainControlVO> tokenDomainControlVOList = new ArrayList<TokenDomainControlVO>();
		//List<String> tokenDomainControlRecordList = new ArrayList<String>();
		Map<String,Object> tokenDomainControlVOMap = new HashMap<String,Object>();
		
		if(domainRestrictionsVOList != null && domainRestrictionsVOList.size() >0){
			tokenDomainControlVO = new TokenDomainControlVO();
			Iterator<TokenDomainRestrictionsVO> iterator = domainRestrictionsVOList.iterator();
			while (iterator.hasNext()) {
				TokenDomainRestrictionsVO tokenDomainRestrictionsVO = (TokenDomainRestrictionsVO)iterator.next();
				if(StringUtils.isNotBlank(tokenDomainRestrictionsVO.getPartnerId())){
					tokenDomainControlVO = (TokenDomainControlVO) tokenDomainControlVOMap.get(tokenDomainRestrictionsVO.getPartnerId());
					if(tokenDomainControlVO==null){
						tokenDomainControlVO = new TokenDomainControlVO();	
						tokenDomainControlVOMap.put(tokenDomainRestrictionsVO.getPartnerId().trim(), tokenDomainControlVO);
					}
					
					if(ServiceConstants.RULE_USAGE_COUNT.equals(tokenDomainRestrictionsVO.getSourceFieldName())){
						tokenDomainControlVO.setUsageCountControl(tokenDomainRestrictionsVO.getTargetFieldName());
					}else if(ServiceConstants.RULE_USAGE_DAYS.equals(tokenDomainRestrictionsVO.getSourceFieldName())){
						tokenDomainControlVO.setExpireControl(Integer.valueOf(tokenDomainRestrictionsVO.getTargetFieldName()));
					}else if(ServiceConstants.RULE_AMT_FIELD.equals(tokenDomainRestrictionsVO.getSourceFieldName())){
						tokenDomainControlVO.setAmountControl(ServiceConstants.RULE_OPERATOR_EQ);
					}
					
					
					
				}				
			}
			
		/*	for(TokenDomainRestrictionsVO tokenDomainRestrictionsVO : domainRestrictionsVOList){
				if(ServiceConstants.RULE_USAGE_COUNT.equals(tokenDomainRestrictionsVO.getSourceFieldName())){
					tokenDomainControlVO.setUsageCountControl(tokenDomainRestrictionsVO.getTargetFieldName());
				}else if(ServiceConstants.RULE_USAGE_DAYS.equals(tokenDomainRestrictionsVO.getSourceFieldName())){
					tokenDomainControlVO.setExpireControl(Integer.valueOf(tokenDomainRestrictionsVO.getTargetFieldName()));
				}else if(ServiceConstants.RULE_AMT_FIELD.equals(tokenDomainRestrictionsVO.getSourceFieldName())){
					tokenDomainControlVO.setAmountControl(ServiceConstants.RULE_OPERATOR_EQ);
				}
				if(StringUtils.isNotBlank(tokenDomainRestrictionsVO.getPartnerId())){
					tokenDomainControlVOMap.put(tokenDomainRestrictionsVO.getPartnerId().trim(), tokenDomainControlVO);	
				}				
			}*/
		}
		return tokenDomainControlVOMap;
	}
	
}