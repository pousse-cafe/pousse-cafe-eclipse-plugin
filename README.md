# Pousse-Café Eclipse Plug-in

This Eclipse plug-in provides:

- source code validation,
- an EMIL editor,
- a Process List view.

Pousse-Café nature must be added to a project in order to enable the plug-in features.

The EMIL editor is associate by default with files having `emil` extension.

The process list view can be opened from the main menu:
- go to "Window > Show View > Other...",
- the go to "Pousse-Café" section and click on "Process List".

## Installation

1. In the menu, click on "Help > Install New Software..."
2. Click on "Add..." button
3. Choose a name and set the URL to `https://download.pousse-cafe-framework.org/eclipse/updates/`
4. Select the new update site then tick "Pousse-Café Eclipse Feature"
5. Click on "Next" and follow instructions

## Add Pousse-Café nature to a project

1. Right-click on the project in the package explorer view
2. Click on "Configure > Add Pousse-Café nature"

## Development

Development, testing and releasing requires an Eclipse workbench with
the [Plug-in Development Environment (PDE)](https://www.eclipse.org/pde/) installed.

The release process is manual (see below).

In the following, the Pousse-Café Eclipse Plug-in is referred to as the "main" plug-in (by opposition to its
dependencies).

### Setup

The setup phase is a bit tedious but needs to be processed only once.

#### Dependencies

The following projects must be cloned into the workspace:

- [pousse-cafe-antlr4-runtime-eclipse-plugin](https://github.com/pousse-cafe/pousse-cafe-antlr4-runtime-eclipse-plugin)
- [pousse-cafe-base-eclipse-plugin](https://github.com/pousse-cafe/pousse-cafe-base-eclipse-plugin)
- [pousse-cafe-core-eclipse-plugin](https://github.com/pousse-cafe/pousse-cafe-core-eclipse-plugin)
- [pousse-cafe-source-eclipse-plugin](https://github.com/pousse-cafe/pousse-cafe-source-eclipse-plugin)

Each project must contain the "original" JAR file:

- JAR files are "gitignored", they have to be copied manually in each dependency project.
- The version and name of the JAR file are mentioned in `MANIFEST.MF` and `build.properties` files.
- It is recommended to use the PDE view of the manifest when changing the version of the JAR file.
- The version of above dependencies follow the major, micro and minor of the original project.
- The version qualifier is the date-time of latest change (i.e. yyyyMMddhhmm).

#### Feature and site

The following projects are needed for releasing but not for development and testing.

When releasing, the following projects must be cloned into the workspace:

- [pousse-cafe-eclipse-feature](https://github.com/pousse-cafe/pousse-cafe-eclipse-feature)
- [pousse-cafe-eclipse-site](https://github.com/pousse-cafe/pousse-cafe-eclipse-site)

Synchronize your local project with the content of the
[remote site](http://download.pousse-cafe-framework.org/eclipse/updates/). The following files should be downloaded:

- `artifacts.jar`
- `content.jar`
- `features/*.jar`
- `plugins/*.jar`

### Develop and test new features

New developments can be tested simply by running the plug-in directly in a test workspace (built-in feature of PDE).

Important notes:
- This may require first some work in the dependencies. In that case, do not forget to update the JARs in the dependency 
projects **AND** to change their version number.
- In case of change of major, minor or micro version part, the dependency version number has to be updated in the main 
plug-in manifest.

### Release a new version of the plug-in

1. In `pousse-cafe-eclipse-site` project, re-build site ("Build All" command)
2. Check that expected plug-in JARs have been copied to the `plugins` folder
3. Copy local content to remote site.
