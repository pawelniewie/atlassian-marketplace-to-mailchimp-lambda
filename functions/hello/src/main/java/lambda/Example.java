package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.atlassian.fugue.Option;
import com.atlassian.marketplace.client.MarketplaceClient;
import com.atlassian.marketplace.client.MarketplaceClientFactory;
import com.atlassian.marketplace.client.MpacException;
import com.atlassian.marketplace.client.api.*;
import com.atlassian.marketplace.client.http.HttpConfiguration;
import com.atlassian.marketplace.client.http.SimpleHttpResponse;
import com.atlassian.marketplace.client.impl.DefaultMarketplaceClient;
import com.atlassian.marketplace.client.model.License;
import com.atlassian.marketplace.client.model.VendorSummary;
import com.atlassian.marketplace.client.util.UriBuilder;
import com.ecwid.maleorang.MailchimpClient;
import com.ecwid.maleorang.MailchimpException;
import com.ecwid.maleorang.MailchimpObject;
import com.ecwid.maleorang.method.v3_0.lists.members.EditMemberMethod;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import java.io.IOException;
import java.net.URI;

import static com.atlassian.fugue.Option.some;
import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.lang.System.setOut;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Slf4j
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

    Supplier<MailchimpClient> mailchimpClient = Suppliers.memoize(() -> new MailchimpClient(getenv("MAILCHIMP_API_KEY")));

    public ExampleResponse handler(ExampleRequest event, Context context) throws MpacException, IOException {
        HttpConfiguration.Credentials credentials = new HttpConfiguration.Credentials(
                getenv("MARKETPLACE_USER"), getenv("MARKETPLACE_PASSWORD"));

        HttpConfiguration config = HttpConfiguration.builder()
                .credentials(some(credentials))
                .build();

        try (MarketplaceClient client = MarketplaceClientFactory.createMarketplaceClient(config)) {
            VendorQuery vendorQuery = VendorQuery.builder().forThisUserOnly(true).build();

            Page<VendorSummary> vendors = client.vendors().find(vendorQuery);
            if (vendors.size() != vendors.totalSize()) {
                throw new RuntimeException("Didn't get whole collection at once which is not supported!");
            }

            for (VendorSummary vendor : vendors) {
                getEvaluationLicenses(client, vendor);
            }

            return new ExampleResponse("" + vendors.totalSize());
        }
    }

    private void getEvaluationLicenses(MarketplaceClient client, VendorSummary vendor) throws MpacException {
        LicenseQuery licenseQuery = LicenseQuery.builder()
                .licenseType(of("evaluation"))
                .sortBy(some("startDate"))
                .bounds(QueryBounds.limit(some(2)))
                .startDate(some(new DateTime().minusDays(1).toLocalDate()))
                .build();

        Page<License> licensePage = client.licenses(vendor).find(licenseQuery);
        for (License license : licensePage) {
            registerNewEvaluation(license);
        }
        if (licensePage.getNext().isDefined()) {
            for (PageReference<License> next : licensePage.getNext()) {
                Page<License> nextPage = client.getMore(next);
                for (License license : nextPage) {
                    registerNewEvaluation(license);
                }
            }
        }
    }

    private void registerNewEvaluation(License license) {
        License.ContactDetails contactDetails = license.getContactDetails();

        Option<License.Contact> contact = contactDetails.getTechnicalContact().orElse(contactDetails.getBillingContact());

        for (License.Contact c : contact) {
            for (String email : c.getEmail()) {
                EditMemberMethod.CreateOrUpdate createOrUpdate = new EditMemberMethod.CreateOrUpdate(getenv("MAILCHIMP_LIST_ID"), email);
                createOrUpdate.status = "subscribed";

                createOrUpdate.merge_fields = new MailchimpObject();
                createOrUpdate.merge_fields.mapping.put("COMPANY", contactDetails.getCompany());
                for (String name : c.getName()) {
                    createOrUpdate.merge_fields.mapping.put("NAME", name);
                }

                String mailChimpInterest = getenv("MAILCHIMP_INTEREST_" + license.getAddonKey());
                if (isNotBlank(mailChimpInterest)) {
                    createOrUpdate.interests = new MailchimpObject();
                    createOrUpdate.interests.mapping.put(mailChimpInterest, true);
                }

                try {
                    mailchimpClient.get().execute(createOrUpdate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.print(new Example().handler(null, null));
    }
}