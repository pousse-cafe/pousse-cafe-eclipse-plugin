#!/bin/bash

# Common
rm *.jar
cp ~/.m2/repository/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar .
cp ~/.m2/repository/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar .

# Doc
cp ../pousse-cafe-doc/target/*.jar .
cp ~/.m2/repository/org/freemarker/freemarker/2.3.33/freemarker-2.3.33.jar .
cp ~/.m2/repository/org/xhtmlrenderer/flying-saucer-core/9.9.0/flying-saucer-core-9.9.0.jar .
cp ~/.m2/repository/org/xhtmlrenderer/flying-saucer-pdf/9.9.0/flying-saucer-pdf-9.9.0.jar .
cp ~/.m2/repository/com/github/librepdf/openpdf/2.0.2/openpdf-2.0.2.jar .
cp ~/.m2/repository/commons-io/commons-io/2.16.1/commons-io-2.16.1.jar .

# Source
cp ../pousse-cafe-source/target/*.jar .
cp ~/.m2/repository/org/antlr/antlr4-runtime/4.13.1/antlr4-runtime-4.13.1.jar .

# Core
cp ../pousse-cafe/pousse-cafe-core/target/*.jar .

# Attribute
cp ../pousse-cafe/pousse-cafe-attribute/target/*.jar .

# Base
cp ../pousse-cafe/pousse-cafe-base/target/*.jar .
