gridappclient
=============

Grid Application Client to run applications easily on the BiG Grid infrastructure, the dutch node in the European Grid Infrastructure. Basically this application wraps an SSH client, so users interact with an easy-to-use GUI only. 

Currently the only application integrated is TopHat (version 2.0.8b). To add an application a wrapper script, a form and some control code must be implemented. See the src/client/apps/tophat package as an example.

Required Libraries
------------------
* [sshj-0.8.1](https://github.com/downloads/shikhar/sshj/sshj-0.8.1.zip)
* [jtar-2.2](http://search.maven.org/remotecontent?filepath=org/kamranzafar/jtar/2.2/jtar-2.2.jar)
* [slf4j-1.7.2](http://www.slf4j.org/download.html) (api and jdk14 modules)
* [commons-io-2.4](http://commons.apache.org/proper/commons-io/download_io.cgi)
* [swiss-proxy-knife](https://github.com/grith/swiss-proxy-knife)
* [bcprov](http://www.bouncycastle.org/latest_releases.html) (required by swiss-proxy-knife)
* [vlet-1.5.0](http://sourceforge.net/projects/vlet/files/vlet-1.5.0/vlet-1.5.0.zip/download)

Requirements to Run the Application
-------------------
* Login credentials to a Grid User Interface (UI) machine.
* A valid grid certificate installed on your pc and the Grid UI machine you want to login to.

Notes
------------------
* The view has been created with the SWING GUI Builder integrated in the Netbeans IDE.
* The UI machine has been locked to ui.grid.sara.nl, because some meta-data is stored and not available if you switch to another UI machine.
