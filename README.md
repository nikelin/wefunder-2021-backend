# Wefunder Assignment - 2021

## Overview
The project is a simple back-end application allowing extraction and rendering of presentation pages in the PDF and 
PPT/PPTX source formats.

### Prerequisites

* SBT: https://www.scala-sbt.org/download.html
* JDK: https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html
* PostgreSQL: https://www.postgresql.org/

### Tech Stack

* Language: Scala
* Database: PostgreSQL

### Build

The command below will generate a distributable artifact:

```
sbt  universal:packageBin
```

The resulting artifact will be located at:
```
$root/target/wefunder-assignment-backend-0.1.zip
```

## Run

There are two options to test application: to start it from the binary distributive or to run directly from the source code repository (**SBT**).

1. **Option 1** - Start from the binary package (see **Build**)

    ```
    $pathToServerDist/bin/wefunder-assignment-backend
    ```

2. **Option 2** - Run from the source code
    ```
    sbt run
    ```

Before attempting to start the application, make sure that its configuration is consistent with your environment settings. Specifically, check the database config and 
each the path to each renderer's storage to make sure that they are able to store the processing results on the disk:
* Default path to images storage
  * PDFs: **/opt/storage/rendered-pages-pdf**
  * PPTs: **/opt/storage/rendered-pages-ppt**
* Database (schema setup is automatic)
  * Table name: `wefunder_pithdeck` 
  * User: `postgres`
  * Password: **empty**
  * Connection: `localhost:5432`

The application can be configured either by changing its configuration file available at `src/main/resources/application.conf`,
or at `./conf/application.conf` in case of a binary distributive.

It is possible to override the application configuration via environment variables, i.e. `APP_HOST=8081 ./bin/wefunder-assignment-backend`. 

Down below is the list of all environment variables supported by the application grouped by the target component: 
* HTTP
  * **APP_HOST** (`string`, default `localhost`) - a hostname used by the application to start the HTTP server
  * **APP_PORT** (`integer`, i.e. `8080`) - a port number used by the application to start the HTTP server
* Renderers
  * **RENDERERS_PARALLELISM_LEVEL** (`integer`, default `5`) - a max number of page rendering tasks (generic) running in parallel 
  * **PDF_RENDERERS_PARALLELISM_LEVEL** (`integer`, default `10`) - a max number of page rendering tasks running in parallel specific to the PDF renderer  
  * **PPT_RENDERERS_PARALLELISM_LEVEL** (`integer`, default `5`) - a max number of page rendering tasks running in parallel specific to the PPT renderer
  * **PDF_RENDERER_STORAGE_PATH** (`string`, default `/opt/storage/rendered-pages-pdf`) - an output folders for the results produced by the PDF renderer
  * **PDF_RENDERER_STORAGE_PATH** (`string`, default `/opt/storage/rendered-pages-ppt`) - an output folders for the results produced by the PPT renderer
  * **PDF_RENDERER_DPI_LEVEL** (`integer`, default `96`) - a DPI resolution setting used by the PDF documents service to render a separate pages into images 
* DB
  * **ENABLE_SCHEMA_SETUP** (`boolean`, default `true`) - if enabled the application will try to set up the DB schema on its startup
  * **PGNAME** (`string`, default `wefunder_pichdeck`) - a DB username
  * **PGHOST** (`string`, default `localhost`) - a DB username
  * **PGPORT** (`integer`, default `5432`) - a DB username
  * **PGUSER** (`string`, default `postgres`) - a DB username
  * **PGPASSWORD** (`string`, default is empty) - a DB password
  * **MAX_DATABASE_CONNECTIONS_POOL_SIZE** (`integer`, default `25`) - a max number of threads in the thread pool allocated for DB connections
  * **MIN_DATABASE_CONNECTIONS_POOL_SIZE** (`integer`, default `1`) - a min number of threads in the thread pool allocated for DB connections    
