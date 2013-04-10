# SCMS

SCMS (Simple Content Management System) is an extremely simple tool that exists to support static (or mostly static)
websites (or dynamic sites that achieve their dynamic nature mostly through JavaScript).

SCMS is extraordinarily simple: given a source directory tree full of Markdown files and html fragments, it
generates a new 1-to-1 directory tree with fully rendered HTML files.  It is expected that you will manage your content
(markdown files, images, etc) using a version control system (for example, git).

When it comes time to preview your changes in HTML or publish your site to web servers,  you run a simple SCMS command
line script to render your web site.  You can take the resulting directory tree and either commit to to version control
or just push it directly to your web servers.  Super easy.

Rendered output is fully customizable via [Velocity templates](http://velocity.apache.org/engine/devel/user-guide.html)

### Build

    > mvn install
    > cd cli/target
    > java -jar scms-cli-<version>-cli.jar

The last command will show a help menu.

## Quickstart

SCMS requires a Java runtime. Ensure you have Java installed by running the following command:

    $ java -version

and you should see something like this:

    java version "1.6.0_43"
    Java(TM) SE Runtime Environment (build 1.6.0_43-b01-447-11M4203)
    Java HotSpot(TM) 64-Bit Server VM (build 20.14-b01-447, mixed mode)

If you don't see this, install Java first.  Ok, moving on...

### Directory Structure

Create a quick directory structure like the following on your file system.  This will be our quick starter project:

    mysite/
        templates/

The `mysite` directory is the root of our quick website project.  The `templates` directory is a sub directory.


### Configuration

Create a `scms.cfg` file in the root directory of your static website project with the following contents to get started:

    patterns {

        '**/*.md' {
            template = 'templates/default.vtl'
        }

    }

This means:

Whenever a Markdown file is encountered in the source directory or any of its children directories,
(the `**/*.md` Ant-style pattern means any `.md` file in the current directory or any children directories), take
that file's contents, render it to html, and merge the rendered HTML with the `templates/default.vtl` Velocity template.
(template file paths are relative to the `.cfg` file.).  We'll cover templates in just a second.

Now our project structure looks like this:

    mysite/
        templates/
        scms.cfg

### Velocity template

Create a `default.vtl` template file in the `templates` subdirectory with the following contents:

    <html>
    <body>

    $content

    </body>
    </html>

This is a Velocity file template (the `.vtl` extension indicates Velocity Template Language).  When scms runs, any encountered Markdown file will be rendered to HTML and then that
rendered HTML will be inserted into the `$content` placeholder.

Now our project structure looks like this:

    mysite/
        templates/
            default.vtl
        scms.cfg

### Our First Content File

Create an `index.md` Markdown file at the root of your sample project with the following contents:

    # Hello World

    This is my first SCMS-rendered site!

Now our project structure looks like this:

    mysite/
        templates/
            default.vtl
        index.md
        scms.cfg

### Render your site

Now that we have our config, a template file and an initial bit of content, we can render our site.  Enter the project
root directory:

    $ cd mysite

Now render your site.  We'll specify `output` as our destination/output directory, relative to the project root.  SCMS
will render all output to the `output` directory.  You can specify a different directory if you want the output to be
somewhere else.  Run this:

    $ java -jar scms-cli-<VERSION>-cli.jar output

Where <VERSION> is replaced by the SCMS version you're using.

After you've run this command, you'll see the following directory structure:

    mysite/
        output/
            index.html
        templates/
            default.vtl
        index.md
        scms.cfg

See the new `output` directory with the `index.html` file?  Open it up in your web browser and see your new rendered page!

### How does this work?

Now that you've gotten your feet wet, here's what is going on:

SCMS will produce a 1:1 copy of the site in your source directory - `mysite` and all of its children directories and
all of their contents - to your specified destination directory.  But during that process, it will render all
Markdown files as HTML files using the specified Velocity template in `scms.cfg`.

As you can infer from `scms.cfg`, you can have multiple templates: for any Markdown path matching a particular pattern,
you can apply a specific template for that file path.

All that is left now is to learn a little bit of the [Velocity Template Language](http://velocity.apache.org/engine/devel/user-guide.html#Velocity_Template_Language_VTL:_An_Introduction)
so you can write as many `.vtl` Velocity templates as you want to customize the rendered output (look and feel) of your
site.

