================================================================================
================================================================================

           Third-Party Software for dpn-federator-certificate-manager

--------------------------------------------------------------------------------

The following 3rd-party software packages may be used by or distributed with dpn-federator-certificate-manager. Any information relevant to third-party vendors listed below are collected using common, reasonable means.

Date generated: 2026-9-23

Revision ID: 3288017199b7354cc349972a1c339b4f442dbed2

================================================================================
================================================================================



================================================================================

                                  Dependencies

================================================================================

- Apache Commons Lang (3.18.0) [Apache-2.0]
- Apache Commons Logging (1.3.6) [Apache-2.0]
- Apache HttpClient (5.6.4) [Apache-2.0]
- Apache HttpComponents Core HTTP/1.1 (5.4.3) [Apache-2.0]
- Apache HttpComponents Core HTTP/2 (5.4.3) [Apache-2.0]
- Apache Log4j API (2.25.5) [Apache-2.0]
- Apache Log4j to SLF4J Adapter (2.25.5) [Apache-2.0]
- asm (9.7.1) [BSD-3-Clause]
- ASM based accessors helper used by json-smart (2.6.0) [Apache-2.0]
- AspectJ Weaver (1.9.25.1) [EPL-2.0]
- Bouncy Castle ASN.1 Extension and Utility APIs (1.85) [BouncyCastle]
- Bouncy Castle PKIX, CMS, EAC, TSP, PKCS, OCSP, CMP, and CRMF APIs (1.85) [BouncyCastle]
- Bouncy Castle Provider (1.85) [BouncyCastle]
- Caffeine cache (3.2.4) [Apache-2.0]
- context-propagation (1.2.1) [Apache-2.0]
- error-prone annotations (2.49.0) [Apache-2.0]
- HdrHistogram (2.2.2) [BSD-2-Clause]
- Jackson-annotations (2.21) [Apache-2.0]
- Jackson-core (2.21.6) [Apache-2.0]
- Jackson-core (3.1.6) [Apache-2.0]

--------------------------------------------------------------------------------
Package Title: Apache Commons Lang (3.18.0)

Package Locator: mvn+org.apache.commons:commons-lang3$3.18.0

Package Depth: Direct
--------------------------------------------------------------------------------


  Apache Commons Lang, a package of Java utility classes for the
  classes that are in java.lang's hierarchy, or are considered to be so
  standard as to justify existence in java.lang.

  The code is tested using the latest revision of the JDK for supported
  LTS releases: 8, 11, 17, 21 and 25 currently.
  See https://github.com/apache/commons-lang/blob/master/.github/workflows/maven.yml
  
  Please ensure your build environment is up-to-date and kindly report any build issues.
  

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Adam Hooper, Adrian Ber, Al Chou, Alban Peignier, Alexander Day Chaffee, Allon Mureinik, Andrew C. Oliver, Antony Riley, Arturo Bernal, Arun Mammen Thomas, Ashwin Suresh, Benedikt Ritter, Benjamin Bentmann, Brian S O'Neill, C. Scott Ananian, Chas Honton, Chris Audley, Chris Feldhacker, Chris Hyzer, Chris Karcher, Chris Webb, Christopher Elkins, Craig R. McClanahan, Daniel Rall, Daniel Trebbien, Dave Meikle, David Leppik, David M. Sledge, Derek C. Ashmore, Dmitri Plotnikov, Duncan Jones, Ed Korthof, Eli Lindsey, Eric Pugh, Fabian Lange, Felipe Adorno, Fredrik Westermarck, Gary Gregory, Glen Stampoultzis, Greg Coladonato, Helge Tesgaard, Hendrik Maryns, Henning P. Schmiedehausen, Henri Yandell, Holger Hoffstatte, Holger Krauth, James Carman, James Sawle, Jan Sorensen, Janek Bogucki, Jason Gritman, Jeff Varszegi, Jin Xu, Joerg Schaible, Jon S. Stevens, Jonathan Baker, Justin Couch, Kasper Nielsen, Loic Guibert, Maarten Coene, Marc Johnson, Mario Winterer, Mark Dacek, Masato Tezuka, Matt Benson, Matthew Hawthorne, Matthias Eichel, Michael A. Smith, Michael Becke, Michael Davey, Michael Heuer, Michael Osipov, Michał Kordas, Mike Bowler, Mikhail Mazursky, Morgan Delagrange, Moritz Petersen, Nathan Beyer, Neeme Praks, Niall Pemberton, Nikolay Metchev, Nissim Karpenstein, Norm Deane, Ola Berg, Oliver Heger, Paul Benedict, Paul Jack, Pete Gieser, Peter Verhas, Rafal Krupinski, Rafal Krzewski, Ralph Schaer, Rand McNeely, Reuben Sivan, Ringo De Smet, Rob Tompkins, Robert Burrell Donkin, Robert Scholte, Roland Foerther, Russel Dittmar, Scott Sanders, Scott Stanchfield, Sean Brown, Sean C. Sullivan, Sean Schofield, Sebastien Riou, Shaun Kalley, Stefan Bodewig, Stepan Koltsov, Stephane Bailliez, Stephen Colebourne, Stephen Putman, Steve Downey, Steven Caswell, Sven Ludwig, Tetsuya Kaneuchi, Thiago Andrade, Tim O'Brien, Travis Reeder, Valentin Rocher, Ville Skytta
Package Manager: Maven
Project URL: https://commons.apache.org/proper/commons-lang/
Package Download URL: https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0-sources.jar
Dependency Path: org.apache.commons:commons-lang3
* Notice File(s) *
META-INF/NOTICE.txt
Apache Commons Lang
Copyright 2001-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).


