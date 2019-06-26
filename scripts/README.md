# NASA/ESA synchronization scripts

These groovy scripts are meant to be deployed in the `/opt/syncope/` server directory and referenced within the Syncope Resource Topology.

The topology model for ESA user account synchronization consists of two separate connectors: [ESA Push](rest/esa-push) and [ESA Pull](rest/esa-push). Each of the resources for these connectors reference the groovy files in their respective folders. 

