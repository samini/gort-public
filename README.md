#gort

The _gort-public_ repository contains the source code (v2) I wrote for my thesis, _Analyzing Mobile App Privacy Using Computation and Crowdsourcing_. The thesis document is available [here](http://cmuchimps.org/publications/analyzing_mobile_app_privacy_using_computation_and_crowdsourcin_2014 "Analyzing Mobile App Privacy Using Computation and Crowdsourcing").

##Thesis Abstract

Mobile apps can make use of the rich data and sensors available on smartphones to offer compelling services. However, the use of sensitive resources by apps is not always justified, which has led to new kinds of privacy risks and challenges. While it is possible for app market owners and third-parties to analyze the privacy-related behaviors of apps, present approaches are difficult and tedious.

I present two iterations of the design, implementation, and evaluation of a system, Gort, which enables more efficient app analysis, by reducing the burden of instrumenting apps, making it easier to find potential privacy problems, and presenting sensitive behavior in context. Gort interacts with apps while instrumenting them to detect sensitive information transmissions. It then presents this information along with the associated app context to a crowd of users to obtain their expectations and comfort regarding the privacy implications of using the app. Gort also runs a set of heuristics on the app to flag potential privacy problems. Finally, Gort synthesizes the information obtained through its analysis and presents it in an interactive GUI, built specifically for privacy analysts.

This work offers three distinct new advances over the state of the art. First, Gort uses a set of heuristics, elicited through interviews with 12 experts, to identify potential app privacy problems. Gort heuristics present high-level privacy problems instead of the overwhelming amount of information offered through existing tools. Second, Gort automatically interacts with apps by discovering and interacting with UI elements while instrumenting app behavior. This eliminates the need for analysts to manually interact with apps or to script interactions. Third, Gort uses crowdsourcing in a novel way to determine whether app privacy leaks are legitimate and desirable and raises red flags about potentially suspicious app behavior. While existing tools can detect privacy leaks, they cannot determine whether the privacy leaks are beneficial or desirable to the user. Gort was evaluated through two separate user studies. The experiences from building Gort and the insights from the user studies guide the creation of future systems, especially systems intended for the inspection and analysis of software.

##Building

This part of the documentation is currently a work in progress.

### General
You will need the following:

    1.  PostgreSQL (I used version 9.3.1 while developing)
    2.  Android SDK
    3.  TaintDroid enabled device (I used version 2.3 on Nexus S devices)
    

TaintDroid is available [here](http://appanalysis.org). Version 2.3 is available [here](http://appanalysis.org/download_2.3.html).

Make sure that the Android tools and platform tools are under your actual system path. `createdb` and `dropdb` commands for PostgreSQL need to be in your path.

### Python
At a minimum, you will need to install the following:

    1.  networkx
    2.  pydot
    3.  ImageMagick
    4.  graphviz
    5.  tesseract
    6.  Android screenshot tool (<androidsource>\sdk\screenshot\src\com)

You need to build and install _tema-adapterlib-3.2-sma_ and _tema-android-adapter-3.2-sma_ under _Source/Squiddy/src/_ in that specific order. For each of _adapterlib_ and _android-adapter_ refer to the included README file under the assocaited directory.

### Java
At a minimum, you will need to install the following:

    1.  NetBeans (I used version 7.3.1)
    

You need to build the _Gort_ project under _Source/GortGUIv2/Gort_ as a NetBeans platform application.

If you are using a different version of NetBeans on a different platform (e.g., not Mac OS X), you may need to specify the NetBeans platform to build against. The platform files are already included in this repository. 

    1.  Right click on the Gort project and select Properties
    2.  Select the libraries category on the left
    3.  For Java Platform, you can choose JDK 1.7 (optional)
    4.  For NetBeans Platform, create a platform called nb731
        1.  The platform folder should point to Source/GortGUIv2/Platform/NetBeans731
        2.  Under the Harness tab, select Harness supplied with Platform

