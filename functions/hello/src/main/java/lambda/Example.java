package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.atlassian.fugue.Option;
import com.atlassian.marketplace.client.MarketplaceClient;
import com.atlassian.marketplace.client.MarketplaceClientFactory;
import com.atlassian.marketplace.client.MpacException;
import com.atlassian.marketplace.client.api.VendorQuery;
import com.atlassian.marketplace.client.http.HttpConfiguration;
import lombok.ToString;

import static com.atlassian.fugue.Option.some;
import static java.lang.System.getenv;

public class Example {

    public static class ExampleRequest {
        String hello;

        public String getHello() {
            return hello;
        }

        public void setHello(String hello) {
            this.hello = hello;
        }

        public ExampleRequest(String hello) {
            this.hello = hello;
        }

        public ExampleRequest() {
        }
    }

    @ToString
    public static class ExampleResponse {
        String hello;

        public String getHello() {
            return hello;
        }

        public void setHello(String hello) {
            this.hello = hello;
        }

        public ExampleResponse(String hello) {
            this.hello = hello;
        }

        public ExampleResponse() {
        }
    }

    public ExampleResponse handler(ExampleRequest event, Context context) throws MpacException {
        HttpConfiguration.Credentials credentials = new HttpConfiguration.Credentials(
                getenv("MARKETPLACE_USER"), getenv("MARKETPLACE_PASSWORD"));

        HttpConfiguration config = HttpConfiguration.builder()
                .credentials(some(credentials))
                .build();

        MarketplaceClient client = MarketplaceClientFactory.createMarketplaceClient(config);

        return new ExampleResponse("" + client.vendors().find(VendorQuery.any()).totalSize());
    }

    public static void main(String[] args) throws MpacException {
        System.out.print(new Example().handler(null, null));
    }
}