--------------------------------------------------------------------------------
Package Title: Apache Commons Logging (1.3.6)

Package Locator: mvn+commons-logging:commons-logging$1.3.6

Package Depth: Transitive
--------------------------------------------------------------------------------

Apache Commons Logging is a thin adapter allowing configurable bridging to other,
    well-known logging systems.

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Arturo Bernal, Berin Loritsch, Brian Stansberry, Costin Manolache, Craig McClanahan, Dennis Lundberg, Gary Gregory, Juozas Baliuka, Matthew P. Del Buono, Morgan Delagrange, Neeme Praks, Peter Donald, Peter Lawrey, Philippe Mouawad, Richard Sitze, Robert Burrell Donkin, Rodney Waldhoff, Scott Sanders, Simon Kitching, Thomas Neidhart, Vince Eagen
Package Manager: Maven
Project URL: https://commons.apache.org/proper/commons-logging/
Package Download URL: https://repo1.maven.org/maven2/commons-logging/commons-logging/1.3.6/commons-logging-1.3.6-sources.jar
Dependency Path: io.github.resilience4j:resilience4j-spring-boot4 > org.springframework:spring-core > commons-logging:commons-logging > org.springframework.boot:spring-boot-starter-actuator > org.springframework.boot:spring-boot-actuator-autoconfigure > org.springframework.boot:spring-boot-actuator > org.springframework.boot:spring-boot > org.springframework:spring-core > commons-logging:commons-logging > org.springframework.boot:spring-boot-starter-aspectj > org.springframework:spring-aop > org.springframework:spring-core > commons-logging:commons-logging > org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-security-oauth2-client > org.springframework.boot:spring-boot-security > org.springframework.security:spring-security-config > org.springframework.security:spring-security-core > org.springframework:spring-core > commons-logging:commons-logging > org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-security-oauth2-client > org.springframework.boot:spring-boot-security > org.springframework.security:spring-security-config > org.springframework:spring-beans > org.springframework:spring-core > commons-logging:commons-logging > org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-security-oauth2-client > org.springframework.boot:spring-boot-security > org.springframework.security:spring-security-config > org.springframework:spring-core > commons-logging:commons-logging > org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-security-oauth2-client > org.springframework.boot:spring-boot-security > org.springframework.security:spring-security-web > org.springframework:spring-core > commons-logging:commons-logging > org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-security-oauth2-client > org.springframework.security:spring-security-oauth2-client > org.springframework.security:spring-security-oauth2-core > org.springframework:spring-core > commons-logging:commons-logging > org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-security-oauth2-client > org.springframework.security:spring-security-oauth2-client > org.springframework:spring-core > commons-logging:commons-logging > org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.security:spring-security-oauth2-jose > org.springframework:spring-core > commons-logging:commons-logging
* Notice File(s) *
META-INF/NOTICE.txt
Apache Commons Logging
Copyright 2001-2026 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).


