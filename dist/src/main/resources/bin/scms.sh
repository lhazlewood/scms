#!/bin/sh

# Copyright 2013 Les Hazlewood
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# -----------------------------------------------------------------------------
# Control Script for the SCMS Command Line Interface
#
# Environment Variable Prerequisites
#
#   Do not set the variables in this script. Instead put them into a script
#   setenv.sh in $SCMS_HOME/bin to keep your customizations separate.
#
#   SCMS_HOME   May point at your SCMS installation directory.
#
#   SCMS_JAVA_OPTS   (Optional) Java runtime options used when the executing SCMS,
#                    such as heap size, etc.
#
#   JAVA_HOME       Must point at your Java Development Kit installation.
#                   Required to run the with the "debug" argument.
#
#   JRE_HOME        Must point at your Java Runtime installation.
#                   Defaults to JAVA_HOME if empty. If JRE_HOME and JAVA_HOME
#                   are both set, JRE_HOME is used.
#
# -----------------------------------------------------------------------------

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set SCMS_HOME if not already set
[ -z "$SCMS_HOME" ] && SCMS_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

# Ensure that any user defined CLASSPATH variables are not used on startup,
# but allow them to be specified in setenv.sh, in rare case when it is needed.
CLASSPATH=

if [ -r "$SCMS_HOME/bin/setenv.sh" ]; then
  . "$SCMS_HOME/bin/setenv.sh"
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$JRE_HOME" ] && JRE_HOME=`cygpath --unix "$JRE_HOME"`
  [ -n "$SCMS_HOME" ] && SCMS_HOME=`cygpath --unix "$SCMS_HOME"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# Get standard Java environment variables
if [ -r "$SCMS_HOME"/bin/setclasspath.sh ]; then
  . "$SCMS_HOME"/bin/setclasspath.sh
else
  echo "Cannot find $SCMS_HOME/bin/setclasspath.sh"
  echo "This file is needed to run this program"
  exit 1
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  JRE_HOME=`cygpath --absolute --windows "$JRE_HOME"`
  SCMS_HOME=`cygpath --absolute --windows "$SCMS_HOME"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi


# ----- Execute The Requested Command -----------------------------------------

eval \"$_RUNJAVA\" $SCMS_JAVA_OPTS com.leshazlewood.scms.cli.Main "$@"
