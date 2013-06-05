Grid Application Client
=============

Program to run applications easily on the [Life Science Grid](https://github.com/bstoker/gridappclient/wiki/Life-Science-Grid). Basically this program wraps an SSH client, so users interact with an easy-to-use graphical user interface (UI) only. Currently these applications are supported:
* [TopHat 2](http://tophat.cbcb.umd.edu/manual.shtml)

See [this page](https://github.com/bstoker/gridappclient/wiki/Adding-a-New-Application) on how to add an application.

Setup in Netbeans
------------------
1. Navigate to Team > Git > Clone...
2. Fill in the forms, repository URL is https://github.com/bstoker/gridappclient.git/
3. Leave 'Scan for NetBeans Projects after Clone' checked or navigate to File > New Project...
4. Choose 'Java Project with Existing Sources' under the category 'Java'
5. Set the Project Folder to the directory you cloned the git repository to
6. Add 'client' and 'images' as Source Package Folders, click next and finish
7. Add the required libraries to your project, build and run it

Required Libraries
------------------
* [sshj-0.8.1](https://github.com/downloads/shikhar/sshj/sshj-0.8.1.zip)
* [jtar-2.2](http://search.maven.org/remotecontent?filepath=org/kamranzafar/jtar/2.2/jtar-2.2.jar)
* [slf4j-1.7.2](http://www.slf4j.org/download.html) (api and jdk14 modules)
* [commons-io-2.4](http://commons.apache.org/proper/commons-io/download_io.cgi)
* [swiss-proxy-knife](https://github.com/grith/swiss-proxy-knife/tree/develop)
* [bcprov](http://www.bouncycastle.org/latest_releases.html) (required by swiss-proxy-knife)
* [vlet-1.5.0](http://sourceforge.net/projects/vlet/files/vlet-1.5.0/vlet-1.5.0.zip/download)

Other Requirements
-------------------
* Java 7 or higher
* Login credentials to a Grid UI machine.
* A valid grid certificate installed on your pc and the Grid UI machine you want to login to.
See [this page](https://github.com/bstoker/gridappclient/wiki/Accessing-the-Life-Science-Grid) on how to access the Life Science Grid.

Notes
------------------
* The view has been created with the SWING GUI Builder integrated in the Netbeans IDE.
* The UI machine has been locked to ui.grid.sara.nl, because some meta-data is stored and not available if you switch to another UI machine.
* When building the application the vlet-1.5.0 folder is not automatically added to the dist/lib/ folder, because it's a directory. Adding [this](https://github.com/bstoker/gridappclient/wiki/Adding-VLET-folder-to-lib-on-build) code to your build.xml file copies the folder on build.
