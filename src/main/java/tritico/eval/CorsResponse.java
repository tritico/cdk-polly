package tritico.eval;

import software.amazon.awscdk.services.apigateway.*;

import java.util.HashMap;
import java.util.Map;

public class CorsResponse implements IntegrationResponse {

    @Override
    public String getStatusCode() {
        return "200";
    }

    @Override
    public void setStatusCode(String s) {
    }

    @Override
    public ContentHandling getContentHandling() {
        return null;
    }

    @Override
    public void setContentHandling(ContentHandling contentHandling) {

    }

    @Override
    public Map<String, String> getResponseParameters() {
        HashMap<String, String> responseParams = new HashMap<String, String>();
        responseParams.put("method.response.header.Access-Control-Allow-Headers", "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token,X-Amz-User-Agent'");
        responseParams.put("method.response.header.Access-Control-Allow-Origin", "'*'");
        responseParams.put("method.response.header.Access-Control-Allow-Credentials", "'false'");
        responseParams.put("method.response.header.Access-Control-Allow-Methods", "'OPTIONS,GET,PUT,POST,DELETE'");
        return responseParams;
    }

    @Override
    public void setResponseParameters(Map<String, String> map) {}

    @Override
    public Map<String, String> getResponseTemplates() {
        return null;
    }

    @Override
    public void setResponseTemplates(Map<String, String> map) {
    }


}
