#!/bin/bash

# Common
rm *.jar
cp ~/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar .
cp ~/.m2/repository/org/slf4j/slf4j-simple/1.7.30/slf4j-simple-1.7.30.jar .

# Doc
cp ../pousse-cafe-doc/target/*.jar .
cp ~/.m2/repository/org/freemarker/freemarker/2.3.30/freemarker-2.3.30.jar .
cp ~/.m2/repository/org/xhtmlrenderer/flying-saucer-core/9.1.20/flying-saucer-core-9.1.20.jar .
cp ~/.m2/repository/org/xhtmlrenderer/flying-saucer-pdf/9.1.20/flying-saucer-pdf-9.1.20.jar .
cp ~/.m2/repository/com/lowagie/itext/2.1.7/itext-2.1.7.jar .
cp ~/.m2/repository/commons-io/commons-io/2.7/commons-io-2.7.jar .

# Source
cp ../pousse-cafe-source/target/*.jar .
cp ~/.m2/repository/org/antlr/antlr4-runtime/4.8-1/antlr4-runtime-4.8-1.jar .

# Core
cp ../pousse-cafe/pousse-cafe-core/target/*.jar .

# Attribute
cp ../pousse-cafe/pousse-cafe-attribute/target/*.jar .

# Base
cp ../pousse-cafe/pousse-cafe-base/target/*.jar .
