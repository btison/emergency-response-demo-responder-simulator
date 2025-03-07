= REST API Level 0 - Eclipse Vert.x Booster

IMPORTANT: This booster requires Java 8 JDK or greater and Maven 3.3.x or greater.



== Runing the Booster Locally

To run this booster on your local host:

[source,bash,options="nowrap",subs="attributes+"]
----
$ unzip responder-simulator.zip

$ cd responder-simulator

$ mvn compile vertx:run
----

== Interacting with the Booster Locally

To interact with your booster while it's running locally, use the form at `http://localhost:8080` or the `curl` command:

[source,bash,options="nowrap",subs="attributes+"]
----
$ curl http://localhost:8080/api/greeting
{"content":"Hello, World!"}

$ curl http://localhost:8080/api/greeting?name=Sarah
{"content":"Hello, Sarah!"}
----


== Updating the Booster
To update your booster:

. Stop your booster.
+
NOTE: To stop your running booster in a Linux or macOS terminal, use `CTRL+C`. In a Windows command prompt, you can use `CTRL + Break(pause)`.

. Make your change (e.g. edit `src/main/resources/webroot/index.html`).
. Restart your booster.
. Verify the change took effect.


== Running the Booster on a Single-node OpenShift Cluster
If you have a single-node OpenShift cluster, such as Minishift or Red Hat Container Development Kit, link:http://launcher.fabric8.io/docs/minishift-installation.html[installed and running], you can also deploy your booster there. A single-node OpenShift cluster provides you with access to a cloud environment that is similar to a production environment.

To deploy your booster to a running single-node OpenShift cluster:
[source,bash,options="nowrap",subs="attributes+"]

----
$ oc login -u developer -p developer

$ oc new-project MY_PROJECT_NAME

$ oc create -f config/configmap.yml

$ mvn clean fabric8:deploy -Popenshift
----

== More Information
You can learn more about this booster and rest of the Eclipse Vert.x runtime in the link:http://launcher.fabric8.io/docs/vertx-runtime.html[Eclipse Vert.x Runtime Guide].

NOTE: Run the set of integration tests included with this booster using `mvn clean verify -Popenshift,openshift-it`.


