# DataSource factory for Tomcat leveraging Azure MSI (Managed Service Identity)

App Service provides Managed Service Identity to the applications, thus eliminating secrets from your app, such as credentials in the connection strings. For the details on the setup of MSI refer:
[Secure SQL Database connection with managed service identity](https://docs.microsoft.com/en-us/azure/app-service/app-service-web-tutorial-connect-msi)

Steps to enable MSI in Java application are described below

## Create Azure AppService and database

- Create AppService and Azure SQL database.

- Add the Connection String from Azure SQL database to **App Service / Application Settings**  ConnectionStrings settings (do NOT include username/password!)
![Azure SQL Connection](https://github.com/lenisha/tutorial-hibernate-jpa/raw/master/img/ConnectionString.PNG "Azure App Service Settings")

DB connection url for Azure SQL is usually in thins format `jdbc:sqlserver://jnditestsrv.database.windows.net:1433;database=jnditestsql;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;`

Placing connection string which contains in credential  information in AppSettings/Connection Strings section allows us to hide it from unauthorized users view. 

Adding ConnectionString in App Settings would create environment variable `SQLAZURECONNSTR_<Name of the Connection>` available to the Java application.   In our example above - `SQLAZURECONNSTR_UsersDB`

- Add MSI to AppService and grant it permissions to SQL database

```
az webapp identity assign --resource-group <resource_group> --name <app_service_name>
az ad sp show --id <id_created_above>

az sql server ad-admin create --resource-group <resource_group> --server-name <sql_server_name>  --display-name <admin_account_name> --object-id <id_created_above>
```

## Add libraries to Application

In application `pom.xml` include the following libs, to be deployed with the application
This library Jar as well as JDBC driver for SQL Server that supports Token based authentication

```
     
        <dependency>
            <groupId>com.microsoft.sqlserver.msi</groupId>
            <artifactId>msi-mssql-jdbc</artifactId>
	        <version>1.0</version>
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
		url="${SQLAZURECONNSTR_UsersDB}"
		msiEnable="true" factory="com.microsoft.sqlserver.msi.MsiDataSourceFactory" />
    
</Context>
```

- `msiEnable` flag tells Factory to use MSI identity to establish connection to Datasource
- `factory` overrides default Tomcat `BasicDataSourceFactory`, and uses `MSI_ENDPOINT and MSI_SECRET` to obtain the token and use it for the connection.


	
## To build :
`mvn clean package`
and copy resulting jar file in the application directory