--------------------------------------------------------------------------------
Package Title: Apache HttpClient (5.6.4)

Package Locator: mvn+org.apache.httpcomponents.client5:httpclient5$5.6.4

Package Depth: Direct
--------------------------------------------------------------------------------

Apache HttpComponents Client

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Andrea Selva, Ant Elder, Arturo Bernal, Asankha C. Perera, Erik Abele, Francois-Xavier Bonnet, Gary Gregory, James Abley, Jonathan Moore, Julius Davies, Karl Wright, Marc Beyerle, Michael Osipov, Michajlo Matijkiw, Oleg Kalnichevski, Ortwin Glueck, Paul Fremantle, Quintin Beukes, Roland Weber, Ryan Schmitt, Sam Berlin, Sean C. Sullivan, Sebastian Bazley, Steffen Pingel, William Speirs
Package Manager: Maven
Project URL: https://hc.apache.org/httpcomponents-client-5.7.x/5.7-alpha1/
Package Download URL: https://repo1.maven.org/maven2/org/apache/httpcomponents/client5/httpclient5/5.6.4/httpclient5-5.6.4-sources.jar
Dependency Path: org.apache.httpcomponents.client5:httpclient5 > org.springframework.cloud:spring-cloud-starter-vault-config > org.apache.httpcomponents.client5:httpclient5
* Notice File(s) *
META-INF/NOTICE

Apache HttpClient
Copyright 1999-2021 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).




--------------------------------------------------------------------------------
Package Title: Apache HttpComponents Core HTTP/1.1 (5.4.3)

Package Locator: mvn+org.apache.httpcomponents.core5:httpcore5$5.4.3

Package Depth: Transitive
--------------------------------------------------------------------------------

Apache HttpComponents HTTP/1.1 core components

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Andrea Selva, Ant Elder, Arturo Bernal, Asankha C. Perera, Erik Abele, Francois-Xavier Bonnet, Gary Gregory, James Abley, Jonathan Moore, Julius Davies, Karl Wright, Marc Beyerle, Michael Osipov, Michajlo Matijkiw, Oleg Kalnichevski, Ortwin Glueck, Paul Fremantle, Quintin Beukes, Roland Weber, Ryan Schmitt, Sam Berlin, Sean C. Sullivan, Sebastian Bazley, Steffen Pingel, William Speirs
Package Manager: Maven
Project URL: https://hc.apache.org/httpcomponents-core-5.5.x/5.5-beta2/
Package Download URL: https://repo1.maven.org/maven2/org/apache/httpcomponents/core5/httpcore5/5.4.3/httpcore5-5.4.3-sources.jar
Dependency Path: org.apache.httpcomponents.client5:httpclient5 > org.apache.httpcomponents.core5:httpcore5 > org.apache.httpcomponents.client5:httpclient5 > org.apache.httpcomponents.core5:httpcore5-h2 > org.apache.httpcomponents.core5:httpcore5 > org.springframework.cloud:spring-cloud-starter-vault-config > org.apache.httpcomponents.client5:httpclient5 > org.apache.httpcomponents.core5:httpcore5 > org.springframework.cloud:spring-cloud-starter-vault-config > org.apache.httpcomponents.client5:httpclient5 > org.apache.httpcomponents.core5:httpcore5-h2 > org.apache.httpcomponents.core5:httpcore5 > org.springframework.cloud:spring-cloud-starter-vault-config > org.apache.httpcomponents.core5:httpcore5
* Notice File(s) *
META-INF/NOTICE

Apache HttpComponents Core HTTP/1.1
Copyright 2005-2021 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).




--------------------------------------------------------------------------------
Package Title: Apache HttpComponents Core HTTP/2 (5.4.3)

Package Locator: mvn+org.apache.httpcomponents.core5:httpcore5-h2$5.4.3

Package Depth: Transitive
--------------------------------------------------------------------------------

