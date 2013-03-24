SCMS
====

SCMS (Simple Content Management System) is an extremely simple tool that exists to support static (or mostly static)
websites (or dynamic sites that achieve their dynamic nature mostly through JavaScript).

SCMS is extraordinarily simple: given a source directory tree full of Markdown files and html fragments, it
generates a new 1-to-1 directory tree with fully rendered HTML files.  It is expected that you will manage your content
(markdown files, images, etc) using a version control system (for example, git).

When it comes time to preview your changes in HTML or publish your site to web servers,  you run a simple SCMS command
line script to render your web site.  You can take the resulting directory tree and either commit to to version control
or just push it directly to your web servers.  Super easy.

Rendered output is fully customizable via [Velocity templates](http://velocity.apache.org/engine/devel/user-guide.html)

Note:

While this does work at the moment, it is currently a brand new project and quite rough around the edges.  Work needs
to be done to encapsulate the functionality as a standalone command line executable.  Most rendering testing
is being done within the IDE, leaving the command-line executable for the next stage of development.