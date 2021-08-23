# OLX article scrapper


Simple selenium project that navigates to olx.ba page with specified search query params and scans if there are new articles since last execution.
Send email if there is a new article found on olx based on the criteria.

### Requirements
- Java 11
- Maven
- Firefox

By default, selenium starts with headless firefox browser, in order to run it with chrome, new environment variable should be added `SELENIUM_BROWSER=chrome`

### Add two environment variables
- EMAIL_PASS - password of a google account
- OLX_EMAIL - Gmail account

### Run with `mvn install`
 

Locally, schedule a job to run every 10 minutes (make sure that jar file is generated, if not run `mvn clean package`) on OS X

1. `env EDITOR=nano crontab -e`
2. `*/10 * * * * cd /full/path/olx-scrapper && ./run_olx_scrapper.sh  >> /full/path/olx-scrapper/script_output.log 2>&1`
3. `crontab -l` to verify that new cronjob is added

### Possible upgrades

1. Use cloud storage
2. Deploy



