## Project setup

1. `cd src`
1. `mvn clean appengine:devserver`

## Deploying

### How to deploy

1. Create a project in [Google Cloud Console](https://cloud.google.com/console)
1. edit ...**/webapp/WEB-INFappengine.web.xml** and change **your-app-id**
1. `mvn clean appengine:update`