[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.microsoft.sqlserver.msi/msi-mssql-jdbc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.microsoft.sqlserver.msi/msi-mssql-jdbc)

Article describing usage step by step: https://dzone.com/articles/migrating-java-applications-to-azure-app-service-p

# DataSource factory for Tomcat leveraging Azure MSI (Managed Service Identity)

App Service provides Managed Service Identity to the applications, thus eliminating secrets from your app, such as credentials in the connection strings. For the details on the setup of MSI refer:
[Secure SQL Database connection with managed service identity](https://docs.microsoft.com/en-us/azure/app-service/app-service-web-tutorial-connect-msi)

Steps to enable MSI in Java application are described below

## Create Azure AppService and database

- Create AppService and Azure SQL database.

- Add the Connection String from Azure SQL database to **App Service / Application Settings**  App settings (**do NOT include username/password!**)
![Azure SQL Connection](https://github.com/lenisha/spring-jndi-appservice/raw/master/img/ConnectionString.PNG "Azure App Service Settings")

DB connection url for Azure SQL is usually in thins format `jdbc:sqlserver://server.database.windows.net:1433;database=db;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;`

Adding Setting `JAVA_OPTS` with value `-D<connection name>=<jdbc url>`  would create environment variable `connection name` available to the Java application.
 In our example above - `SQLDB_URL`


- Add MSI to AppService and grant it permissions to SQL database

```
az webapp identity assign --resource-group <resource_group> --name <app_service_name>
az ad sp show --id <id_created_above>

az sql server ad-admin create --resource-group <resource_group> --server-name <sql_server_name>  --display-name <admin_account_name> --object-id <id_created_above>
```
### Enable MSI flag 

There are currently 2 ways to enable MSI for datasource connection Factory

- Environment variable: `JDBC_MSI_ENABLE=true`, set it in ApplicationSettings for Azure WebApp

- jdbcURL flag: to set it add in jdbc connection string `msiEnable=true`. E.g `jdbc:sqlserver://server.database.windows.net:1433;database=db;msiEnable=true;...`


# Tomcat JDBC Pool - using JdbcInterceptor
There are two connection pools generally used by Tomcat server - DBCP and Tomcat JDBC. While DBCP is the default one, Tomcat JDBC pool provides 
a better way to intercept connections being established. This solution is relying to using Tomcat JDBC pool, for DBCP refer to section below.

## Add libraries to application
In application `pom.xml` include the following libs, to be deployed with the application
This library Jar as well as JDBC driver for SQL Server that supports Token based authentication

```
        <dependency>
            <groupId>com.microsoft.sqlserver.msi</groupId>
            <artifactId>msi-mssql-jdbc</artifactId>
	        <version>2.0.2</version>
        </dependency>

        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.8.0</version>
        </dependency>

        <dependency>
            <groupId>com.microsoft.sqlserver</groupId>
            <artifactId>mssql-jdbc</artifactId>
            <version>6.4.0.jre7</version>
        </dependency>
        
        </dependency><dependency>
            <groupId>com.nimbusds</groupId>
            <artifactId>nimbus-jose-jwt</artifactId>
            <version>3.1.2</version>
        </dependency>

        
```        
## Define the Datasource with MSI interceptor
 To define JNDI Datsource for Tomact Application, add file `META-INF/context.xml` to the application.
 In this example it's added to `main/webapp/META-INF/context.xml` anc contains the following datasource definition using Tomcat JDBC pool
 and MSI interceptor:
 
 ```
 <Context>
     <Resource auth="Container" 
         driverClassName="com.microsoft.sqlserver.jdbc.SQLServerDriver"
         maxActive="8" maxIdle="4" 
         name="jdbc/tutorialDS" type="javax.sql.DataSource"
         url="${SQLDB_URL}"
         factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
         jdbcInterceptors="com.microsoft.sqlserver.msi.MsiTokenInterceptor"/>

 </Context>
 ```
 
 - `url` points to url, in the example above provided by environment variable set by `JAVA_OPTS`


# Tomcat DBCP Pool - using Spring AOP
If application needs DBCP as its connection pool, solution to intercept connection is based on Spring AOP to inject the aspect that will refresh token.

## Add libraries to Application

In application `pom.xml` include the following libs, to be deployed with the application
This library Jar as well as JDBC driver for SQL Server that supports Token based authentication
and libraries required to activate aspects

```
     
        <dependency>
            <groupId>com.microsoft.sqlserver.msi</groupId>
            <artifactId>msi-mssql-jdbc</artifactId>
	        <version>2.0.2</version>
        </dependency>

        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.8.0</version>
        </dependency>

        <dependency>
            <groupId>com.microsoft.sqlserver</groupId>
            <artifactId>mssql-jdbc</artifactId>
            <version>6.4.0.jre7</version>
        </dependency>
        
         <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-aop</artifactId>
            <version>${org.springframework-version}</version>
        </dependency>

        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjrt</artifactId>
            <version>1.6.11</version>
        </dependency>

        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <version>1.6.11</version>
        </dependency>

        </dependency><dependency>
            <groupId>com.nimbusds</groupId>
            <artifactId>nimbus-jose-jwt</artifactId>
            <version>3.1.2</version>
        </dependency>

```        
## Enable MSI token refreshing Aspect
Add aspect bean to beans definitions - `application-context.xml` or `*-dispatcher-servlet.xml` 

```
<beans xmlns="http://www.springframework.org/schema/beans"
    ...
    http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-3.2.xsd
    ">

  <aop:aspectj-autoproxy />

   <bean id="msiAspect" class="com.microsoft.sqlserver.msi.MsiTokenAspect"/>
</beans>
```    
    

## Define DataSource in Tomcat


To define JNDI Datsource for Tomact Application, add file `META-INF/context.xml` to the application.
In this example it's added to `main/webapp/META-INF/context.xml` anc contains the following datasource definition

```
<Context>
    <Resource auth="Container" 
        driverClassName="com.microsoft.sqlserver.jdbc.SQLServerDriver"
        maxActive="8" maxIdle="4" 
        name="jdbc/tutorialDS" type="javax.sql.DataSource"
        url="${SQLDB_URL}"
        factory="org.apache.tomcat.dbcp.dbcp.BasicDataSourceFactory" />
</Context>
```

- `url` points to url, in the example above provided by environment variable set by `JAVA_OPTS`



# Build and publish application to Maven Central :
`mvn clean package`
and copy resulting jar file in the application directory

## To publish on Maven Central
Details on publishing process [OSS Maven Central process](https://central.sonatype.org/pages/requirements.html)

- Generate gpg keys and send the public key to server
```
gpg --gen-key
gpg --list-keys
gpg --keyserver hkp://pool.sks-keyservers.net --send-keys XXXX
```
- Update settings.xml with connections to jira project and gpg pass phrase

- Update pom.xml with all required information (as described in the link above) and plugins

- Run SnapShot release, update version and Release to staging
```
mvn clean deploy
mvn versions:set -DnewVersion=1.1.0
mvn clean deploy
```

Verify in central after 30 min
https://repo.maven.apache.org/maven2/com/microsoft/sqlserver/msi/msi-mssql-jdbc/


### References
[ADAL4J](https://github.com/AzureAD/azure-activedirectory-library-for-java)
