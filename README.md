Atlassian Marketplace - MailChimp integration
-----
An example of Lambda function that adds new evaluators into MailChimps Automation.

To set it up, edit `env.json` and fill in details:

```json
{
    "MAILCHIMP_API_KEY": "API",
    "MARKETPLACE_USER": "username",
    "MARKETPLACE_PASSWORD": "password",
    "MAILCHIMP_LIST_ID": "6ca8f9b04b",
    "MAILCHIMP_INTEREST_com_pawelniewiadomski_jira_confluence_openid_authentication_plugin": "f8f18bdac5",
    "MAILCHIMP_INTEREST_com_pawelniewiadomski_jira_jira_openid_authentication_plugin": "f8f18bdac5",
    "MAILCHIMP_INTEREST_easy_social_sign_ups_servicedesk": "57eadd3fcb"
}
```

To deploy:

`apex deploy -E env.json marketplace_to_mailchimp`

To invoke it:

`apex invoke marketplace_to_mailchimp`

To see logs:

`apex logs`