Apache HttpComponents HTTP/2 Core Components

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Andrea Selva, Ant Elder, Arturo Bernal, Asankha C. Perera, Erik Abele, Francois-Xavier Bonnet, Gary Gregory, James Abley, Jonathan Moore, Julius Davies, Karl Wright, Marc Beyerle, Michael Osipov, Michajlo Matijkiw, Oleg Kalnichevski, Ortwin Glueck, Paul Fremantle, Quintin Beukes, Roland Weber, Ryan Schmitt, Sam Berlin, Sean C. Sullivan, Sebastian Bazley, Steffen Pingel, William Speirs
Package Manager: Maven
Project URL: https://hc.apache.org/httpcomponents-core-5.5.x/5.5-beta2/
Package Download URL: https://repo1.maven.org/maven2/org/apache/httpcomponents/core5/httpcore5-h2/5.4.3/httpcore5-h2-5.4.3-sources.jar
Dependency Path: org.apache.httpcomponents.client5:httpclient5 > org.apache.httpcomponents.core5:httpcore5-h2 > org.springframework.cloud:spring-cloud-starter-vault-config > org.apache.httpcomponents.client5:httpclient5 > org.apache.httpcomponents.core5:httpcore5-h2
* Notice File(s) *
META-INF/NOTICE

Apache HttpComponents Core HTTP/2
Copyright 2005-2021 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).




--------------------------------------------------------------------------------
Package Title: Apache Log4j API (2.25.5)

Package Locator: mvn+org.apache.logging.log4j:log4j-api$2.25.5

Package Depth: Transitive
--------------------------------------------------------------------------------

The Apache Log4j API

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Bruce Brouwer, Carter Kozak, Christian Grobmeier, Gary Gregory, Matt Sicker, Mikael Ståldal, Nick Williams, Piotr P. Karwasz, Ralph Goers, Raman Gupta, Remko Popma, Ron Grabowski, Scott Deboy, Volkan Yazıcı
Package Manager: Maven
Project URL: https://logging.apache.org/log4j/3.x/
Package Download URL: https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.25.5/log4j-api-2.25.5-sources.jar
Dependency Path: org.springframework.boot:spring-boot-starter-actuator > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api > org.springframework.boot:spring-boot-starter-actuator > org.springframework.boot:spring-boot-starter-micrometer-metrics > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api > org.springframework.boot:spring-boot-starter-aspectj > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api > org.springframework.boot:spring-boot-starter-cache > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api > org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api > org.springframework.boot:spring-boot-starter-security > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api > org.springframework.boot:spring-boot-starter-web > org.springframework.boot:spring-boot-starter-jackson > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api > org.springframework.boot:spring-boot-starter-web > org.springframework.boot:spring-boot-starter-tomcat > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api > org.springframework.cloud:spring-cloud-starter-vault-config > org.springframework.cloud:spring-cloud-starter > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api > org.springframework.cloud:spring-cloud-starter-vault-config > org.springframework.cloud:spring-cloud-vault-config > org.springframework.cloud:spring-cloud-starter > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.apache.logging.log4j:log4j-api
* Notice File(s) *
META-INF/NOTICE
Apache Log4j API
Copyright 1999-2026 The Apache Software Foundation


This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).


--------------------------------------------------------------------------------
Package Title: Apache Log4j to SLF4J Adapter (2.25.5)

Package Locator: mvn+org.apache.logging.log4j:log4j-to-slf4j$2.25.5

Package Depth: Transitive
--------------------------------------------------------------------------------

