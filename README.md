# Backend expertise test from Outfit7

If you would like to test it on your own, add `config.properties` file inside
`/src/main/webapp/` with the following lines:

    adminName=YourAdminName
    password=YourAdminPassword

When the software is started, it automatically creates first admin user
with name = adminName and password = password, as well as first campaign
and both platforms (android, iphone).


--------------------------------------------------------------------------------

# Deployment:


To prevent MITM (man in the middle attacks), https should be used (at least for
admin api, since we are sending admin passwords across the network).
Google app engine already provides ssl certificate by default, so one less
thing to worry about.


1. Install 'google-cloud-sdk'. See google app engine documentation.

2. 'which gcloud' should return path, otherwise you have to add it manually to
bash profile. See: https://stackoverflow.com/a/31084490

3. Create new project on GAE (remember app-id)

4. Edit src/main/webapp/WEB-INF/appengine-web.xml and change
<application>{your app-id}</application>

5. Make sure to add 'config.properties' file inside
/src/main/webapp/ with the following lines:

adminName=YourAdminName
password=YourAdminPassword

When the software is started, it automatically creates first admin user
with name = adminName and password = password, as well as first campaign
and both platforms (android, iphone).


6. Official documentation states 'mvn appengine:deploy' should be used to deploy
the application, but that did not work in my case.

According to the Github issue:
https://github.com/GoogleCloudPlatform/app-maven-plugin/issues/89

I should provide full command path (this was the command I actually used for deploying):
'mvn clean com.google.cloud.tools:appengine-maven-plugin:deploy'

7. If no error occurs it's deployed. Use 'gcloud app:browse' to see your
app in action


I am using Linux, so those are the steps I took in order to deploy app on GAE.
On different platform it might be different.


    # run tests
    $ mvn clean test

    # run app locally
    $ mvn clean package
    $ mvn appengine:devserver




# Requirements:

Click Tracker

Assume that you are working on a backend application responsible for tracking all clicks that were made from our mobile clients. For that purpose we don’t use direct links but instead provide our own tracking links. These are urls that point to our server and contain information about the the click itself. When user clicks a link, he is redirected through our servers and then sent to the intended web address. Information about each click is also stored for analytical purposes.

Two main purposes of this application is to correctly redirect users to the target url and to provide basic analytics about clicks that go through it.

Every click is always in the context of a given campaign. Campaign is the main entity in the system and is managed by administrators, who can create new and delete old campaigns. Each campaign has id, name, redirect url and platforms that it is available on (assume that there are only two platforms: Android and IPhone). Every tracking link should always contain id of the campaign.

Our analytics team is interested to know how many clicks have happened for given campaign.

Application should expose two REST APIs, one for click tracking and one for admin users.

Tracker API:

    1. Main API through which mobile clients interact with the backend application.
    2. Handle incoming clicks that contain information about campaign id.
    3. If click is valid then user should be redirected to url defined on the campaign and statistic about this click should be saved.
    4. All invalid clicks (e.g. for non existing campaigns) should be redirected to http://outfit7.com.

Admin API:

    1. API is used by admin users to manage campaigns and check current statistics.
    2. Should enable users:
        a. Creation of new campaign.
        b. Update of the existing campaign.
        c. Deletion of the existing campaign.
        d. Displaying information about the existing campaign.
        e. Listing all existing campaigns available on given platform.
        f. Retrieving number of clicks for given campaign.
    3. Validate all incoming data and inform API user appropriately.

Your assignment is to design and implement application with the requirements above.

Technical requirements:

    1. Your solution should leverage Google App Engine java environment and you can (but don’t have to) use all the components available there (e.g. DataStore, Memcache, Cloud Endpoints etc). For full set of features see this link.
    2. Feel free to use any external library of your choice (e.g. REST framework or persistence library).
    3. Please send us either a zip archive or a link to project on the github/bitbucket (preferred).
    4. Make sure that your solution is production ready so apply all the techniques that you would normally do when writing code in a real life situation.
    5. Please provide Readme.txt file with the description on how to build your application and deploy it to Google App Engine.
    6. If something is not clear feel free to make some assumptions. In that case make sure to put it in Readme.txt. It should contain all the assumptions you made and all the extra informations or explanations that you think might be useful for anybody reading your solution.
