# Pousse-Café Eclipse Plug-in Development

Development, testing and releasing requires an Eclipse workbench with
the [Plug-in Development Environment (PDE)](https://www.eclipse.org/pde/) installed.

The release process is manual (see below).

### Setup

The setup phase is a bit tedious but needs to be processed only once.

#### Dependencies

The following projects must be git-cloned into the workspace:

- [pousse-cafe](https://github.com/pousse-cafe/pousse-cafe)
- [pousse-cafe-source](https://github.com/pousse-cafe/pousse-cafe-source)
- [pousse-cafe-doc](https://github.com/pousse-cafe/pousse-cafe-doc)

They must have been built with `maven package`. Each time a dependency has been rebuilt, the JARs must be copied
in this project using script `copy_jar.sh`.

### Develop and test new features

New developments can be tested simply by running the plug-in directly in a test workspace (built-in feature of PDE).

Important notes:
- This may require first some work in the dependencies. In that case, do not forget to update the JARs in the dependency 
projects **AND** to change their version number.
- In case of change of major, minor or micro version part, the dependency version number has to be updated in the main 
plug-in manifest.

## Release

### Additional dependencies

When releasing, the following additional projects must be cloned into the workspace:

- [pousse-cafe-eclipse-feature](https://github.com/pousse-cafe/pousse-cafe-eclipse-feature)
- [pousse-cafe-eclipse-site](https://github.com/pousse-cafe/pousse-cafe-eclipse-site)

Synchronize your local `pousse-cafe-eclipse-site` project with the content of the
[remote site](http://download.pousse-cafe-framework.org/eclipse/updates/). The following files should be downloaded:

- `artifacts.jar`
- `content.jar`
- `features/*.jar`
- `plugins/*.jar`

### Additional dependencies

1. In `pousse-cafe-eclipse-feature` project, synchronize plug-ins versions and change version number
2. In `pousse-cafe-eclipse-site` project
    2.1 replace old feature with the latest
    2.2 re-build site ("Build All" command)
3. Copy local content to remote site.
