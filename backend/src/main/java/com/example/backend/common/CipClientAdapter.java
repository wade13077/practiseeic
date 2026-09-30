// package com.example.backend.common;

package com.esb.icrm.client.integration.esip.system.cip.adapter;

import com.esb.icrm.client.integration.esip.system.cip.CipApiClient;
import com.esb.icrm.client.integration.esip.system.cip.dto.request.FindCustomerInfoRequestBody;
import com.esb.icrm.client.integration.esip.system.cip.port.CipClientPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CipClientAdapter implements CipClientPort {

    private final CipApiClient cipApiClient;

    public CipClientAdapter(CipApiClient cipApiClient) {
        this.cipApiClient = cipApiClient;
    }

    @Override
    public List<Map<String, Object>> findCustomerInfo(
            FindCustomerInfoRequestBody requestBody,
            String operatorCode,
            String unitCode,
            String authorizerCode) {
        return cipApiClient.findCustomerInfo(requestBody, operatorCode, unitCode, authorizerCode);
    }

    @Override
    public List<Map<String, Object>> findCodeInfo(
            String operatorCode,
            String unitCode,
            String authorizerCode) {
        return cipApiClient.findCodeInfo(operatorCode, unitCode, authorizerCode);
    }
}