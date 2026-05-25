# FiddConnectorRestServer

Spring REST wrapper over `FiddConnector`.

For the first implementation the server creates one `FolderFiddConnector` from `fidd.folder-path`
and exposes it through the generated OpenAPI controller interfaces.

## Generate

```bash
gradle :FiddConnectorRestServer:openApiGenerate
```

`compileJava` depends on `openApiGenerate`, so a normal build also regenerates the API interfaces:

```bash
gradle :FiddConnectorRestServer:build
```

## Run

The service requires a Fidd folder path:

```bash
FIDD_FOLDER_PATH=/path/to/fidd gradle :FiddConnectorRestServer:bootRun
```
