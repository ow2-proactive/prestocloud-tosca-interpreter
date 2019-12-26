# TOSCA Parser

This repository contains the TOSCA parser. This component is in charge of:

- Parsing the type-level TOSCA file describing an application to be parsed

- Assessing computing resources availability

- Programming a Btrplace model

- Executing btrplace solving

- Interpreting a reconfiguration plan matching with TOSCA specification and resources availability

- Determining hourly operation cost of the new deployment

In the context of the PrEstoCloud architecture, this component implements both:

- *The ADIAM*: It interprets the input from the meta-management layout, and determine which management operation should be enacted.

- *The APSC*: It performs the solving of the fragment placement problem. This feature is supported by the BtrPlace library, provided by CNRS.

## Building the project

The project use maven to retrieve the build dependencies, perform the compilation and the package the outcome.

1. Perform the compilation with `$ mvn package -Dmaven.test.skip=true`

2. Retrieve the JAR executable from `target/` directory.

## Testing locally the project

The component can be tested offline of the whole ADIAM platform with the following command:

```
java -jar target/prestocloud-tosca-1.0.0-SNAPSHOT.jar <tosca_repository> <tosca_resource> <type_level_tosca_file> <reconfiguration_deployment_file> <reconfiguration_deployment_file> <mapping> <edge_status_file>
```

The argument are the following:

- *tosca_repository* : Refering the TOSCA definition file necessary to tosca file parsing. A valid directory containing valid definition has to be specified. `src/main/resources/repository/` is provided as an example in the repository.

- *tosca_resource*: Refering to the directory containing tosca file defining cloud and edge resource to be schedulled. A valid file containing cloud resource and edge resource definition has to be mandatorily specified. `src/test/resources/prestocloud/resources/` is provided as an example.

- *type_level_tosca_file*: Identifying the tosca file to be parsed and interpreted into an ADIAM's reconfiguration plan. This parameter must refer to a valid TOSCA file

- *reconfiguration_deployment_file*: Pointing to the ADIAM reconfiguration file to be created. This file is to be consumed later by the [ADIAM main workflow](https://gitlab.com/prestocloud-project/adiam-workflows).

- *instance_level_tosca_file*: Providing the instance level TOSCA file to be produced. The file has not to necessarily exist.

- *mapping*: Containing the scheme of an already existing deployment. Can refer to a non-existing file for initial deployment.

- *edge_status_file*: Mandatorily referring to a file containing an output of the [edge-gateway](https://gitlab.com/prestocloud-project/edge-gateway/tree/master) API call to topology endpoint. An example file should have the following content:
```
{"rescode":"SUCCESS","message":null,"resobject":{"peers":[],"nodes":[]}}

```

Command usage example:
```
java -jar target/prestocloud-tosca-1.0.0-SNAPSHOT.jar  src/main/resources/repository/ src/test/resources/prestocloud/resources/ simple_tosca_deployment_php_mariadb.yaml output.json mapping.json status.sample.json
```

## Installing in the ADIAM platform

This parser is designed to be installed inside the ADIAM platform, and called from the [main workflow](https://gitlab.com/prestocloud-project/adiam-workflows).

To proceed so, the package has to be uploaded on the server operating the ProActive instance loaded with the ADIAM workflow, to the following path:
```
/home/ubuntu/tosca-parser/prestocloud_tosca_parser.jar
```