The Apache Log4j binding between Log4j 2 API and SLF4J.

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Bruce Brouwer, Carter Kozak, Christian Grobmeier, Gary Gregory, Matt Sicker, Mikael Ståldal, Nick Williams, Piotr P. Karwasz, Ralph Goers, Raman Gupta, Remko Popma, Ron Grabowski, Scott Deboy, Volkan Yazıcı
Package Manager: Maven
Project URL: https://logging.apache.org/log4j/3.x/
Package Download URL: https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-to-slf4j/2.25.5/log4j-to-slf4j-2.25.5-sources.jar
Dependency Path: org.springframework.boot:spring-boot-starter-actuator > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.springframework.boot:spring-boot-starter-actuator > org.springframework.boot:spring-boot-starter-micrometer-metrics > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.springframework.boot:spring-boot-starter-aspectj > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.springframework.boot:spring-boot-starter-cache > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.springframework.boot:spring-boot-starter-security > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.springframework.boot:spring-boot-starter-web > org.springframework.boot:spring-boot-starter-jackson > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.springframework.boot:spring-boot-starter-web > org.springframework.boot:spring-boot-starter-tomcat > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.springframework.cloud:spring-cloud-starter-vault-config > org.springframework.cloud:spring-cloud-starter > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j > org.springframework.cloud:spring-cloud-starter-vault-config > org.springframework.cloud:spring-cloud-vault-config > org.springframework.cloud:spring-cloud-starter > org.springframework.boot:spring-boot-starter > org.springframework.boot:spring-boot-starter-logging > org.apache.logging.log4j:log4j-to-slf4j
* Notice File(s) *
META-INF/NOTICE
Log4j API to SLF4J Adapter
Copyright 1999-2026 The Apache Software Foundation


This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).


--------------------------------------------------------------------------------
Package Title: asm (9.7.1)

Package Locator: mvn+org.ow2.asm:asm$9.7.1

Package Depth: Transitive
--------------------------------------------------------------------------------

ASM, a very small and fast Java bytecode manipulation framework

* Concluded Licenses *
BSD-3-Clause

* Copyrights *
BSD-3-Clause
- Copyright (c) 2000-2011 INRIA, France Telecom

* Package Info *

Authors: Eric Bruneton, Eugene Kuleshov, Guillaume Sauthier, Remi Forax
Package Manager: Maven
Project URL: http://asm.ow2.io/
Package Download URL: https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1-sources.jar
Dependency Path: org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-security-oauth2-client > org.springframework.security:spring-security-oauth2-client > com.nimbusds:oauth2-oidc-sdk > net.minidev:json-smart > net.minidev:accessors-smart > org.ow2.asm:asm

--------------------------------------------------------------------------------
Package Title: ASM based accessors helper used by json-smart (2.6.0)

Package Locator: mvn+net.minidev:accessors-smart$2.6.0

Package Depth: Transitive
--------------------------------------------------------------------------------

Java reflect give poor performance on getter setter an constructor calls, accessors-smart use ASM to speed up those calls.

* Concluded Licenses *
Apache-2.0

* Copyrights *
Apache-2.0
- Copyright (c) 2011-2025 JSON-SMART authors

* Package Info *

Authors: Uriel Chemouni, Zhangjian He
Package Manager: Maven
Project URL: https://urielch.github.io/
Package Download URL: https://repo1.maven.org/maven2/net/minidev/accessors-smart/2.6.0/accessors-smart-2.6.0-sources.jar
Dependency Path: org.springframework.boot:spring-boot-starter-oauth2-client > org.springframework.boot:spring-boot-security-oauth2-client > org.springframework.security:spring-security-oauth2-client > com.nimbusds:oauth2-oidc-sdk > net.minidev:json-smart > net.minidev:accessors-smart

--------------------------------------------------------------------------------
Package Title: AspectJ Weaver (1.9.25.1)

Package Locator: mvn+org.aspectj:aspectjweaver$1.9.25.1

Package Depth: Transitive
--------------------------------------------------------------------------------

The AspectJ weaver applies aspects to Java classes. It can be used as a Java agent in order to apply load-time
		weaving (LTW) during class-loading and also contains the AspectJ runtime classes.

* Concluded Licenses *
EPL-2.0

* Copyrights *
EPL-2.0
- Copyright (c) 2001 The Apache Software Foundation.  All rights
- Copyright (c) 2000-2011 INRIA, France Telecom

* Package Info *

Authors: Alexander Kriegisch, Andy Clement
Package Manager: Maven
Project URL: https://www.eclipse.org/aspectj/
Package Download URL: https://repo1.maven.org/maven2/org/aspectj/aspectjweaver/1.9.25.1/aspectjweaver-1.9.25.1-sources.jar
Dependency Path: org.springframework.boot:spring-boot-starter-aspectj > org.aspectj:aspectjweaver

--------------------------------------------------------------------------------
Package Title: Bouncy Castle ASN.1 Extension and Utility APIs (1.85)

Package Locator: mvn+org.bouncycastle:bcutil-jdk18on$1.85

