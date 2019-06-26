# MAAP Syncope Server Configuration 

[Apache Syncope](https://syncope.apache.org) is an identity management application that facilitates MAAP user registration, account data storage, and profile synchronization between NASA and ESA. This project provides add-on functionality to the baseline Syncope framework, including:

- [NASA/ESA synchronization scripts](scripts)
- [Resource topology schema](MAAP-configuration.xml)
- Configuration and deployment instructions

## Deployment

MAAP uses the [Maven Project](https://syncope.apache.org/docs/getting-started.html#maven-project) method of deployment. The following build command is used for generating the necessary `war` files running Syncope:

```shell
mvn -P all clean install \
   -Dconf.directory=/opt/syncope/conf \
   -Dbundles.directory=/opt/syncope/bundles \
   -Dlog.directory=/opt/syncope/log
```

This Maven command establishes the `/opt/syncope` directory as the location for our configuration files. The `all` argument directs Syncope to load all the [built-in extensions](http://syncope.apache.org/docs/reference-guide.html#extensions) required for running MAAP.

### PostgreSQL

The MAAP Syncope deployment uses PostgreSQL for user data persistence. Instuctions for configuration: http://syncope.apache.org/docs/reference-guide.html#postgresql

### Gitlab Plugin

The [maap-auth-gitlab4syncope](https://github.com/MAAP-Project/maap-auth-gitlab4syncope) plugin is required for syncing Syncope with the MAAP Gitlab service. This customization must be installed as a [Connector Bundle](http://syncope.apache.org/docs/reference-guide.html#connector-bundles). Once installed, the bundle will be available as a Bundle option when adding a new connector in Syncope.


