OSGi specification testframework
=================================

[The project home page](https://github.com/wtreur/osgi-specification-test-framework)

This projects contains a system for testing the specification conformance of an OSGi framework.
The tests are created to run inside Pax Exam so different framework vendors can be used.

To build the framework, use 'ant' Refer to project home page for more information

JUnit Pax Exam tests
--------------------
The JUnit tests are executed by the Pax Exam JUnitTestRunner. This creates a testcontainer that runs Pax Exam in a
separate VM. Every testmethod is executed in a new framework instance.


A short overview of the project's code:


*   `/src`<br />
    contains all sources, from which the testing bundles will be built
*   `/ext`<br />
    contains all compile- and runtime dependencies for running the tests
*   `/lib`<br />
    contains compile-time only dependencies
*   `/var`<br />
    contains resources for creating the testreport

Testing OSGi specification compliance
-------------------------------------

Use separete project [OSGi specification tests](https://github.com/wtreur/osgi-specification-tests)