Package Depth: Transitive
--------------------------------------------------------------------------------

The Bouncy Castle Java APIs for ASN.1 extension and utility APIs used to support bcpkix and bctls. This jar contains  APIs for Java 1.8 and later.

* Concluded Licenses *
BouncyCastle

* Copyrights *
BouncyCastle
- Copyright (c) 2000-2026 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org).

* Package Info *

Authors: The Legion of the Bouncy Castle Inc.
Package Manager: Maven
Project URL: https://www.bouncycastle.org/download/bouncy-castle-java/
Package Download URL: https://repo1.maven.org/maven2/org/bouncycastle/bcutil-jdk18on/1.85/bcutil-jdk18on-1.85-sources.jar
Dependency Path: org.bouncycastle:bcpkix-jdk18on > org.bouncycastle:bcutil-jdk18on

--------------------------------------------------------------------------------
Package Title: Bouncy Castle PKIX, CMS, EAC, TSP, PKCS, OCSP, CMP, and CRMF APIs (1.85)

Package Locator: mvn+org.bouncycastle:bcpkix-jdk18on$1.85

Package Depth: Direct
--------------------------------------------------------------------------------

The Bouncy Castle Java APIs for CMS, PKCS, EAC, TSP, CMP, CRMF, OCSP, and certificate generation. This jar contains  APIs for Java 1.8 and later. The APIs are designed primarily to be used in conjunction with the BC Java provider but may also be used with other providers providing cryptographic services.

* Concluded Licenses *
BouncyCastle

* Copyrights *
BouncyCastle
- Copyright (c) 2000-2026 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org).

* Package Info *

Authors: The Legion of the Bouncy Castle Inc.
Package Manager: Maven
Project URL: https://www.bouncycastle.org/download/bouncy-castle-java/
Package Download URL: https://repo1.maven.org/maven2/org/bouncycastle/bcpkix-jdk18on/1.85/bcpkix-jdk18on-1.85-sources.jar
Dependency Path: org.bouncycastle:bcpkix-jdk18on

--------------------------------------------------------------------------------
Package Title: Bouncy Castle Provider (1.85)

Package Locator: mvn+org.bouncycastle:bcprov-jdk18on$1.85

Package Depth: Direct
--------------------------------------------------------------------------------

The Bouncy Castle Crypto package is a Java implementation of cryptographic algorithms. This jar contains the  JCA/JCE provider and low-level API for the BC Java version 1.86 for Java 1.8 and later.

* Concluded Licenses *
BouncyCastle

