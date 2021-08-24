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
- OLX_DB_PWD - Gmail account
- OLX_DB_URL - Gmail account


### Running application

1. Run seed.sql script against MySQL server
2. Make sure that environment variables mentioned above are set
3. `mvn spring-boot:run`
 

Schedule a job to invoke endpoint every 10 minutes (make sure that jar file is generated, if not run `mvn clean package`) on OS X

1. `env EDITOR=nano crontab -e`
2. `*/10 * * * * cd /full/path/olx-scrapper && ./run_olx_scrapper.sh`
3. `crontab -l` to verify that new cronjob is added
