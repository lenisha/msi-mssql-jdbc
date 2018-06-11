# Spring/Hibernate App on Azure AppService (Tomcat) using JNDI to access SQL Server on Azure  

## Spring Hibernate JNDI Example

This Spring example is based on tutorial [Hibernate, JPA & Spring MVC - Part 2] (https://www.alexecollins.com/tutorial-hibernate-jpa-spring-mvc-part-2/) by Alex Collins

Spring application uses hibernate to connect to SQL Server. Hibernate is using container managed JNDI Data Source.

There are few options to configure JNDI for the web application running undet Tomcat (Tomcat JNDI)[https://www.journaldev.com/2513/tomcat-datasource-jndi-example-java] :

- Application context.xml - located in app `META-INF/context.xml` - define Resource element in the context file and container will take care of loading and configuring it.
- Server server.xml - Global, shared by applications, defined in `tomcat/conf/server.xml`
- server.xml and context.xml - defining Datasource globally and including ResourceLink in application context.xml

When deploying to Java application on Azure AppService, you can customize  out of the box managed Tomcat server.xml, but is not recommended as it will create snowflake deployement. That's why we will define JNDI Datasource on the **Application level**

# Create Azure AppService and database

In Azure Portal create `Web App + SQL` and configure settings for the App to use Tomcat
![Azure App Service config](https://github.com/lenisha/tutorial-hibernate-jpa/raw/master/img/AppService.PNG  "App Service Config")

Copy the Connection String from Azure SQL database 
![Azure SQL Connection](https://github.com/lenisha/tutorial-hibernate-jpa/raw/master/img/ConnString.PNG "Azure  SQL Server")

Add the Connection String from Azure SQL database to **App Service / Application Settings**  ConnectionStrings settings
![Azure SQL Connection](https://github.com/lenisha/tutorial-hibernate-jpa/raw/master/img/ConnectionString.PNG "Azure App Service Settings")

DB connection url for Azure SQL is usually in thins format `jdbc:sqlserver://jnditestsrv.database.windows.net:1433;database=jnditestsql;user=XXX@jnditestsrv;password=XXXX;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;`

Placing connection string which contains in credential  information in AppSettings/Connection Strings section allows us to hide it from unauthorized users view. 

Adding ConnectionString in App Settings would create environment variable `SQLAZURECONNSTR_<Name of the Connection>` available to the Java application.   In our example above - `SQLAZURECONNSTR_UsersDB`


# Define DataSource
JDBC driver for SQL server `sqljdbc4jar` is installed in Tomcat in Azure App Service by default so no need to include it, but it could be included in `pom.xml` file if you need to test it locally

To define JNDI Datsource for Tomact Application, add file `META-INF/context.xml` to the application.
In this example it's added to `main/webapp/META-INF/context.xml` anc contains the following datasource definition

```
<Context>
    <Resource auth="Container" 
	    driverClassName="com.microsoft.sqlserver.jdbc.SQLServerDriver" 
	    maxActive="8" maxIdle="4" 
	    name="jdbc/tutorialDS" type="javax.sql.DataSource" 
	    url="${SQLAZURE_UsersDB}" />
   
</Context>
```

Notice that the URL for the database uses environment variable that should be available and processed by Tomcat startup.
Unfortunately directly reading App Settings Connection string environment varibale does not work, due to the way Tomcat is started by Azure App Service lifecycle.
So we will use **indirection** or what was called in C++ world a pointer to a variable.

The way to enforce Tomcat to read environment variables and make them available to application is to define them in `JAVA_OPTS` paramaters during Tomcat startup.

## Define SQL Connection env varible for JAVA_OPTS
As discussed in [How to set env in Java app in Azure App Service](https://blogs.msdn.microsoft.com/azureossds/2015/10/09/setting-environment-variable-and-accessing-it-in-java-program-on-azure-webapp/)
To set environment variable that uses another variable definition (not direct value) is to override it in `web.config`

And here are our configuration where we are passing in `JAVA_OPTS` to tomcat  `SQLAZURE_UsersDB`variable in that is used in our DataSource definition above. The value of it relies on the fact that we have set in Application Settings Database Connection string called `UsersDB`, as discussed previously.

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <system.webServer>
    <handlers>
      <remove name="httpPlatformHandlerMain" />
      <add name="httpPlatformHandlerMain" path="*" verb="*" modules="httpPlatformHandler" resourceType="Unspecified"/>
    </handlers>
    <httpPlatform processPath="%AZURE_TOMCAT7_HOME%\bin\startup.bat">
        <environmentVariables>
            <environmentVariable name="JAVA_OPTS" value="-DSQLAZURE_UsersDB=%SQLAZURECONNSTR_UsersDB%"/> 
        </environmentVariables>
      </httpPlatform>
  </system.webServer>
</configuration>
```

This file `web.config` should be copied to `D:\home\site\wwwroot` on the AppService to override Tomcat loading.

## Use SQL Server Hibernate Dialect 

in `resources\META-INF\persistence.xml`

```
<property name="hibernate.dialect" value="org.hibernate.dialect.SQLServerDialect" />
```
	
## To test locally:
`mvn clean package`
and copy resulting war file from target directory to Tomcat webapps directory

Navigate to `localhost:8080/tutorial-hibernate-jpa/create-user.html`

## Deploy on Azure AppService

To test on Azure AppService deploy using Azure deployment options, the simplest is to use FTP:

### FTP
  - Get the FTP hostname and credentials from the App Service blade
  - Upload war file to `d:\home\site\wwwroot\webapps`  
  - Upload web.config file to `d:\home\site\wwwroot`
  - Restart App Service
  - Navigate to `https://appname.azurewebsites.net/tutorial-hibernate-jpa/create-user.html`

### Maven plugin
- setup authentication in .m2/settings.xml as described in https://docs.microsoft.com/en-us/java/azure/spring-framework/deploy-spring-boot-java-app-with-maven-plugin

- Add plugin definition in pom.xml
```
      <plugin>
            <groupId>com.microsoft.azure</groupId>
            <artifactId>azure-webapp-maven-plugin</artifactId>
            <version>0.2.0</version>
            <configuration>
                <authentication>
                    <serverId>azure-auth</serverId>
                </authentication>
                 <!-- Web App information -->
               <resourceGroup>testjavajndi</resourceGroup>
               <appName>testjavajndi</appName>
               <!-- <region> and <pricingTier> are optional. They will be used to create new Web App if the specified Web App doesn't exist -->
               <region>canadacentral</region>
               <pricingTier>S1</pricingTier>
               
               <!-- Java Runtime Stack for Web App on Windows-->
               <javaVersion>1.7.0_51</javaVersion>
               <javaWebContainer>tomcat 7.0.50</javaWebContainer>
               
               <!-- FTP deployment -->
               <deploymentType>ftp</deploymentType>
               <!-- Resources to be deployed to your Web App -->
               <resources>
                  <resource>
                     <!-- Where your artifacts are stored -->
                     <directory>${project.basedir}/target</directory>
                     <!-- Relative path to /site/wwwroot/ -->
                     <targetPath>webapps</targetPath>
                     <includes>
                        <include>*.war</include>
                     </includes>
                  </resource>
                   <resource>
                     <!-- Where your artifacts are stored -->
                     <directory>${project.basedir}</directory>
                     <!-- Relative path to /site/wwwroot/ -->
                     <targetPath>.</targetPath>
                     <includes>
                        <include>web.config</include>
                     </includes>
                  </resource>
               </resources>
            </configuration>
        </plugin>
	
```
- run `mvn azure-webapp:deploy`

Example output:
```
[INFO] --- azure-webapp-maven-plugin:0.2.0:deploy (default-cli) @ tutorial-hibernate-jpa ---
AI: INFO 25-03-2018 19:31, 1: Configuration file has been successfully found as resource
AI: INFO 25-03-2018 19:31, 1: Configuration file has been successfully found as resource
[INFO] Start deploying to Web App testjavajndi...
[INFO] Authenticate with ServerId: azure-auth
[INFO] [Correlation ID: 462a8a7f-c2ec-40c8-a43b-80be705225b8] Instance discovery was successful
[INFO] Updating target Web App...
[INFO] Successfully updated Web App.
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 1 resource to C:\projects\tutorial-hibernate-jpa\target\azure-webapps\testjavajndi\webapps
[INFO] Starting uploading files to FTP server: waws-prod-yt1-005.ftp.azurewebsites.windows.net
[INFO] Starting uploading directory: C:\projects\tutorial-hibernate-jpa\target\azure-webapps\testjavajndi --> /site/wwwroot
[INFO] [DIR] C:\projects\tutorial-hibernate-jpa\target\azure-webapps\testjavajndi --> /site/wwwroot
[INFO] ..[DIR] C:\projects\tutorial-hibernate-jpa\target\azure-webapps\testjavajndi\webapps --> /site/wwwroot/webapps
[INFO] ....[FILE] C:\projects\tutorial-hibernate-jpa\target\azure-webapps\testjavajndi\webapps\tutorial-hibernate-jpa.war --> /site/wwwroot/webapps
[INFO] ...........Reply Message : 226 Transfer complete.

[INFO] Finished uploading directory: C:\projects\tutorial-hibernate-jpa\target\azure-webapps\testjavajndi --> /site/wwwroot
[INFO] Successfully uploaded files to FTP server: waws-prod-yt1-005.ftp.azurewebsites.windows.net
[INFO] Successfully deployed Web App at https://testjavajndi.azurewebsites.net
``` 

# Managed Service Identity
[ADAL4J](https://github.com/AzureAD/azure-activedirectory-library-for-java)

```
az webapp identity assign --resource-group jnditest --name testjndi
az ad sp show --id 82cc5f96-226a-4721-902c-7451cc68fd80

az sql server ad-admin create --resource-group jnditest --server-name jnditestsrv  --display-name admin-msi --object-id 82cc5f96-226a-4721-902c-7451cc68fd80
```
