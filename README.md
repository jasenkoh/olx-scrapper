# OLX article scrapper


Spring boot application that uses selenium web driver to navigate to olx.ba page with specified search query params and scans if there are new articles since last execution.
Send email if there is a new article found on olx based on the criteria.

### Requirements
- Java 11
- Maven
- MySQL
- Chrome


### Add two environment variables
- EMAIL_PASS - password of a google account
- OLX_EMAIL - Gmail account
- OLX_DB_PWD - Database Password account
- OLX_DB_URL - DB URL with a name of a database, e.g. `url.rds.amazonaws.com:3306/olx-scrapper`


### Running application

1. Run seed.sql script against MySQL server
2. Make sure that environment variables mentioned above are set
3. `mvn spring-boot:run`

Schedule a job to invoke endpoint every 10 minutes (make sure that jar file is generated, if not run `mvn clean package`) on OS X

1. `env EDITOR=nano crontab -e`
2. `*/10 * * * * curl http://localhost:8888/scrap-pages`
3. `crontab -l` to verify that new cronjob is added

### Adding new filter

```
POST: http://localhost:8888/filters
{
    "queryParams": "...",
    "searchPageName": "...",
    "active": true
}
```

### Deactivating existing filter

`POST: http://localhost:8888/filters/{filterId}/{status}` where `status` is set to `true` or `false`


### Potential improvements
1. Structure query building instead of persisting `queryParams` in DB in order to generate URL.
2. Extend E-Mail content and move to e.g. Sendgrid