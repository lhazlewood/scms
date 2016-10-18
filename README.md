[![Build Status](https://travis-ci.org/lhazlewood/scms.png)](https://travis-ci.org/lhazlewood/scms)
# SCMS

SCMS (Simple Content Management System) is very simple file-based CMS - no database required!  
SCMS exists to support static websites or dynamic sites that achieve their dynamic nature through JavaScript.

SCMS is extraordinarily simple: given a source directory tree of Markdown and html content files, it applies one
or more page templates to the content, resulting in a new 1-to-1 directory tree with fully rendered 
HTML files.  It is expected that you will manage your content (markdown files, images, etc) using a version control 
system like Git or Subversion.

When it comes time to preview your changes as complete HTML or publish your site to web servers, you run a simple SCMS 
command line script to render your web site.  You can take the resulting directory tree and either commit it to version \
control or just push it directly to your web servers.  Super easy.

Resulting output is fully customizable via [Velocity templates](http://velocity.apache.org/engine/devel/user-guide.html)

## Quickstart

### Prerequisites

SCMS is a Java-based command line program.  You do **not** need to know anything about Java and the generated
site does not require Java.  Java is _only_ needed to execute the scms command line script.

Check to see if you have Java installed by running the following command at a terminal prompt:

    $ java -version

and you should see something like this:

    java version "1.8.0_92"
    Java(TM) SE Runtime Environment (build 1.8.0_92-b14)
    Java HotSpot(TM) 64-Bit Server VM (build 25.92-b14, mixed mode)

If you don't see something similar, [install Java](http://java.com/en/download/index.jsp) first and ensure the `java` 
command is in your `$PATH`.  Ok, moving on...

### Download SCMS

You may [download the latest stable SCMS version](http://repo.maven.apache.org/maven2/com/leshazlewood/scms/scms/0.2.0/scms-0.2.0.zip) from Maven Central.

### Install SCMS

Unzip the SCMS distribution .zip file (e.g. scms-0.2.0.zip).  Add the resulting `scms-0.2.0/bin` directory
to your `PATH` environment variable.  For example, on Unix, Linux and Mac OS X*:

    $ unzip scms-0.2.0.zip
    $ export PATH=scms-0.2.0/bin:$PATH

It is recommended that you set this in `~/.bash_profile` so you don't have to do this manually every time you open 
a new terminal prompt.

*Windows users can set their PATH via [these instructions](http://www.computerhope.com/issues/ch000549.htm)

### Run SCMS

Once you add the SCMS `bin` directory to your path, you can run it by 
simply typing `scms` on the command line:

    $ scms

The above command will show a help menu with further usage instructions.

### Directory Structure

Create a quick directory structure like the following on your file system.  This will be our quick starter project:

    mysite/
        templates/

The `mysite` directory is the root of our quick website project.  The `templates` directory is a sub directory.

### Configuration

Create a `.scms.groovy` file in the root directory of your static website project with the following contents to get started:

    scms {

        excludes = ['templates/**']

        patterns {
            '**/*.md' {
                template = 'templates/default.vtl'
            }
        }
    }

Here's what the contents mean:

- The `scms` block is the top-level 'wrapper' containing all relevant SCMS configuration.
- The `excludes` property is a comma-delimited list of [Ant-style pattern](http://ant.apache.org/manual/dirtasks.html#patterns) strings. Any
  file discovered matching one of these patterns will **not** be copied by SCMS to the output directory.
  The above example shows what most people want: to exclude any rendering templates.
- The `patterns` block contains one or more Ant-style patterns, each with their own config block to be applied when
  SCMS encounters a file matching that pattern.

The above `**/*.md` Ant-style pattern example in the `patterns` block ensures that, whenever a Markdown file is
encountered in the `mysite` directory or any of its children directories, SCMS will:

1. Read the Markdown file's contents
2. Convert those contents from Markdown to HTML
3. Merge the resulting HTML with the `templates/default.vtl` [Velocity](http://velocity.apache.org/engine/devel/user-guide.html) template.
   We'll cover templates in just a second.

Now our project structure looks like this:

    mysite/
        templates/
        .scms.groovy

### HTML Template

Create a `default.vtl` template file in the `templates` subdirectory with the following contents:

    <html>
    <body>

    $content

    </body>
    </html>

This is a [Velocity](http://velocity.apache.org/engine/devel/user-guide.html) template file (the `.vtl` extension
indicates a Velocity Template Language file).  When SCMS runs, any encountered Markdown file will be
rendered to HTML and then that rendered HTML will replace the `$content` placeholder.

Now our project structure looks like this:

    mysite/
        templates/
            default.vtl
        .scms.groovy

### Our First Content File

Create an `index.md` Markdown file at the root of your sample project with the following contents:

    # Hello World

    This is my first SCMS-rendered site!

Now our project structure looks like this:

    mysite/
        templates/
            default.vtl
        .scms.groovy
        index.md

### Render your site

Now that we have our config, an HTML template and an initial bit of Markdown content, we can render our site.  Enter the 
project root directory:

    $ cd mysite

Now render your site.  We'll specify `output` as our destination directory, relative to the project root.  SCMS
will render all output to the `output` directory.  You can specify a different directory if you want the output to be
somewhere else.  Run this:

    $ scms output

After you've run this command, you'll see the following directory structure:

    mysite/
        output/
            index.html
        templates/
            default.vtl
        .scms.groovy
        index.md

See the new `output` directory with the `index.html` file?  Open it up and this is what you'll see:

    <html>
    <body>

    <h1>Hello World</h1>
    <p>This is my first SCMS-rendered site!</p>

    </body>
    </html>

See how the `index.md` file was converted to the `<h1>` and `<p>` content?  And then see how the
`$content` placeholder in `default.vtl` was replaced with the converted HTML?

This is what SCMS does - sweet and simple.

### How does this work?

Now that you've gotten your feet wet, here's what is going on:

SCMS will produce a 1:1 recursive copy of the site in your source directory (the `mysite` directory above) to your 
specified destination directory (the `output` directory above).  But during that process, it will render all Markdown 
files as HTML files using the specified Velocity template(s) in `.scms.groovy`.

As you can infer from `.scms.groovy`, you can have multiple templates: for any file matching a particular pattern,
you can apply a specific template for that pattern.  Patterns are matched based on a 'first match wins' policy, so more
specific patterns must be defined before more general patterns.  If a file in the source directory tree does not 
match a pattern in `.scms.groovy`, it is simply copied to the destination directory unchanged.  Any file that you
don't want copied to the destination directory should have a path that matches one of the String patterns in the 
`excludes` array.

All that is left now is to learn a little bit of the [Velocity Template Language](http://velocity.apache.org/engine/devel/user-guide.html#Velocity_Template_Language_VTL:_An_Introduction)
so you can write as many `.vtl` Velocity templates as you want to customize the rendered output (look and feel) of your
site.

## Build Instructions

This section is only necessary if you want to build SCMS yourself instead of downloading it directly.

SCMS requires [Java 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and [Maven 3](http://maven.apache.org/) to build:

    $ mvn install
    
This will create the SCMS distribution .zip file in the `dist/target` directory.  You can unzip the zip file and run 
the `bin/scms` script as explained at the top of this document.