* Copyrights *
BouncyCastle
- Copyright (c) 2000-2026 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org).
- Copyright (c) 2000-2023 The Legion Of The Bouncy Castle Inc. (https://www.bouncycastle.org)

* Package Info *

Authors: The Legion of the Bouncy Castle Inc.
Package Manager: Maven
Project URL: https://www.bouncycastle.org/download/bouncy-castle-java/
Package Download URL: https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/1.85/bcprov-jdk18on-1.85-sources.jar
Dependency Path: org.bouncycastle:bcprov-jdk18on

--------------------------------------------------------------------------------
Package Title: Caffeine cache (3.2.4)

Package Locator: mvn+com.github.ben-manes.caffeine:caffeine$3.2.4

Package Depth: Direct
--------------------------------------------------------------------------------

A high performance caching library

* Concluded Licenses *
Apache-2.0

* Copyrights *
Apache-2.0
- Copyright (c) 2014 Ben Manes. All Rights Reserved.
- Copyright (c) 2015 Ben Manes. All Rights Reserved.
- Copyright (c) 2026 Ben Manes. All Rights Reserved.
- Copyright (c) 2018 Ben Manes. All Rights Reserved.
- Copyright (c) 2016 Ben Manes. All Rights Reserved.
- Copyright (c) 2019 Ben Manes. All Rights Reserved.
- Copyright (c) 2022 Ben Manes. All Rights Reserved.
- Copyright (c) 2017 Ben Manes. All Rights Reserved.

* Package Info *

Authors: Ben Manes
Package Manager: Maven
Project URL: https://github.com/ben-manes/caffeine
Package Download URL: https://repo1.maven.org/maven2/com/github/ben-manes/caffeine/caffeine/3.2.4/caffeine-3.2.4-sources.jar
Dependency Path: com.github.ben-manes.caffeine:caffeine

--------------------------------------------------------------------------------
Package Title: context-propagation (1.2.1)

Package Locator: mvn+io.micrometer:context-propagation$1.2.1

Package Depth: Transitive
--------------------------------------------------------------------------------

A library that assists with context propagation across different types of context mechanisms such as ThreadLocal, Reactor Context etc.

* Concluded Licenses *
Apache-2.0

* Copyrights *
Apache-2.0
- Copyright (c) 2022 the original author or authors.
- Copyright (c) 2023 the original author or authors.
- Copyright (c) 2024-2025 the original author or authors.
- Copyright (c) 2024 the original author or authors.

* Package Info *

Authors: Jonatan Ivanov, Marcin Grzejszczak, Tommy Ludwig
Package Manager: Maven
Project URL: https://github.com/micrometer-metrics/context-propagation
Package Download URL: https://repo1.maven.org/maven2/io/micrometer/context-propagation/1.2.1/context-propagation-1.2.1-sources.jar
Dependency Path: io.micrometer:micrometer-tracing-bridge-otel > io.micrometer:micrometer-tracing > io.micrometer:context-propagation > org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry > io.micrometer:micrometer-tracing > io.micrometer:context-propagation > org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry > org.springframework.boot:spring-boot-micrometer-tracing > io.micrometer:micrometer-tracing > io.micrometer:context-propagation

--------------------------------------------------------------------------------
Package Title: error-prone annotations (2.49.0)

Package Locator: mvn+com.google.errorprone:error_prone_annotations$2.49.0

Package Depth: Transitive
--------------------------------------------------------------------------------

Error Prone is a static analysis tool for Java that catches common programming mistakes at compile-time.

* Concluded Licenses *
Apache-2.0

* Copyrights *
Apache-2.0
- Copyright (c) 2015 The Error Prone Authors.
- Copyright (c) 2016 The Error Prone Authors.
- Copyright (c) 2021 The Error Prone Authors.
- Copyright (c) 2017 The Error Prone Authors.
- Copyright (c) 2018 The Error Prone Authors.
- Copyright (c) 2014 The Error Prone Authors.
- Copyright (c) 2023 The Error Prone Authors.

* Package Info *

Authors: Eddie Aftandilian
Package Manager: Maven
Project URL: https://errorprone.info/
Package Download URL: https://repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.49.0/error_prone_annotations-2.49.0-sources.jar
Dependency Path: com.github.ben-manes.caffeine:caffeine > com.google.errorprone:error_prone_annotations

--------------------------------------------------------------------------------
Package Title: HdrHistogram (2.2.2)

Package Locator: mvn+org.hdrhistogram:HdrHistogram$2.2.2

Package Depth: Transitive
--------------------------------------------------------------------------------


        HdrHistogram supports the recording and analyzing sampled data value
        counts across a configurable integer value range with configurable value
        precision within the range. Value precision is expressed as the number of
        significant digits in the value recording, and provides control over value
        quantization behavior across the value range and the subsequent value
        resolution at any given level.
    

* Concluded Licenses *
BSD-2-Clause

* Copyrights *
BSD-2-Clause
- Copyright (c) 2012, 2013, 2014, 2015, 2016 Gil Tene
- Copyright (c) 2014 Michael Barker
- Copyright (c) 2014 Matt Warren

* Package Info *

Authors: Gil Tene
Package Manager: Maven
Project URL: http://hdrhistogram.github.io/HdrHistogram/
Package Download URL: https://repo1.maven.org/maven2/org/hdrhistogram/HdrHistogram/2.2.2/HdrHistogram-2.2.2-sources.jar
Dependency Path: io.github.resilience4j:resilience4j-spring-boot4 > io.github.resilience4j:resilience4j-micrometer > io.micrometer:micrometer-core > org.hdrhistogram:HdrHistogram > io.github.resilience4j:resilience4j-spring-boot4 > io.github.resilience4j:resilience4j-spring6 > io.github.resilience4j:resilience4j-framework-common > io.github.resilience4j:resilience4j-micrometer > io.micrometer:micrometer-core > org.hdrhistogram:HdrHistogram > org.springframework.boot:spring-boot-starter-actuator > io.micrometer:micrometer-jakarta9 > io.micrometer:micrometer-core > org.hdrhistogram:HdrHistogram > org.springframework.boot:spring-boot-starter-actuator > org.springframework.boot:spring-boot-starter-micrometer-metrics > org.springframework.boot:spring-boot-micrometer-metrics > io.micrometer:micrometer-core > org.hdrhistogram:HdrHistogram

--------------------------------------------------------------------------------
Package Title: Jackson-annotations (2.21)

Package Locator: mvn+com.fasterxml.jackson.core:jackson-annotations$2.21

Package Depth: Transitive
--------------------------------------------------------------------------------

Core annotations used for value types, used by Jackson data binding package.
  

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Tatu Saloranta
Package Manager: Maven
Project URL: http://github.com/FasterXML/jackson
Package Download URL: https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.21/jackson-annotations-2.21-sources.jar
Dependency Path: com.fasterxml.jackson.core:jackson-databind > com.fasterxml.jackson.core:jackson-annotations > org.springframework.boot:spring-boot-starter-web > org.springframework.boot:spring-boot-starter-jackson > org.springframework.boot:spring-boot-jackson > tools.jackson.core:jackson-databind > com.fasterxml.jackson.core:jackson-annotations > org.springframework.cloud:spring-cloud-starter-vault-config > org.springframework.cloud:spring-cloud-vault-config > com.fasterxml.jackson.core:jackson-annotations > org.springframework.cloud:spring-cloud-starter-vault-config > org.springframework.cloud:spring-cloud-vault-config > tools.jackson.core:jackson-databind > com.fasterxml.jackson.core:jackson-annotations
* Notice File(s) *
META-INF/NOTICE
# Jackson JSON processor

Jackson is a high-performance, Free/Open Source JSON processing library.
It was originally written by Tatu Saloranta (tatu.saloranta@iki.fi), and has
been in development since 2007.
It is currently developed by a community of developers.

## Copyright

Copyright 2007-, Tatu Saloranta (tatu.saloranta@iki.fi)

## Licensing

Jackson 2.x core and extension components are licensed under Apache License 2.0
To find the details that apply to this artifact see the accompanying LICENSE file.

## Credits

A list of contributors may be found from CREDITS(-2.x) file, which is included
in some artifacts (usually source distributions); but is always available
from the source code management (SCM) system project uses.


--------------------------------------------------------------------------------
Package Title: Jackson-core (2.21.6)

Package Locator: mvn+com.fasterxml.jackson.core:jackson-core$2.21.6

Package Depth: Transitive
--------------------------------------------------------------------------------

Core Jackson processing abstractions (aka Streaming API), implementation for JSON

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Tatu Saloranta
Package Manager: Maven
Project URL: https://github.com/FasterXML/jackson-core
Package Download URL: https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.21.6/jackson-core-2.21.6-sources.jar
Dependency Path: com.fasterxml.jackson.core:jackson-databind > com.fasterxml.jackson.core:jackson-core

--------------------------------------------------------------------------------
Package Title: Jackson-core (3.1.6)

Package Locator: mvn+tools.jackson.core:jackson-core$3.1.6

Package Depth: Transitive
--------------------------------------------------------------------------------

Core Jackson processing abstractions (aka Streaming API), implementation for JSON

* Concluded Licenses *
Apache-2.0

* Package Info *

Authors: Tatu Saloranta
Package Manager: Maven
Project URL: https://github.com/FasterXML/jackson-core
Package Download URL: https://repo1.maven.org/maven2/tools/jackson/core/jackson-core/3.1.6/jackson-core-3.1.6-sources.jar
Dependency Path: org.springframework.boot:spring-boot-starter-web > org.springframework.boot:spring-boot-starter-jackson > org.springframework.boot:spring-boot-jackson > tools.jackson.core:jackson-databind > tools.jackson.core:jackson-core > org.springframework.cloud:spring-cloud-starter-vault-config > org.springframework.cloud:spring-cloud-vault-config > tools.jackson.core:jackson-databind > tools.jackson.core:jackson-core


--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

Report Generated by FOSSA on 2026-9-23